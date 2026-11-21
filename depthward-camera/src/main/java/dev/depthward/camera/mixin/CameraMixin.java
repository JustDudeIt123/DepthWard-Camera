package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = {"getMaxZoom", "m_90566_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void dw$zoom(double distance, CallbackInfoReturnable<Double> ci) {
        if (Cutaway.enabled()) {
            ci.setReturnValue(dev.depthward.camera.ThinWallCollision.vanilla((Camera)(Object)this,distance));
        }
    }
}
