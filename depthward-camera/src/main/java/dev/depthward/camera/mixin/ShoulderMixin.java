package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.ShoulderSurfingCamera", remap = false)
public class ShoulderMixin {
    @Inject(method = "maxZoom", at = @At("HEAD"), cancellable = true)
    private static void dw$zoom(Camera camera, Entity entity, BlockGetter level, Vec3 offset, float tick, CallbackInfoReturnable<Double> ci) {
        if (Cutaway.enabled()) {
            ci.setReturnValue(dev.depthward.camera.ThinWallCollision.shoulder(camera,entity,level,offset,tick));
        }
    }
}
