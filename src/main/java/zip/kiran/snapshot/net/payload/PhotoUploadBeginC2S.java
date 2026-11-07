package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.component.CameraSettings;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record PhotoUploadBeginC2S(int transferId, int chunkCount, int compressedLength, CameraSettings settings)
        implements FabricPacket {
    public static final PacketType<PhotoUploadBeginC2S> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "photo_upload_begin"), PhotoUploadBeginC2S::new);

    public PhotoUploadBeginC2S(PacketByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), CameraSettings.read(buf));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(transferId);
        buf.writeVarInt(chunkCount);
        buf.writeVarInt(compressedLength);
        settings.write(buf);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
