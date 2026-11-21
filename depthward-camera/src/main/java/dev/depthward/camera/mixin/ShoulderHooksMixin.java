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
@Mixin(targets = "com.github.exopandora.shouldersurfing.client.EventHooks", remap = false)
public class ShoulderHooksMixin {
    @Inject(method = "getTargetOffset", at = @At("HEAD"), cancellable = true)
    private static void dw$offset(Vec3 offset, Camera camera, Entity entity, BlockGetter level, CallbackInfoReturnable<Vec3> ci) {
        if (Cutaway.enabled()) ci.setReturnValue(offset);
    }
    @Inject(method = "isTemporaryFirstPerson", at = @At("HEAD"), cancellable = true)
    private static void dw$perspective(CallbackInfoReturnable<Boolean> ci) {
        if (Cutaway.enabled()) ci.setReturnValue(false);
    }
}
