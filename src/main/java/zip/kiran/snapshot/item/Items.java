package zip.kiran.snapshot.item;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.item.misc.CameraItem;
import zip.kiran.snapshot.item.misc.PhotographItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class Items {

    public static final Item CAMERA = register(new CameraItem(), "camera");
    public static final Item PHOTOGRAPH = register(new PhotographItem(), "photograph");

    public static void init() {
    }

    public static @NotNull Item register(Item item, String id) {
        return Registry.register(Registries.ITEM, new Identifier(CameraMod.MOD_ID, id), item);
    }
}
