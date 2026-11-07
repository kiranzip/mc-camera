package zip.kiran.snapshot.sound;

import zip.kiran.snapshot.CameraMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class Sounds {
    public static final SoundEvent CAMERA_SHUTTER = register("camera_shutter");
    public static final SoundEvent CAMERA_EJECT = register("camera_eject");
    public static final SoundEvent CAMERA_DIAL = register("camera_dial");

    public static void init() {
    }

    public static @NotNull SoundEvent register(String id) {
        Identifier identifier = new Identifier(CameraMod.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }
}
