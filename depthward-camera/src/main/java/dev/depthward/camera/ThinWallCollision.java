package dev.depthward.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One/two full-block layers: stop inside their far face; partial shapes stop at their actual surface. */
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

            var context=new ClipContext(from,from.add(delta),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity);
            double[] nearest={allowed};
            boolean[] encountered={false};
            // Visit all obstacles: a thick region must not hide a later thin wall.
            BlockGetter.traverseBlocks(from,from.add(delta),context,(ctx,pos)->{
                var shape=ctx.getBlockShape(level.getBlockState(pos),level,pos);
                var hit=shape.clip(ctx.getFrom(),ctx.getTo(),pos);
                if(hit==null)return null;
                Vec3 direction=delta.scale(1.0/distance);
                var axis=hit.getDirection().getAxis();
                double component=direction.get(axis);
                if(Math.abs(component)<1e-8)return null;
                if(!WallLayers.full(level,pos)){
                    encountered[0]=true;
                    nearest[0]=Math.min(nearest[0],WallBoundary.floorStop(from.distanceTo(hit.getLocation()),component));
                }else{
                    double farFace=WallLayers.farFace(level,pos,hit.getDirection().getOpposite());
                    if(Double.isFinite(farFace)){
                        encountered[0]=true;
                        nearest[0]=Math.min(nearest[0],WallBoundary.stop(from.get(axis),component,farFace));
                    }
                }
                return null;
            },ctx->null);
            allowed=nearest[0];thinEncountered|=encountered[0];
        }
        limited=thinEncountered;
        return allowed;
    }
    private ThinWallCollision(){}
}
