package zip.kiran.snapshot.server;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.net.payload.PhotoBeginS2C;
import zip.kiran.snapshot.net.payload.PhotoChunkS2C;
import zip.kiran.snapshot.net.payload.PhotoMissingS2C;
import zip.kiran.snapshot.photo.PhotoCompression;
import zip.kiran.snapshot.photo.PhotoFile;
import zip.kiran.snapshot.component.Components;
import zip.kiran.snapshot.component.CameraSettings;
import zip.kiran.snapshot.component.PhotoRef;
import zip.kiran.snapshot.item.Items;
import zip.kiran.snapshot.sound.Sounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerPhotoStore {
    private static volatile ServerPhotoStore current;

    private final Path directory;
    private final UploadManager uploadManager = new UploadManager();
    private final Map<UUID, Long> lastServe = new HashMap<>();
    private final Map<UUID, Long> lastSettingsUpdate = new HashMap<>();
    private final Map<UUID, UUID> photoOwners = new HashMap<>();

    private final Map<UUID, CachedPhoto> compressedCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, CachedPhoto> eldest) {
            return size() > 64;
        }
    };

    private ServerPhotoStore(MinecraftServer server) {
        this.directory = server.getSavePath(WorldSavePath.ROOT).resolve("photographs");
    }

    public static void registerLifecycle() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> current = new ServerPhotoStore(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> current = null);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerPhotoStore store = current;
            if (store != null && server.getTicks() % 40 == 0) {
                store.pruneExpiredUploads();
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPhotoStore store = current;
            if (store != null && handler.player != null) {
                store.forgetPlayer(handler.player.getUuid());
            }
        });
    }

    public static ServerPhotoStore get() {
        return current;
    }

    public void forgetPlayer(UUID uuid) {
        uploadManager.forgetPlayer(uuid);
        lastServe.remove(uuid);
        lastSettingsUpdate.remove(uuid);
    }

    public boolean tryConsumeShutter(ServerPlayerEntity player) {
        return uploadManager.tryConsumeShutter(player);
    }

    public void beginUpload(ServerPlayerEntity player, int transferId, int chunkCount, int compressedLength, CameraSettings settings) {
        if (transferId <= 0) return;
        if (chunkCount < 1 || chunkCount > CameraConstants.MAX_CHUNKS) return;
        if (compressedLength < 1 || compressedLength > CameraConstants.MAX_CHUNKS * CameraConstants.MAX_CHUNK_BYTES) return;
        if (!isHoldingCamera(player)) return;

        ItemStack camera = heldCamera(player);
        if (camera.isEmpty()) return;
        CameraSettings clean = settings.sanitised();
        Components.CAMERA_SETTINGS.set(camera, clean);
        uploadManager.beginUpload(player, transferId, chunkCount, compressedLength, settings);
    }

    public void acceptChunk(ServerPlayerEntity player, int transferId, int index, byte[] data) {
        PhotoUploadSession session = uploadManager.getSession(player.getUuid());
        if (session == null || session.transferId != transferId) return;
        if (index < 0 || data == null || data.length > CameraConstants.MAX_CHUNK_BYTES) {
            uploadManager.removeSession(player.getUuid());
            return;
        }
        if (!session.accept(index, data)) {
            uploadManager.removeSession(player.getUuid());
            return;
        }
        if (session.isComplete()) {
            uploadManager.removeSession(player.getUuid());
            finishUpload(player, session);
        }
    }

    private void finishUpload(ServerPlayerEntity player, PhotoUploadSession session) {
        byte[] compressed = session.assemble();
        if (compressed == null) {
            return;
        }

        byte[] rgb;
        try {
            rgb = PhotoCompression.inflateExact(compressed);
        } catch (java.util.zip.DataFormatException e) {
            CameraMod.LOGGER.warn("Discarded a malformed photograph from {}: {}", player.getGameProfile().getName(), e.toString());
            return;
        }

        UUID photoId = UUID.randomUUID();
        try {
            PhotoFile.write(pathFor(photoId), rgb);
        } catch (java.io.IOException e) {
            CameraMod.LOGGER.error("Could not save photograph {}", photoId, e);
            return;
        }
        compressedCache.put(photoId, new CachedPhoto(compressed, PhotoCompression.chunk(compressed)));
        photoOwners.put(photoId, player.getUuid());

        player.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(),
                Sounds.CAMERA_SHUTTER, SoundCategory.PLAYERS, 0.9F, 1.0F);

        ItemStack print = new ItemStack(Items.PHOTOGRAPH);
        Components.PHOTO.set(print, new PhotoRef(
                photoId,
                player.getGameProfile().getName(),
                System.currentTimeMillis(),
                describeLocation(player),
                session.settings));

        if (!player.getInventory().insertStack(print)) {
            player.dropItem(print, false);
        }
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                Sounds.CAMERA_EJECT, SoundCategory.PLAYERS, 0.7F, 1.0F);
    }

    private void pruneExpiredUploads() {
        long now = System.currentTimeMillis();
        uploadManager.pruneExpired(now);
    }

    public void updateSettings(ServerPlayerEntity player, CameraSettings settings) {
        long now = System.currentTimeMillis();
        Long last = lastSettingsUpdate.get(player.getUuid());
        if (last != null && now - last < CameraConstants.SETTINGS_COOLDOWN_MS) {
            return;
        }
        lastSettingsUpdate.put(player.getUuid(), now);

        ItemStack camera = heldCamera(player);
        if (!camera.isEmpty()) {
            Components.CAMERA_SETTINGS.set(camera, settings.sanitised());
        }
    }

    public void serve(ServerPlayerEntity player, UUID photoId) {
        long now = System.currentTimeMillis();
        Long last = lastServe.get(player.getUuid());
        if (last != null && now - last < CameraConstants.SERVE_COOLDOWN_MS) {
            return;
        }
        lastServe.put(player.getUuid(), now);

        UUID owner = photoOwners.get(photoId);
        if (owner != null && !owner.equals(player.getUuid())) {
            ServerPlayNetworking.send(player, new PhotoMissingS2C(photoId));
            return;
        }

        CachedPhoto cached = compressedCache.get(photoId);
        if (cached == null) {
            byte[] rgb = PhotoFile.read(pathFor(photoId));
            if (rgb == null) {
                ServerPlayNetworking.send(player, new PhotoMissingS2C(photoId));
                return;
            }
            byte[] compressed = PhotoCompression.deflate(rgb);
            cached = new CachedPhoto(compressed, PhotoCompression.chunk(compressed));
            compressedCache.put(photoId, cached);
        }

        byte[][] chunks = cached.chunks();
        ServerPlayNetworking.send(player, new PhotoBeginS2C(photoId, chunks.length, cached.compressed().length));
        for (int i = 0; i < chunks.length; i++) {
            ServerPlayNetworking.send(player, new PhotoChunkS2C(photoId, i, chunks[i]));
        }
    }

    private Path pathFor(UUID photoId) {
        return directory.resolve(photoId.toString() + ".png");
    }

    private static boolean isHoldingCamera(ServerPlayerEntity player) {
        return !heldCamera(player).isEmpty();
    }

    private static ItemStack heldCamera(ServerPlayerEntity player) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Items.CAMERA)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String describeLocation(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private record CachedPhoto(byte[] compressed, byte[][] chunks) {
    }
}
