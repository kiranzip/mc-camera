package zip.kiran.snapshot.client.mixin;

import zip.kiran.snapshot.client.CameraClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void kiranzip$hideCrosshair(CallbackInfo info) {
        if (CameraClient.state().isActive()) {
            info.cancel();
        }
    }
}
