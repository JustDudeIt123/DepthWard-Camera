package dev.depthward.camera;
public final class CylinderMath {
    /** Farthest AABB projection, including corners at oblique camera angles. */
    public static double endAtModel(double x,double y,double z,double ax,double ay,double az,
                                   double halfWidth,double halfHeight,double clearance){
        double depth=x*ax+y*ay+z*az;
        double extent=halfWidth*(Math.abs(ax)+Math.abs(az))+halfHeight*Math.abs(ay);
        return depth>0?depth+extent+clearance:0;
    }
    public static double radiusScale(double wallGap){
        double t=Math.max(0,Math.min(1,1-wallGap/1.5));
        return 1+1.2*t*t*(3-2*t);
    }
    /** Centre is always sampled, regardless of animated radius; edges are included too. */
    public static double[] wallOffsets(double radius){
        var values=new java.util.ArrayList<Double>();values.add(0d);
        for(double distance=.5;distance<radius-1e-6;distance+=.5){values.add(-distance);values.add(distance);}
        if(radius>1e-6){values.add(-radius);values.add(radius);}
        double[] result=new double[values.size()];
        for(int i=0;i<result.length;i++)result[i]=values.get(i);
        return result;
    }
    private CylinderMath(){}
}
