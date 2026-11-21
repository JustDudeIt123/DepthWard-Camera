package dev.depthward.camera;
/** Distance along a unit ray, retaining 0.15 blocks before the far wall plane. */
public final class WallBoundary {
    public static double stop(double origin,double normalComponent,double farFace){
        return Math.max(0,(farFace-origin)/normalComponent-0.15/Math.abs(normalComponent));
    }
    private WallBoundary(){}
}
