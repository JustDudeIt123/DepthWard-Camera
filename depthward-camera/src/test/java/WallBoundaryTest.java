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
        close(WallBoundary.floorStop(3,-1),2.85); // stops above entry plane, never at far floor face
        close(5-0.5*WallBoundary.floorStop(6,-0.5),2.15); // top is y=2, camera retains 0.15 clearance
        close(WallBoundary.floorStop(0.1,-1),0); // very close floor cannot yield a negative camera distance
        close(WallBoundary.stop(0,1,WallBoundary.farFace(1,1,2)),2.85);
        close(WallBoundary.stop(0,-1,WallBoundary.farFace(-2,-1,2)),2.85);
        // Partial collision surfaces: slab at 0.5, fence post at 0.375; clearance is along face normal.
        close(WallBoundary.floorStop(.5,1),.35);
        close(WallBoundary.floorStop(.375,1),.225);
        for(int thickness:new int[]{1,2,3,20})for(int cell=0;cell<thickness;cell++){
            final int at=cell,wall=thickness;
            int[] extent=WallBoundary.fullLayerExtent(offset->at+offset>=0&&at+offset<wall);
            if(thickness<=2){
                if(extent==null)throw new AssertionError("missed thin layer");
                close(extent[0]+extent[1]+1,thickness);
            }else if(extent!=null)throw new AssertionError("interior of thick wall mistaken for thin layer");
        }
        System.out.println("PASS: far face, both directions, oblique clearance and entry into wall.");
    }
}

