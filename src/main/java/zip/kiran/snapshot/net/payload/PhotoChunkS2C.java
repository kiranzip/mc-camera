package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PhotoChunkS2C(UUID photoId, int index, byte[] data) implements FabricPacket {
    public static final PacketType<PhotoChunkS2C> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "photo_chunk_down"), PhotoChunkS2C::new);

    public PhotoChunkS2C(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readVarInt(), buf.readByteArray(CameraConstants.MAX_CHUNK_BYTES));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(photoId);
        buf.writeVarInt(index);
        buf.writeByteArray(data);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
