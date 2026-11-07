package zip.kiran.snapshot.net.payload;

import zip.kiran.snapshot.CameraMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PhotoMissingS2C(UUID photoId) implements FabricPacket {
    public static final PacketType<PhotoMissingS2C> TYPE =
            PacketType.create(new Identifier(CameraMod.MOD_ID, "photo_missing"), PhotoMissingS2C::new);

    public PhotoMissingS2C(PacketByteBuf buf) {
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
