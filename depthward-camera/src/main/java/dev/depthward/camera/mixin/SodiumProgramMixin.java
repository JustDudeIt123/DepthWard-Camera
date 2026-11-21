package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlProgram", remap = false)
public class SodiumProgramMixin {
    // Oculus owns its glUseProgram redirect. Observe the finished bind instead of replacing it.
    @Inject(method = "bind", at = @At("RETURN"))
    private void dw$bind(CallbackInfo ci) {
        Cutaway.upload(org.lwjgl.opengl.GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
    }
}
