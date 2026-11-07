package zip.kiran.snapshot.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class CameraKeys {
    private CameraKeys() {
    }

    public static final String CATEGORY = "key.categories.kiranzip.camera";

    public static KeyBinding toggleViewfinder;
    public static KeyBinding cycleDial;
    public static KeyBinding adjustUp;
    public static KeyBinding adjustDown;
    public static KeyBinding autofocus;

    public static void register() {
        toggleViewfinder = register("key.kiranzip.toggle_viewfinder", GLFW.GLFW_KEY_UNKNOWN);
        cycleDial = register("key.kiranzip.cycle_dial", GLFW.GLFW_KEY_V);
        adjustUp = register("key.kiranzip.adjust_up", GLFW.GLFW_KEY_RIGHT_BRACKET);
        adjustDown = register("key.kiranzip.adjust_down", GLFW.GLFW_KEY_LEFT_BRACKET);
        autofocus = register("key.kiranzip.autofocus", GLFW.GLFW_KEY_G);
    }

    private static KeyBinding register(String translationKey, int keyCode) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(translationKey, InputUtil.Type.KEYSYM, keyCode, CATEGORY));
    }
}
