package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GlStateManager.class)
public class GlMixin {
    @Inject(method = "_glUseProgram", at = @At("RETURN"), remap = false)
    private static void dw$bind(int program, CallbackInfo ci) { Cutaway.upload(program); }
}
