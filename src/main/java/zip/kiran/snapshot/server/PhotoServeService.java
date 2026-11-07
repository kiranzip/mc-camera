package zip.kiran.snapshot.server;

import zip.kiran.snapshot.photo.PhotoCompression;
import zip.kiran.snapshot.photo.PhotoFile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PhotoServeService {
    private final Map<UUID, Long> lastServe = new HashMap<>();
    private final Map<UUID, CachedPhoto> compressedCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, CachedPhoto> eldest) { return size() > 64; }
    };
    private final Path directory;

    public PhotoServeService(Path directory) {
        this.directory = directory;
    }

    public boolean canServe(UUID playerUuid) {
        long now = System.currentTimeMillis();
        Long last = lastServe.get(playerUuid);
        if (last != null && now - last < 100L) return false;
        lastServe.put(playerUuid, now);
        return true;
    }

    public void forgetPlayer(UUID uuid) {
        lastServe.remove(uuid);
    }

    private record CachedPhoto(byte[] compressed, byte[][] chunks) {}
}