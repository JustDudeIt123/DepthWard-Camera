package dev.depthward.camera;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/** Shared normal-thickness classification for camera motion and cutaway planes. */
final class WallLayers {
    static double farFace(BlockGetter level,BlockPos pos,Direction outward){
        int[] extent=WallBoundary.fullLayerExtent(offset->full(level,pos.relative(outward,offset)));
        if(extent==null)return Double.NaN;
        int high=extent[1];
        int coordinate=pos.get(outward.getAxis());
        int sign=outward.getStepX()+outward.getStepY()+outward.getStepZ();
        return sign>0?coordinate+high+1:coordinate-high;
    }
    static boolean full(BlockGetter level,BlockPos pos){return level.getBlockState(pos).isCollisionShapeFullBlock(level,pos);}
    private WallLayers(){}
}
