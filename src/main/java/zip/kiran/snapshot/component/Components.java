package zip.kiran.snapshot.component;

import com.mojang.serialization.Codec;
import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.component.CameraSettings;
import zip.kiran.snapshot.component.PhotoRef;
import org.jetbrains.annotations.NotNull;

public class Components {
    public static final ItemData<CameraSettings> CAMERA_SETTINGS = register(CameraSettings.CODEC, "camera_settings");
    public static final ItemData<PhotoRef> PHOTO = register(PhotoRef.CODEC, "photo");

    public static void init() {
    }

    public static <T> @NotNull ItemData<T> register(Codec<T> codec, String id) {
        return new ItemData<>(CameraMod.MOD_ID + ":" + id, codec);
    }
}
