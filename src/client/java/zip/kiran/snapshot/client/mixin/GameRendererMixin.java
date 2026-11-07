package zip.kiran.snapshot.client.mixin;

import zip.kiran.snapshot.client.CameraClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 1)
    private void kiranzip$viewfinderZoom(Camera camera, float tickDelta, boolean changingFov,
                                          CallbackInfoReturnable<Double> info) {
        if (!changingFov) {
            return;
        }
        double vanilla = info.getReturnValue();
        double adjusted = CameraClient.fovFor(vanilla);
        if (adjusted != vanilla) {
            info.setReturnValue(adjusted);
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void kiranzip$hideHandInViewfinder(CallbackInfo info) {
        if (CameraClient.isViewfinderActive()) {
            info.cancel();
        }
    }
}
