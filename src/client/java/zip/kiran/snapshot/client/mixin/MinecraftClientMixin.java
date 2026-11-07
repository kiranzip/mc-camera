package zip.kiran.snapshot.client.mixin;

import zip.kiran.snapshot.client.CameraClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void kiranzip$shutterRelease(CallbackInfoReturnable<Boolean> info) {
        if (CameraClient.onAttackPressed()) {
            info.setReturnValue(false);
        }
    }
}
