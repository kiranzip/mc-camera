package zip.kiran.snapshot.client.mixin;

import zip.kiran.snapshot.client.CameraClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void kiranzip$turnDial(long window, double horizontal, double vertical, CallbackInfo info) {
        if (CameraClient.onScroll(vertical)) {
            info.cancel();
        }
    }
}
