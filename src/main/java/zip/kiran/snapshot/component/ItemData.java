package zip.kiran.snapshot.component;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

public record ItemData<T>(String key, Codec<T> codec) {
    public T get(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return null;
        }
        NbtElement element = nbt.get(key);
        if (element == null) {
            return null;
        }
        return codec.parse(NbtOps.INSTANCE, element).result().orElse(null);
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value == null ? fallback : value;
    }

    public NbtElement raw(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return null;
        }
        return nbt.get(key);
    }

    public void set(ItemStack stack, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(element -> stack.setSubNbt(key, element));
    }
}
