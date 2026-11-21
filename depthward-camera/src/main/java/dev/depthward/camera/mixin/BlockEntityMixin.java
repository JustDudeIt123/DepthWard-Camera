package dev.depthward.camera.mixin;
import dev.depthward.camera.Cutaway;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityMixin {
    @Group(name = "dwBlockEntity", min = 1, max = 1)
    @Redirect(method = {"setupAndRender", "m_112284_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), remap = false, require = 0)
    private static <T extends BlockEntity> void dw$render(BlockEntityRenderer<T> renderer, T entity, float tick,
                PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        dw$scoped(renderer,entity,tick,pose,buffers,light,overlay);
    }
    @Group(name = "dwBlockEntity", min = 1, max = 1)
    @Redirect(method = {"setupAndRender", "m_112284_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;m_6922_(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), remap = false, require = 0)
    private static <T extends BlockEntity> void dw$runtime(BlockEntityRenderer<T> renderer, T entity, float tick,
                PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        dw$scoped(renderer,entity,tick,pose,buffers,light,overlay);
    }
    private static <T extends BlockEntity> void dw$scoped(BlockEntityRenderer<T> renderer, T entity, float tick,
                PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(buffers instanceof MultiBufferSource.BufferSource batch) || !Cutaway.nearCamera(Vec3.atCenterOf(entity.getBlockPos()))) {
            renderer.render(entity,tick,pose,buffers,light,overlay);
            return;
        }
        // Flush each side of the scope so buffered player/entity geometry never inherits the mask.
        batch.endBatch();
        boolean old = Cutaway.blockEntity(true);
        try {
            renderer.render(entity,tick,pose,buffers,light,overlay);
            batch.endBatch();
        } finally { Cutaway.blockEntity(old); }
    }
}
