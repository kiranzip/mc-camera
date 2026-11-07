package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record PhotoChunkC2S(int transferId, int index, byte[] data) implements FabricPacket {
    public static final PacketType<PhotoChunkC2S> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "photo_chunk_up"), PhotoChunkC2S::new);

    public PhotoChunkC2S(PacketByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readByteArray(CameraConstants.MAX_CHUNK_BYTES));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(transferId);
        buf.writeVarInt(index);
        buf.writeByteArray(data);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
