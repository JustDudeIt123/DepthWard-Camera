import dev.depthward.camera.CameraMath;

public final class CameraMathTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        float value=0;
        for(int i=0;i<12;i++)value=CameraMath.transition(value,true,1/60f);
        check(value==1,"Fade reaches full visibility");
        value=CameraMath.transition(value,false,1/60f);
        check(value>0&&value<1,"Fade does not snap off");
        for(int i=0;i<12;i++)value=CameraMath.transition(value,false,1/60f);
        check(value==0,"Fade ends at zero");
        System.out.println("PASS: smooth aperture fade in/out.");
    }
}
