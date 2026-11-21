package dev.depthward.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;

/** Inset, depth-tested black block interiors close missing chunk faces without covering the scene. */
final class CutSurfaces {
    private static final BufferBuilder BUFFER = new BufferBuilder(65536);
    private static final int[][] FACES = {
        {0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}
    };
    static void draw(RenderLevelStageEvent event,Vec3 feet,Vec3 camera) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.player.isSpectator() || !CameraDiagnostics.interiors())return;
        // Bound work even for an unusually distant custom camera. Only loaded client blocks are read.
        int minX=(int)Math.floor(Math.min(feet.x,camera.x)-3),maxX=(int)Math.floor(Math.max(feet.x,camera.x)+3);
        int minY=(int)Math.floor(Math.min(feet.y,camera.y)-3),maxY=(int)Math.floor(Math.max(feet.y+mc.player.getBbHeight(),camera.y)+3);
        int minZ=(int)Math.floor(Math.min(feet.z,camera.z)-3),maxZ=(int)Math.floor(Math.max(feet.z,camera.z)+3);
        if ((long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1)>8192) {
            minX=(int)Math.floor(camera.x)-4;maxX=minX+8;
            minY=(int)Math.floor(camera.y)-4;maxY=minY+8;
            minZ=(int)Math.floor(camera.z)-4;maxZ=minZ+8;
        }
        var pose=event.getPoseStack().last().pose();
        BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        int boxes=0;
        for(var pos:BlockPos.betweenClosed(minX,minY,minZ,maxX,maxY,maxZ)) {
            var state=mc.level.getBlockState(pos);
            if(!state.canOcclude())continue;
            for(var box:state.getOcclusionShape(mc.level,pos).toAabbs()) {
                if(box.getXsize()<0.006||box.getYsize()<0.006||box.getZsize()<0.006)continue;
                double[] xs={pos.getX()+box.minX-camera.x,pos.getX()+box.maxX-camera.x};
                double[] ys={pos.getY()+box.minY-camera.y,pos.getY()+box.maxY-camera.y};
                double[] zs={pos.getZ()+box.minZ-camera.z,pos.getZ()+box.maxZ-camera.z};
                // Inset only the face normal, keeping tangential edges overlapping and sealed.
                for(int i=0;i<FACES.length;i++)for(int corner:FACES[i]) {
                    double inset=(i%2==0?0.002:-0.002);
                    vertex(pose,xs[corner&1]+(i>=4?inset:0),ys[(corner>>1)&1]+(i>=2&&i<4?inset:0),zs[(corner>>2)&1]+(i<2?inset:0));
                }
                boxes++;
            }
        }
        var rendered=BUFFER.end();
        if(boxes==0){rendered.release();return;}
        var shader=RenderSystem.getShader();
        var color=RenderSystem.getShaderColor().clone();
        boolean depth=GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST),blend=GL11C.glIsEnabled(GL11C.GL_BLEND);
        boolean write=GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        boolean cull=GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        boolean old=Cutaway.blockEntity(true);
        try {
            RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(rendered);
        } finally {
            Cutaway.blockEntity(old);
            RenderSystem.setShader(()->shader);
            RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.depthMask(write);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
        }
    }
    private static void vertex(Matrix4f pose,double x,double y,double z) {
        BUFFER.vertex(pose,(float)x,(float)y,(float)z).color(0,0,0,255).endVertex();
    }
    private CutSurfaces() {}
}
