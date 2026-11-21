package dev.depthward.camera;

/** Frame-rate independent aperture transition. */
public final class CameraMath {
    public static float transition(float current,boolean target,float seconds) {
        float change=Math.max(0,Math.min(seconds,0.1f))/0.18f;
        return target?Math.min(1,current+change):Math.max(0,current-change);
    }
    private CameraMath() {}
}
