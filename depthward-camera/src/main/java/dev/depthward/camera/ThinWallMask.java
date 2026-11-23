package dev.depthward.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20C;

/** Clip the cutaway at the far boundary of one/two-block walls, relative to the player. */
final class ThinWallMask {
    private static final java.nio.FloatBuffer planes=BufferUtils.createFloatBuffer(10*4);
    private static int count;
    static void prepare(Vec3 camera,Vec3 feet,double length,double radius){
        var mc=Minecraft.getInstance();
        planes.clear();count=0;
        if(mc.player.isSpectator()){planes.flip();return;}
        double reach=Math.min(16,length+radius+2);
        for(Direction direction:new Direction[]{Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH,Direction.UP,Direction.DOWN}){
            Vec3 normal=new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());
            double boundary=Double.POSITIVE_INFINITY;
            for(double fraction:new double[]{.25,.55,.85}){
                Vec3 from=feet.add(0,mc.player.getBbHeight()*fraction,0);
                // Only rays originating in free space establish which side belongs to the player.
                if(opaque(BlockPos.containing(from)))continue;
                var hit=mc.level.clip(new ClipContext(from,from.add(normal.scale(reach)),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection().getAxis()!=direction.getAxis())continue;
                var pos=hit.getBlockPos();
                if(!WallLayers.full(mc.level,pos))continue;
                double far=WallLayers.farFace(mc.level,pos,direction);
                if(!Double.isFinite(far))continue;
                int sign=direction.getStepX()+direction.getStepY()+direction.getStepZ();
                double cameraAxis=camera.get(direction.getAxis());
                boundary=Math.min(boundary,(far-cameraAxis)*sign-.01);
            }
            if(Double.isFinite(boundary)){
                planes.put((float)normal.x).put((float)normal.y).put((float)normal.z).put((float)boundary);count++;
            }
        }
        protectFront(camera,feet);
        planes.flip();
    }
    private static void protectFront(Vec3 camera,Vec3 feet){
        var mc=Minecraft.getInstance();
        Vec3 forward=new Vec3(feet.x-camera.x,0,feet.z-camera.z);
        if(forward.lengthSqr()<1e-6)return;
        forward=forward.normalize();
        Vec3 side=new Vec3(-forward.z,0,forward.x);
        double[] nearest={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        for(double fraction:new double[]{.25,.55,.85})for(double offset:new double[]{-.75,0,.75}){
            Vec3 from=feet.add(0,mc.player.getBbHeight()*fraction,0).add(side.scale(offset*mc.player.getBbWidth()*.5));
            if(opaque(BlockPos.containing(from)))continue;
            var hit=mc.level.clip(new ClipContext(from,from.add(forward.scale(1.25)),
                ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.BLOCK||!opaque(hit.getBlockPos()))continue;
            var direction=hit.getDirection().getOpposite();
            if(direction.getAxis()==Direction.Axis.Y)continue;
            Vec3 normal=new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());
            if(normal.dot(forward)<=.1)continue;
            // Keep the player side open; protect the first surface, regardless of wall thickness.
            double boundary=normal.dot(hit.getLocation().subtract(camera))-.01;
            if(boundary<=normal.dot(feet.subtract(camera))+.02)continue;
            int index=direction.getAxis()==Direction.Axis.X?(direction==Direction.EAST?0:1):(direction==Direction.SOUTH?2:3);
            nearest[index]=Math.min(nearest[index],boundary);
        }
        for(int i=0;i<4;i++)if(Double.isFinite(nearest[i])){
            float sign=i%2==0?1:-1;
            planes.put(i<2?sign:0).put(0).put(i>=2?sign:0).put((float)nearest[i]);count++;
        }
    }
    private static boolean opaque(BlockPos pos){
        var level=Minecraft.getInstance().level;
        var state=level.getBlockState(pos);
        return state.canOcclude()&&!state.getOcclusionShape(level,pos).isEmpty();
    }
    static void upload(int program,boolean active){
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program,"dwWallCount"),active?count:0);
        if(active&&count>0){planes.rewind();GL20C.glUniform4fv(GL20C.glGetUniformLocation(program,"dwWallPlanes[0]"),planes);}
    }
    private ThinWallMask(){}
}
