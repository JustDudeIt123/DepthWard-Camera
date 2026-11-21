package dev.depthward.camera.mixin;
import dev.depthward.camera.PlayerFade;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderDispatcher.class)
public class PlayerFadeMixin {
    @Inject(method={"render","m_114384_"},at=@At("HEAD"),remap=false)
    private void dw$begin(Entity entity,double x,double y,double z,float yaw,float tick,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){PlayerFade.begin(entity,tick,buffers);}
    @Inject(method={"render","m_114384_"},at=@At("RETURN"),remap=false)
    private void dw$end(Entity entity,double x,double y,double z,float yaw,float tick,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){PlayerFade.end(entity,buffers);}
}
