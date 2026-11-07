package zip.kiran.snapshot.item.misc;

import zip.kiran.snapshot.component.Components;
import zip.kiran.snapshot.component.CameraSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CameraItem extends Item {
    public CameraItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        CameraSettings settings = Components.CAMERA_SETTINGS.getOrDefault(stack, CameraSettings.DEFAULT);
        tooltip.add(Text.literal(settings.formatAperture() + "  " + settings.formatFocalLength()).formatted(Formatting.GRAY));
        tooltip.add(Text.literal(settings.formatFocusDistance() + "  " + settings.formatIso()).formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("item.kiranzip.camera.hint").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }

    public static CameraSettings settingsOf(ItemStack stack) {
        return Components.CAMERA_SETTINGS.getOrDefault(stack, CameraSettings.DEFAULT);
    }
}
