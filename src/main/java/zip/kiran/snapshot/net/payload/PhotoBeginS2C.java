package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PhotoBeginS2C(UUID photoId, int chunkCount, int compressedLength) implements FabricPacket {
    public static final PacketType<PhotoBeginS2C> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "photo_begin_down"), PhotoBeginS2C::new);

    public PhotoBeginS2C(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(photoId);
        buf.writeVarInt(chunkCount);
        buf.writeVarInt(compressedLength);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
