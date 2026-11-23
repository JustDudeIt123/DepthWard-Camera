import dev.depthward.camera.CylinderMath;
public class CylinderMathTest {
    static void close(double actual,double expected){if(Math.abs(actual-expected)>1e-8)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        close(CylinderMath.endAtModel(0,0,4,0,0,1,.3,.9,.02),4.32);
        close(CylinderMath.endAtModel(.8,-.7,4,0,0,1,.3,.9,.02),4.32);
        close(CylinderMath.endAtModel(0,4,0,0,1,0,.3,.9,.02),4.92);
        double a=Math.sqrt(.5);
        double end=CylinderMath.endAtModel(4,0,4,a,0,a,.3,.9,.02);
        close(end,Math.sqrt(32)+.6*a+.02);
        // Every AABB corner must be before the end, not just its centre.
        for(double x:new double[]{3.7,4.3})for(double z:new double[]{3.7,4.3})
            if(x*a+z*a>=end)throw new AssertionError("uncut corner");
        close(CylinderMath.endAtModel(0,0,-4,0,0,1,.3,.9,.02),0);
        close(CylinderMath.radiusScale(Double.POSITIVE_INFINITY),1);
        close(CylinderMath.radiusScale(1.5),1);
        close(CylinderMath.radiusScale(.75),1.6);
        close(CylinderMath.radiusScale(0),2.2);
        close(CylinderMath.radiusScale(-.1),2.2);
        for(double radius:new double[]{.25,1,1.25,1.63,2.2}){
            double[] offsets=CylinderMath.wallOffsets(radius);
            close(offsets[0],0);
            boolean left=false,right=false;
            for(double offset:offsets){
                if(offset < -radius-1e-8 || offset > radius+1e-8)throw new AssertionError("outside radius");
                boolean mirrored=false;for(double other:offsets)if(Math.abs(offset+other)<1e-8)mirrored=true;
                if(!mirrored)throw new AssertionError("asymmetric rays");
                left|=Math.abs(offset+radius)<1e-8;right|=Math.abs(offset-radius)<1e-8;
            }
            if(!left||!right)throw new AssertionError("missing boundary rays");
        }
        System.out.println("PASS: camera axis, shoulder offset, all corner depths and bounded proximity growth.");
    }
}
