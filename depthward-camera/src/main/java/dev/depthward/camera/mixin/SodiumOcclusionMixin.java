package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public class SodiumOcclusionMixin {
    @Inject(method = "shouldUseOcclusionCulling", at = @At("HEAD"), cancellable = true)
    private void dw$culling(CallbackInfoReturnable<Boolean> ci) {
        if (Cutaway.needsOcclusionBypass()) ci.setReturnValue(false);
    }
}
