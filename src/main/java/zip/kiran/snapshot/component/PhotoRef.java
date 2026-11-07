package zip.kiran.snapshot.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.config.RuntimeConfig;

import java.util.UUID;

public record PhotoRef(UUID photoId, String author, long capturedAt, String location, CameraSettings settings) {
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<PhotoRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("photo_id").forGetter(PhotoRef::photoId),
            Codec.STRING.optionalFieldOf("author", "").forGetter(PhotoRef::author),
            Codec.LONG.optionalFieldOf("captured_at", 0L).forGetter(PhotoRef::capturedAt),
            Codec.STRING.optionalFieldOf("location", "").forGetter(PhotoRef::location),
            CameraSettings.CODEC.optionalFieldOf("settings", CameraSettings.DEFAULT).forGetter(PhotoRef::settings)
    ).apply(instance, PhotoRef::new));

    public static PhotoRef fromNbt(NbtElement element) {
        if (!(element instanceof NbtCompound tag)) {
            return null;
        }
        try {
            UUID photoId = UUID.fromString(tag.getString("photo_id"));
            String author = tag.contains("author") ? tag.getString("author") : "";
            long capturedAt = tag.contains("captured_at") ? tag.getLong("captured_at") : 0L;
            String location = tag.contains("location") ? tag.getString("location") : "";
            NbtElement settings = tag.get("settings");
            CameraSettings camera = settings instanceof NbtCompound settingsTag
                    ? CameraSettings.fromNbt(settingsTag)
                    : CameraSettings.DEFAULT;
            return new PhotoRef(photoId, author, capturedAt, location, camera);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public float developProgress(long nowMillis) {
        if (!RuntimeConfig.polaroidDevelopEffect) {
            return 1.0F;
        }
        if (capturedAt <= 0L) {
            return 1.0F;
        }
        float seconds = (nowMillis - capturedAt) / 1000.0F;
        if (seconds <= 0.0F) {
            return 0.0F;
        }
        float linear = seconds / RuntimeConfig.developSeconds;
        if (linear >= 1.0F) {
            return 1.0F;
        }
        return linear * linear * (3.0F - 2.0F * linear);
    }
}
