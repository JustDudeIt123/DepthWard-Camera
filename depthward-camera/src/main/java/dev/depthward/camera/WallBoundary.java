package dev.depthward.camera;
/** Distance along a unit ray, retaining 0.15 blocks before the far wall plane. */
public final class WallBoundary {
    public static double stop(double origin,double normalComponent,double farFace){
        return Math.max(0,(farFace-origin)/normalComponent-0.15/Math.abs(normalComponent));
    }
    public static double floorStop(double hitDistance,double verticalComponent){
        return Math.max(0,hitDistance-0.15/Math.abs(verticalComponent));
    }
    public static double farFace(int firstCell,double normalComponent,int thickness){
        return normalComponent>0?firstCell+thickness:firstCell+1-thickness;
    }
    public static int[] fullLayerExtent(java.util.function.IntPredicate full){
        int low=0,high=0;
        while(low<2&&full.test(-low-1))low++;
        while(high<2&&full.test(high+1))high++;
        return low+high+1<=2?new int[]{low,high}:null;
    }
    private WallBoundary(){}
}
