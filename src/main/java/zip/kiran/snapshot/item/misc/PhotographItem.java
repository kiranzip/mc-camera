package zip.kiran.snapshot.item.misc;

import zip.kiran.snapshot.component.Components;
import zip.kiran.snapshot.component.PhotoRef;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PhotographItem extends Item {
    public PhotographItem() {
        super(new Settings().maxCount(16));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public Text getName(ItemStack stack) {
        PhotoRef ref = Components.PHOTO.get(stack);
        if (ref == null || ref.author().isEmpty()) {
            return super.getName(stack);
        }
        return Text.translatable("item.kiranzip.photograph.by", ref.author());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        PhotoRef ref = Components.PHOTO.get(stack);
        if (ref == null) {
            tooltip.add(Text.translatable("item.kiranzip.photograph.blank").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            return;
        }
        if (!ref.location().isEmpty()) {
            tooltip.add(Text.literal(ref.location()).formatted(Formatting.GRAY));
        }
        tooltip.add(Text.literal(ref.settings().formatAperture() + "  \u00b7  " + ref.settings().formatFocalLength()
                + "  \u00b7  " + ref.settings().formatIso()).formatted(Formatting.DARK_GRAY));
        if (ref.developProgress(System.currentTimeMillis()) < 1.0F) {
            tooltip.add(Text.translatable("item.kiranzip.photograph.developing").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
    }
}
