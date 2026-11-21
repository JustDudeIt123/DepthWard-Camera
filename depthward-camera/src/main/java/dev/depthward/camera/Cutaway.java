package dev.depthward.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.opengl.GL20C;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = DepthwardCamera.ID, value = Dist.CLIENT)
public final class Cutaway {
    private static float x, y, radius, depth, cutDepth;
    private static float feetY, width, height;
    private static float visibility;

    private static final Matrix4f inverse = new Matrix4f();

    private static final java.nio.FloatBuffer matrixBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static boolean frame;
    private static long lastFrame,holdUntil;
    private static final Matrix4f sightProjection=new Matrix4f();
    private static boolean blockEntity;
    private static boolean restoreCull, previousCull;
    private static Method shaderPackActive;
    private static boolean oculusChecked;

    public static boolean enabled() {
        var mc = Minecraft.getInstance();
        return DepthwardCamera.ENABLED.get() && mc.level != null && mc.player != null
            && !mc.options.getCameraType().isFirstPerson() && !shadersActive();
    }
    private static boolean shadersActive() {
        if (!oculusChecked) {
            oculusChecked = true;
            try {
                shaderPackActive = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance");
            } catch (ClassNotFoundException e) {
                try { shaderPackActive = Class.forName("net.coderbot.iris.api.v0.IrisApi").getMethod("getInstance"); }
                catch (ReflectiveOperationException absent) { shaderPackActive = null; }
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        if (shaderPackActive == null) return false;
        try {
            Object api = shaderPackActive.invoke(null);
            return (boolean) shaderPackActive.getReturnType().getMethod("isShaderPackInUse").invoke(api);
        } catch (ReflectiveOperationException e) { return true; }
    }
    @SubscribeEvent
    public static void stage(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            long now=System.nanoTime();
            float seconds=lastFrame==0?1/60f:(now-lastFrame)/1_000_000_000f;
            lastFrame=now;
            CullingCompat.resume();
            frame = false;
            blockEntity = false;
            radius = 0;
            if (!enabled()) {visibility=0;return;}
            var mc = Minecraft.getInstance();
            var feet = mc.player.getPosition(e.getPartialTick());
            var camera = e.getCamera().getPosition();
            boolean obstructed=insideSolid(camera)||occluded(feet,camera);
            if(obstructed)holdUntil=now+120_000_000L;
            visibility=CameraMath.transition(visibility,obstructed||now<holdUntil,seconds);
            if(visibility<=0)return;
            var p = feet.add(0, mc.player.getBbHeight() * 0.5, 0);
            Vec3 relative = p.subtract(e.getCamera().getPosition());
            var view = new Vector4f((float)relative.x, (float)relative.y, (float)relative.z, 1);
            e.getPoseStack().last().pose().transform(view);
            if (view.z >= -0.1f) return;
            var projected = e.getProjectionMatrix().transform(new Vector4f(view));
            if (projected.w <= 0) return;
            int w = mc.getMainRenderTarget().width, h = mc.getMainRenderTarget().height;
            width = w;
            height = h;
            feetY = (float)(feet.y-camera.y);
            inverse.set(e.getProjectionMatrix()).mul(e.getPoseStack().last().pose()).invert();
            sightProjection.set(inverse).invert();

            x = (projected.x / projected.w * 0.5f + 0.5f) * w;
            y = (projected.y / projected.w * 0.5f + 0.5f) * h;
            // Reach the far side of the model so touching/intersecting blockers are removed too.
            float rear = view.z;
            double half = mc.player.getBbWidth()*0.5;
            for (double dx : new double[]{-half,half}) for (double dy : new double[]{0,mc.player.getBbHeight()}) for (double dz : new double[]{-half,half}) {
                var corner = feet.add(dx,dy,dz).subtract(camera);
                var v = new Vector4f((float)corner.x,(float)corner.y,(float)corner.z,1);
                e.getPoseStack().last().pose().transform(v);
                rear = Math.min(rear,v.z);
            }
            view.z = rear - DepthwardCamera.MARGIN.get().floatValue();
            if (view.z >= -0.05f) return;
            e.getProjectionMatrix().transform(view);
            depth = view.z / view.w * 0.5f + 0.5f;
            cutDepth=depth;
            double distance=camera.distanceTo(mc.player.getEyePosition(e.getPartialTick()));
            radius = h * DepthwardCamera.RADIUS.get().floatValue()
                * (float)Math.min(2.2,Math.pow(Math.max(1,ThinWallCollision.requestedDistance()/Math.max(0.25,distance)),0.65));
            frame = true;
            VisibilityGrid.prepare(camera,mc.player.getEyePosition(e.getPartialTick()));

            CullingCompat.pause();
            previousCull = org.lwjgl.opengl.GL11C.glIsEnabled(org.lwjgl.opengl.GL11C.GL_CULL_FACE);
            restoreCull = true;


            VisibilityBackdrop.draw();
            CutSurfaces.draw(e,feet,camera);
        } else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            frame=false;
            CullingCompat.resume();
            if (restoreCull) {
                restoreCull = false;
                if (previousCull) com.mojang.blaze3d.platform.GlStateManager._enableCull();
                else com.mojang.blaze3d.platform.GlStateManager._disableCull();
            }
        }
    }
    /** Called after the target GL program is bound, including every Embeddium terrain bind. */
    public static void upload(int program) {
        if (program == 0) return;
        PlayerFade.upload(program);
        VisibilityGrid.bind(program);
        int circle = GL20C.glGetUniformLocation(program, "dwCutCircle");
        boolean active = frame && enabled();
        if (circle < 0) {
            circle = GL20C.glGetUniformLocation(program, "dwObjectCircle");
            active &= blockEntity;
        }
        if (circle < 0) return;
        GL20C.glUniform4f(circle, x, y, active ? radius : 0, DepthwardCamera.FEATHER.get().floatValue());
        GL20C.glUniform2f(GL20C.glGetUniformLocation(program, "dwCutDepth"), cutDepth, visibility);
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program,"dwCutWorld"),width,height,feetY);
        matrixBuffer.clear();
        GL20C.glUniform4f(GL20C.glGetUniformLocation(program,"dwSightClip"),x,y,frame?radius:0,depth);
        sightProjection.get(matrixBuffer);
        GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(program,"dwSightProjection"),false,matrixBuffer);
        matrixBuffer.clear();
        inverse.get(matrixBuffer);
        GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(program,"dwCutInverse"),false,matrixBuffer);
    }
    private static boolean insideSolid(Vec3 camera){
        var mc=Minecraft.getInstance();
        for(double[] offset:new double[][]{{0,0,0},{0.06,0,0},{-0.06,0,0},{0,0.06,0},{0,-0.06,0},{0,0,0.06},{0,0,-0.06}}){
            var pos=BlockPos.containing(camera.add(offset[0],offset[1],offset[2]));
            if(mc.level.getBlockState(pos).isSolidRender(mc.level,pos))return true;
        }
        return false;
    }
    private static boolean occluded(Vec3 feet, Vec3 camera) {
        var mc = Minecraft.getInstance();
        double side = mc.player.getBbWidth()*0.35;
        for (double fraction : new double[]{0.12,0.5,0.9}) {
            for(double[] shift:new double[][]{{0,0},{side,0},{-side,0},{0,side},{0,-side}}) {
                var point = feet.add(shift[0],mc.player.getBbHeight()*fraction,shift[1]);
                if (mc.level.clip(new ClipContext(point,camera,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player)).getType() == HitResult.Type.BLOCK) return true;
            }
        }
        return false;
    }
    public static boolean blockEntity(boolean value) {
        boolean old = blockEntity;
        blockEntity = value;
        upload(org.lwjgl.opengl.GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
        return old;
    }
    public static boolean nearCamera(Vec3 position) {
        if (!frame || !enabled()) return false;
        var mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        double reach = camera.distanceTo(mc.player.position()) + 3;
        return camera.distanceToSqr(position) <= reach * reach;
    }
    public static boolean worldMaskActive() { return frame && enabled(); }
    public static boolean needsOcclusionBypass() {
        return enabled()&&insideSolid(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }
    private Cutaway() {}
}
