package dev.depthward.camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.lwjgl.opengl.*;
public final class PlayerFade {
    private static float alpha=1;
    public static void upload(int program){if(program!=0)GL20C.glUniform1f(GL20C.glGetUniformLocation(program,"dwPlayerAlpha"),alpha);}
    public static void begin(Entity entity,float tick,MultiBufferSource buffers){
        var mc=Minecraft.getInstance();
        if(!Cutaway.enabled()||entity!=mc.player||entity.isSpectator())return;
        double distance=mc.gameRenderer.getMainCamera().getPosition().distanceTo(entity.getEyePosition(tick));
        double normal=Math.max(0.25,ThinWallCollision.requestedDistance());
        double t=Math.max(0,Math.min(1,(0.95-distance/normal)/0.6));
        float value=(float)(1-0.75*t*t*(3-2*t));
        if(value>=0.999)return;
        if(!(buffers instanceof MultiBufferSource.BufferSource batch))return;
        batch.endBatch();alpha=value;upload(GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
    }
    public static void end(Entity entity,MultiBufferSource buffers){
        if(entity!=Minecraft.getInstance().player||alpha>=0.999)return;
        if(buffers instanceof MultiBufferSource.BufferSource batch)batch.endBatch();
        alpha=1;upload(GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
    }
    private PlayerFade(){}
}
