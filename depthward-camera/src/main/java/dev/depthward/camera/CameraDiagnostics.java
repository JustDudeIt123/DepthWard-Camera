package dev.depthward.camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
@Mod.EventBusSubscriber(modid=DepthwardCamera.ID,value=Dist.CLIENT)
public final class CameraDiagnostics {
    private static final KeyMapping KEY=new KeyMapping("Диагностика просвета камеры",GLFW.GLFW_KEY_F8,"DepthWard Camera");
    private static int mode;
    static boolean interiors(){return mode==0||mode==2;}
    static boolean sight(){return mode==0||mode==1;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null){mode=0;while(KEY.consumeClick()){}return;}
        while(KEY.consumeClick()){
            mode=(mode+1)%4;
            String text=switch(mode){case 1->"1/3: чёрные внутренние поверхности отключены";case 2->"2/3: маска видимости отключена, внутренние поверхности включены";case 3->"3/3: обе чёрные маски отключены";default->"обычный режим: обе маски включены";};
            String message="DepthWard Camera — "+text;
            if(mc.player!=null)mc.player.displayClientMessage(Component.literal(message),true);
            System.out.println("[DepthWard Camera diagnostics] "+message);
        }
    }
    @Mod.EventBusSubscriber(modid=DepthwardCamera.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration{
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(KEY);}
    }
    private CameraDiagnostics(){}
}
