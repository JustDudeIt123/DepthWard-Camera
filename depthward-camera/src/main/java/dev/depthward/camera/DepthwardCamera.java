package dev.depthward.camera;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.ForgeConfigSpec;

@Mod(DepthwardCamera.ID)
public final class DepthwardCamera {
    public static final String ID = "depthward_camera";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue RADIUS, FEATHER, MARGIN, CYLINDER_RADIUS;
    static {
        var b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Third-person camera cutaway. Shader packs currently disable this feature safely.").define("enabled", true);
        RADIUS = b.comment("Circle radius as a fraction of viewport height.").defineInRange("radius", 0.253, 0.05, 0.8);
        CYLINDER_RADIUS = b.comment("World-space cylinder radius in blocks; radius/0.253 scales this value.").defineInRange("cylinderRadius",1.25,0.25,4.0);
        FEATHER = b.comment("Dithered soft edge, as a fraction of circle radius.").defineInRange("feather", 0.15, 0.0, 0.8);
        MARGIN = b.comment("Small clearance behind the player's bounding box.").defineInRange("depthMargin", 0.02, 0.0, 0.25);
        SPEC = b.build();
    }
    public DepthwardCamera() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}
