package zip.kiran.snapshot.client.photo;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.config.RuntimeConfig;
import zip.kiran.snapshot.net.payload.PhotoRequestC2S;
import zip.kiran.snapshot.photo.PhotoCompression;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientPhotoCache {
    private ClientPhotoCache() {
    }

    private static final int MAX_LIVE_TEXTURES = 96;
    private static final int MAX_ENTRIES = 512;
    private static final long PENDING_RETRY_MS = 2000L;

    private static final class Entry {
        Identifier textureId;
        NativeImageBackedTexture texture;
        boolean missing;
        long requestedAt;
        long lastUsed;
    }

    private static final Map<UUID, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<UUID, byte[][]> DOWNLOADS = new HashMap<>();
    private static final Map<UUID, Integer> DOWNLOAD_LENGTHS = new HashMap<>();

    public static Identifier texture(UUID photoId) {
        Entry entry = ENTRIES.get(photoId);
        long now = System.currentTimeMillis();

        if (entry == null) {
            entry = new Entry();
            entry.requestedAt = now;
            ENTRIES.put(photoId, entry);
            loadFromDiskOrAsk(photoId);
        } else if (entry.textureId == null && !entry.missing && now - entry.requestedAt > PENDING_RETRY_MS) {
            entry.requestedAt = now;
            askServer(photoId);
        }

        entry.lastUsed = now;
        return entry.textureId;
    }

    public static boolean isMissing(UUID photoId) {
        Entry entry = ENTRIES.get(photoId);
        return entry != null && entry.missing;
    }

    private static void loadFromDiskOrAsk(UUID photoId) {
        Util.getIoWorkerExecutor().execute(() -> {
            byte[] rgb = null;
            Path path = cachePath(photoId);
            try {
                if (Files.isRegularFile(path)) {
                    try (InputStream in = Files.newInputStream(path); NativeImage image = NativeImage.read(in)) {
                        if (image.getWidth() == RuntimeConfig.photoSize && image.getHeight() == RuntimeConfig.photoSize) {
                            rgb = toRgb(image);
                        }
                    }
                }
            } catch (Exception e) {
                rgb = null;
            }

            byte[] loaded = rgb;
            MinecraftClient.getInstance().execute(() -> {
                if (loaded != null) {
                    install(photoId, loaded, false);
                } else {
                    askServer(photoId);
                }
            });
        });
    }

    private static void askServer(UUID photoId) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(new PhotoRequestC2S(photoId));
        }
    }

    public static void onDownloadBegin(UUID photoId, int chunkCount, int compressedLength) {
        if (chunkCount < 1 || chunkCount > CameraConstants.MAX_CHUNKS) {
            return;
        }
        DOWNLOADS.put(photoId, new byte[chunkCount][]);
        DOWNLOAD_LENGTHS.put(photoId, compressedLength);
    }

    public static void onDownloadChunk(UUID photoId, int index, byte[] data) {
        byte[][] chunks = DOWNLOADS.get(photoId);
        if (chunks == null || index < 0 || index >= chunks.length) {
            return;
        }
        chunks[index] = data;

        int total = 0;
        for (byte[] chunk : chunks) {
            if (chunk == null) {
                return;
            }
            total += chunk.length;
        }

        DOWNLOADS.remove(photoId);
        Integer expected = DOWNLOAD_LENGTHS.remove(photoId);
        if (expected == null || expected != total) {
            return;
        }

        byte[] compressed = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, compressed, offset, chunk.length);
            offset += chunk.length;
        }

        try {
            install(photoId, PhotoCompression.inflateExact(compressed), true);
        } catch (Exception e) {
            CameraMod.LOGGER.warn("Photograph {} arrived corrupt", photoId);
        }
    }

    public static void onMissing(UUID photoId) {
        Entry entry = ENTRIES.computeIfAbsent(photoId, key -> new Entry());
        entry.missing = true;
    }

    private static void install(UUID photoId, byte[] rgb, boolean writeToDisk) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entry entry = ENTRIES.computeIfAbsent(photoId, key -> new Entry());

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, RuntimeConfig.photoSize, RuntimeConfig.photoSize, false);
        int index = 0;
        for (int y = 0; y < RuntimeConfig.photoSize; y++) {
            for (int x = 0; x < RuntimeConfig.photoSize; x++) {
                int r = rgb[index++] & 0xFF;
                int g = rgb[index++] & 0xFF;
                int b = rgb[index++] & 0xFF;
                image.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }

        if (writeToDisk) {
            byte[] copy = rgb.clone();
            Util.getIoWorkerExecutor().execute(() -> writeCache(photoId, copy));
        }

        Identifier textureId = new Identifier(CameraMod.MOD_ID, "photo/" + photoId.toString().replace("-", ""));
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        client.getTextureManager().registerTexture(textureId, texture);

        entry.textureId = textureId;
        entry.texture = texture;
        entry.missing = false;
        entry.lastUsed = System.currentTimeMillis();

        evictIfCrowded(client);
    }

    private static void writeCache(UUID photoId, byte[] rgb) {
        try (NativeImage image = new NativeImage(NativeImage.Format.RGBA, RuntimeConfig.photoSize, RuntimeConfig.photoSize, false)) {
            int index = 0;
            for (int y = 0; y < RuntimeConfig.photoSize; y++) {
                for (int x = 0; x < RuntimeConfig.photoSize; x++) {
                    int r = rgb[index++] & 0xFF;
                    int g = rgb[index++] & 0xFF;
                    int b = rgb[index++] & 0xFF;
                    image.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
                }
            }
            Path path = cachePath(photoId);
            Files.createDirectories(path.getParent());
            image.writeTo(path);
        } catch (Exception e) {
            CameraMod.LOGGER.warn("Could not cache photograph {}", photoId, e);
        }
    }

    private static void evictIfCrowded(MinecraftClient client) {
        while (ENTRIES.size() > MAX_ENTRIES) {
            UUID oldestId = null;
            long oldest = Long.MAX_VALUE;
            for (Map.Entry<UUID, Entry> candidate : ENTRIES.entrySet()) {
                Entry entry = candidate.getValue();
                if (entry.textureId == null && entry.lastUsed < oldest) {
                    oldest = entry.lastUsed;
                    oldestId = candidate.getKey();
                }
            }
            if (oldestId == null) {
                break;
            }
            ENTRIES.remove(oldestId);
        }

        int live = 0;
        for (Entry entry : ENTRIES.values()) {
            if (entry.textureId != null) {
                live++;
            }
        }
        while (live > MAX_LIVE_TEXTURES) {
            UUID oldestId = null;
            long oldest = Long.MAX_VALUE;
            for (Map.Entry<UUID, Entry> candidate : ENTRIES.entrySet()) {
                if (candidate.getValue().textureId != null && candidate.getValue().lastUsed < oldest) {
                    oldest = candidate.getValue().lastUsed;
                    oldestId = candidate.getKey();
                }
            }
            if (oldestId == null) {
                return;
            }
            release(client, ENTRIES.remove(oldestId));
            live--;
        }
    }

    private static void release(MinecraftClient client, Entry entry) {
        if (entry == null || entry.textureId == null) {
            return;
        }
        try {
            client.getTextureManager().destroyTexture(entry.textureId);
        } catch (Exception ignored) {
        }
        entry.textureId = null;
        entry.texture = null;
    }

    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        Iterator<Entry> iterator = ENTRIES.values().iterator();
        while (iterator.hasNext()) {
            release(client, iterator.next());
            iterator.remove();
        }
        DOWNLOADS.clear();
        DOWNLOAD_LENGTHS.clear();
    }

    private static Path cachePath(UUID photoId) {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("photographs").resolve("cache").resolve(photoId + ".png");
    }

    private static byte[] toRgb(NativeImage image) {
        int size = RuntimeConfig.photoSize * RuntimeConfig.photoSize * 3;
        byte[] rgb = new byte[size];
        int index = 0;
        for (int y = 0; y < RuntimeConfig.photoSize; y++) {
            for (int x = 0; x < RuntimeConfig.photoSize; x++) {
                int packed = image.getColor(x, y);
                rgb[index++] = (byte) (packed & 0xFF);
                rgb[index++] = (byte) ((packed >> 8) & 0xFF);
                rgb[index++] = (byte) ((packed >> 16) & 0xFF);
            }
        }
        return rgb;
    }
}
