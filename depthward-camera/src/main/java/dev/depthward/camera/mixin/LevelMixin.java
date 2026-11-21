package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public class LevelMixin {
    @Unique private boolean dw$smartCull;
    @Inject(method = {"renderLevel", "m_109599_"}, at = @At("HEAD"), remap = false)
    private void dw$begin(CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        dw$smartCull = mc.smartCull;
        if (Cutaway.needsOcclusionBypass()) mc.smartCull = false;
    }
    @Inject(method = {"renderLevel", "m_109599_"}, at = @At("RETURN"), remap = false)
    private void dw$end(CallbackInfo ci) { Minecraft.getInstance().smartCull = dw$smartCull; }
}
