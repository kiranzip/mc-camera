package zip.kiran.snapshot.server;

import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.component.CameraSettings;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UploadManager {
    private final Map<UUID, PhotoUploadSession> uploads = new HashMap<>();
    private final Map<UUID, Long> lastShot = new HashMap<>();

    public boolean tryConsumeShutter(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        Long previous = lastShot.get(player.getUuid());
        if (previous != null && now - previous < CameraConstants.SHUTTER_COOLDOWN_MS) {
            return false;
        }
        lastShot.put(player.getUuid(), now);
        return true;
    }

    public void beginUpload(ServerPlayerEntity player, int transferId, int chunkCount, int compressedLength, CameraSettings settings) {
        if (transferId <= 0) return;
        if (chunkCount < 1 || chunkCount > CameraConstants.MAX_CHUNKS) return;
        if (compressedLength < 1 || compressedLength > CameraConstants.MAX_CHUNKS * CameraConstants.MAX_CHUNK_BYTES) return;
        if (!tryConsumeShutter(player)) return;

        CameraSettings clean = settings.sanitised();
        uploads.put(player.getUuid(), new PhotoUploadSession(transferId, chunkCount, compressedLength, clean, System.currentTimeMillis()));
    }

    public PhotoUploadSession getSession(UUID uuid) {
        return uploads.get(uuid);
    }

    public void removeSession(UUID uuid) {
        uploads.remove(uuid);
    }

    public void pruneExpired(long now) {
        uploads.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public void forgetPlayer(UUID uuid) {
        lastShot.remove(uuid);
        uploads.remove(uuid);
    }
}