package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PhotoRequestC2S(UUID photoId) implements FabricPacket {
    public static final PacketType<PhotoRequestC2S> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "photo_request"), PhotoRequestC2S::new);

    public PhotoRequestC2S(PacketByteBuf buf) {
        this(buf.readUuid());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(photoId);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
