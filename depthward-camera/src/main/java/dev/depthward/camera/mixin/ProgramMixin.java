package dev.depthward.camera.mixin;
import dev.depthward.camera.ShaderPatch;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import java.io.InputStream;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(Program.class)
public class ProgramMixin {
    @Redirect(method = {"compileShaderInternal", "m_166612_"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;glShaderSource(ILjava/util/List;)V"), remap = false)
    private static void dw$source(int shader, List<String> source, Program.Type type, String name,
                                 InputStream input, String namespace, GlslPreprocessor processor) {
        if (name.startsWith("minecraft:")) name = name.substring("minecraft:".length());
        if (type == Program.Type.FRAGMENT && ShaderPatch.terrain(name))
            source = List.of(ShaderPatch.patch(String.join("", source)));
        else if (type == Program.Type.FRAGMENT && (name.startsWith("rendertype_") || name.equals("position_color") || name.equals("clouds")))
            source = List.of(ShaderPatch.objectPatch(String.join("", source)));
        if(type==Program.Type.FRAGMENT)source=List.of(ShaderPatch.playerFade(String.join("",source)));
        GlStateManager.glShaderSource(shader, source);
    }
}
