package dev.depthward.camera.mixin;
import dev.depthward.camera.ShaderPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlShader", remap = false)
public class SodiumShaderMixin {
    // Static argument hook runs before super() without accessing this or generated Args classes.
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String dw$source(String source) {
        return source.contains("in float v_MaterialAlphaCutoff") ? ShaderPatch.patch(source) : source;
    }
}
