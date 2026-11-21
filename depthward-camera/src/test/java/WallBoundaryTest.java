import dev.depthward.camera.WallBoundary;
public class WallBoundaryTest {
    static void close(double actual,double expected){if(Math.abs(actual-expected)>1e-8)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        close(WallBoundary.stop(0,1,3),2.85); // wall occupies [2,3], not a stop at its entry 2
        close(WallBoundary.stop(5,-1,2),2.85);
        close(0+0.5*WallBoundary.stop(0,0.5,3),2.85); // oblique view retains normal clearance
        close(5-0.5*WallBoundary.stop(5,-0.5,2),2.15);
        close(WallBoundary.stop(2.95,1,3),0); // already in the far-face safety margin
        close(Math.min(2.5,WallBoundary.stop(0,1,3)),2.5); // entry into wall is permitted
        System.out.println("PASS: far face, both directions, oblique clearance and entry into wall.");
    }
}
