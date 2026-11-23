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
    private static float radius,cylinderLength,circleX,circleY;
    private static float feetY, width, height;
    private static float visibility;
    private static boolean cameraEmbedded;
    private static Vec3 cylinderAxis=Vec3.ZERO;

    private static final Matrix4f inverse = new Matrix4f();

    private static final java.nio.FloatBuffer matrixBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static boolean frame;
    private static long lastFrame,holdUntil;

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
            var look=e.getCamera().getLookVector();
            cylinderAxis=new Vec3(look.x(),look.y(),look.z()).normalize();

            cameraEmbedded=insideSolid(camera);
            boolean obstructed=cameraEmbedded||occluded(feet,camera);
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


            cylinderLength=(float)CylinderMath.endAtModel(relative.x,relative.y,relative.z,
                cylinderAxis.x,cylinderAxis.y,cylinderAxis.z,
                mc.player.getBbWidth()*0.5,mc.player.getBbHeight()*0.5,DepthwardCamera.MARGIN.get());
            if(cylinderLength<=0.05)return;
            circleX=(projected.x/projected.w*.5f+.5f)*w;
            circleY=(projected.y/projected.w*.5f+.5f)*h;
            double distance=camera.distanceTo(mc.player.getEyePosition(e.getPartialTick()));
            radius = h * DepthwardCamera.RADIUS.get().floatValue()
                * (float)Math.min(2.2,Math.pow(Math.max(1,ThinWallCollision.requestedDistance()/Math.max(0.25,distance)),0.65));
            double worldRadius=Math.min(8,Math.max(.25,2*radius/h*(-view.z)/e.getProjectionMatrix().m11()));
            ThinWallMask.prepare(camera,feet,cylinderLength,worldRadius);
            frame=true;
            CullingCompat.pause();
            previousCull = org.lwjgl.opengl.GL11C.glIsEnabled(org.lwjgl.opengl.GL11C.GL_CULL_FACE);
            restoreCull = true;



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

        int circle = GL20C.glGetUniformLocation(program, "dwCutCircle");
        boolean active = frame && enabled();
        if (circle < 0) {
            circle = GL20C.glGetUniformLocation(program, "dwObjectCircle");
            active &= blockEntity;
        }
        if (circle < 0) return;
        GL20C.glUniform4f(circle,circleX,circleY,active?radius:0,DepthwardCamera.FEATHER.get().floatValue());
        GL20C.glUniform2f(GL20C.glGetUniformLocation(program,"dwCutDepth"),cameraEmbedded?1:0,visibility);
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program,"dwCutWorld"),width,height,feetY);
        GL20C.glUniform4f(GL20C.glGetUniformLocation(program,"dwCylinder"),(float)cylinderAxis.x,(float)cylinderAxis.y,(float)cylinderAxis.z,radius);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(program,"dwCylinderLength"),active?cylinderLength:0);
        ThinWallMask.upload(program,active);
        matrixBuffer.clear();
        inverse.get(matrixBuffer);
        GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(program,"dwCutInverse"),false,matrixBuffer);
    }
    /** Horizontal camera-relative back: floor/ceiling and decorative plants do not enlarge the opening. */
    private static double backWallGap(Vec3 feet){
        var mc=Minecraft.getInstance();
        Vec3 back=new Vec3(-cylinderAxis.x,0,-cylinderAxis.z);
        if(back.lengthSqr()<1e-6)return Double.POSITIVE_INFINITY;
        back=back.normalize();
        double extent=mc.player.getBbWidth()*0.5/Math.max(Math.abs(back.x),Math.abs(back.z));
        double gap=Double.POSITIVE_INFINITY;
        for(double fraction:new double[]{0.25,0.55,0.85}){
            Vec3 from=feet.add(0,mc.player.getBbHeight()*fraction,0);
            var hit=mc.level.clip(new ClipContext(from,from.add(back.scale(extent+1.5)),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()==HitResult.Type.BLOCK&&hit.getDirection().getAxis()!=net.minecraft.core.Direction.Axis.Y)
                gap=Math.min(gap,Math.max(0,from.distanceTo(hit.getLocation())-extent));
        }
        return gap;
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
