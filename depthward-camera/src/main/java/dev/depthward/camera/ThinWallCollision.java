package dev.depthward.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One-block surface layer: allow entry, but stop before the camera exits its far face. */
public final class ThinWallCollision {
    private static boolean limited;
    private static double requestedDistance=4;
    public static double requestedDistance(){return requestedDistance;}
    public static boolean limited(){return limited;}
    public static double vanilla(Camera camera,double distance){
        return limit(camera.getEntity(),net.minecraft.client.Minecraft.getInstance().level,
            camera.getPosition(),new Vec3(camera.getLookVector()).scale(-distance));
    }
    public static double shoulder(Camera camera,Entity entity,BlockGetter level,Vec3 offset,float tick){
        Vec3 delta=new Vec3(camera.getUpVector()).scale(offset.y)
            .add(new Vec3(camera.getLeftVector()).scale(offset.x))
            .add(new Vec3(camera.getLookVector()).scale(-offset.z));
        return limit(entity,level,entity.getEyePosition(tick),delta);
    }
    private static double limit(Entity entity,BlockGetter level,Vec3 start,Vec3 delta){
        limited=false;
        double distance=delta.length(),allowed=distance;
        requestedDistance=distance;
        boolean thinEncountered=false;
        if(entity==null||entity.isSpectator()||level==null||distance<0.001)return distance;
        // Centre plus near-plane corner rays, with a further safety clearance.
        for(int ray=-1;ray<8;ray++){
            Vec3 shift=ray<0?Vec3.ZERO:new Vec3((ray&1)==0?-0.1:0.1,(ray&2)==0?-0.1:0.1,(ray&4)==0?-0.1:0.1);
            Vec3 from=start.add(shift);
            var hit=level.clip(new ClipContext(from,from.add(delta),ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,entity));
            if(hit.getType()!=HitResult.Type.BLOCK)continue;
            var behind=hit.getBlockPos().relative(hit.getDirection().getOpposite());
            var state=level.getBlockState(behind);
            // Thickness is counted along the hit face normal, not oblique ray length.
            // A second opaque block behind this face retains the existing thick-wall cutaway.
            if(state.canOcclude()&&!state.getOcclusionShape(level,behind).isEmpty())continue;
            thinEncountered=true;
            Vec3 direction=delta.scale(1.0/distance);
            var axis=hit.getDirection().getAxis();
            double component=direction.get(axis),origin=from.get(axis);
            int cell=hit.getBlockPos().get(axis);
            double farFace=cell+(component>0?1.0:0.0);
            // World-space normal clearance protects the far face even at an oblique angle.
            double stop=WallBoundary.stop(origin,component,farFace);
            allowed=Math.min(allowed,Math.max(0,stop));
        }
        limited=thinEncountered;
        return allowed;
    }
    private ThinWallCollision(){}
}
