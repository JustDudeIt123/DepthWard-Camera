package dev.depthward.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;

/** Masks hidden sky too: empty pixels have no terrain fragment to run the visibility shader. */
final class VisibilityBackdrop {
    private static final BufferBuilder BUFFER=new BufferBuilder(256);
    static void draw() {
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var shader=RenderSystem.getShader();
        var color=RenderSystem.getShaderColor().clone();
        boolean depth=GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST),blend=GL11C.glIsEnabled(GL11C.GL_BLEND);
        boolean write=GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        var model=RenderSystem.getModelViewStack();model.pushPose();
        VisibilityGrid.backdrop(true);
        try {
            model.setIdentity();RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionColorShader);
            Cutaway.upload(GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
            BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            BUFFER.vertex(-1,-1,0.999999).color(0,0,0,255).endVertex();
            BUFFER.vertex(1,-1,0.999999).color(0,0,0,255).endVertex();
            BUFFER.vertex(1,1,0.999999).color(0,0,0,255).endVertex();
            BUFFER.vertex(-1,1,0.999999).color(0,0,0,255).endVertex();
            BufferUploader.drawWithShader(BUFFER.end());
        } finally {
            VisibilityGrid.backdrop(false);Cutaway.upload(GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
            model.popPose();RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setShader(()->shader);
            RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.depthMask(write);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        }
    }
    private VisibilityBackdrop() {}
}
