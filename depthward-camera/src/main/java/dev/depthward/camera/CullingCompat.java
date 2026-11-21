package dev.depthward.camera;

import java.lang.reflect.Field;

/** EntityCulling's CPU rays cannot see the GPU cutaway. Suspend its render checks for this world pass. */
final class CullingCompat {
    private static Field enabled;
    private static boolean checked, suspended, previous;
    static void pause() {
        if (!checked) {
            checked = true;
            try {
                enabled = Class.forName("dev.tr7zw.entityculling.versionless.EntityCullingVersionlessBase").getField("enabled");
            } catch (ClassNotFoundException absent) {
                return;
            } catch (ReflectiveOperationException e) {
                org.slf4j.LoggerFactory.getLogger("depthward_camera").warn("EntityCulling integration unavailable", e);
            }
        }
        if (enabled == null || suspended) return;
        try {
            previous = enabled.getBoolean(null);
            enabled.setBoolean(null, false);
            suspended = true;
        } catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    static void resume() {
        if (!suspended) return;
        try { enabled.setBoolean(null, previous); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        finally { suspended = false; }
    }
    private CullingCompat() {}
}
