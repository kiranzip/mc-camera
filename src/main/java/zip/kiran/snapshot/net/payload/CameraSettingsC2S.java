package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.component.CameraSettings;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record CameraSettingsC2S(CameraSettings settings) implements FabricPacket {
    public static final PacketType<CameraSettingsC2S> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "camera_settings"), CameraSettingsC2S::new);

    public CameraSettingsC2S(PacketByteBuf buf) {
        this(CameraSettings.read(buf));
    }

    @Override
    public void write(PacketByteBuf buf) {
        settings.write(buf);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
