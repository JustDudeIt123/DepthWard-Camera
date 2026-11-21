package dev.depthward.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Local opaque geometry; restored 1.0.10 visibility path. */
final class VisibilityGrid {
    static final int SIZE=32, UNIT=12;
    private static final java.nio.ByteBuffer DATA=BufferUtils.createByteBuffer(SIZE*SIZE*SIZE);
    private static int texture,ox,oy,oz;
    private static long tick=Long.MIN_VALUE;
    private static Object world;
    private static Vec3 camera=Vec3.ZERO,eye=Vec3.ZERO;
    private static boolean active,backdrop;
    static void prepare(Vec3 cameraPosition,Vec3 eyePosition) {
        var mc=Minecraft.getInstance();camera=cameraPosition;eye=eyePosition;
        // Below the player's support plane the eye ray crosses the floor even when
        // looking into open sky. Interior caps already close that underground view.
        active=!mc.player.isSpectator()&&!ThinWallCollision.limited()&&camera.y>=mc.player.getY()-0.1;if(!active)return;
        int nx=(int)Math.floor(mc.player.getX())-8,ny=(int)Math.floor(mc.player.getY())-7,nz=(int)Math.floor(mc.player.getZ())-8;
        if(world==mc.level&&tick==mc.level.getGameTime()&&nx==ox&&ny==oy&&nz==oz)return;
        world=mc.level;tick=mc.level.getGameTime();ox=nx;oy=ny;oz=nz;
        for(int i=0;i<DATA.capacity();i++)DATA.put(i,(byte)0);
        for(var pos:BlockPos.betweenClosed(ox,oy,oz,ox+15,oy+15,oz+15)) {
            var state=mc.level.getBlockState(pos);if(!state.canOcclude())continue;
            var boxes=state.getOcclusionShape(mc.level,pos).toAabbs();
            for(int z=0;z<2;z++)for(int y=0;y<2;y++)for(int x=0;x<2;x++) {
                double ax=x*0.5,ay=y*0.5,az=z*0.5;
                for(var b:boxes)if(b.minX<=ax+0.0001&&b.maxX>=ax+0.4999&&b.minY<=ay+0.0001&&b.maxY>=ay+0.4999&&b.minZ<=az+0.0001&&b.maxZ>=az+0.4999) {
                    int gx=(pos.getX()-ox)*2+x,gy=(pos.getY()-oy)*2+y,gz=(pos.getZ()-oz)*2+z;
                    DATA.put(gx+SIZE*(gy+SIZE*gz),(byte)255);break;
                }
            }
        }
        int previous=GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0+UNIT);
        if(texture==0)texture=GL11C.glGenTextures();
        GL11C.glBindTexture(GL12C.GL_TEXTURE_3D,texture);
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL11C.GL_TEXTURE_MIN_FILTER,GL11C.GL_NEAREST);
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL11C.GL_TEXTURE_MAG_FILTER,GL11C.GL_NEAREST);
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL11C.GL_TEXTURE_WRAP_S,GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL11C.GL_TEXTURE_WRAP_T,GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL12C.GL_TEXTURE_WRAP_R,GL12C.GL_CLAMP_TO_EDGE);
        int[] stores={GL11C.GL_UNPACK_ALIGNMENT,GL11C.GL_UNPACK_ROW_LENGTH,GL11C.GL_UNPACK_SKIP_PIXELS,GL11C.GL_UNPACK_SKIP_ROWS,GL12C.GL_UNPACK_IMAGE_HEIGHT,GL12C.GL_UNPACK_SKIP_IMAGES};
        int[] previousStores=new int[stores.length];
        int unpack=GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING);
        for(int i=0;i<stores.length;i++){previousStores[i]=GL11C.glGetInteger(stores[i]);GL11C.glPixelStorei(stores[i],i==0?1:0);}
        GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER,0);
        try{DATA.clear();GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D,0,GL30C.GL_R8,SIZE,SIZE,SIZE,0,GL11C.GL_RED,GL11C.GL_UNSIGNED_BYTE,DATA);}
        finally{GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER,unpack);for(int i=0;i<stores.length;i++)GL11C.glPixelStorei(stores[i],previousStores[i]);}
        GL13C.glActiveTexture(previous);
    }
    static void bind(int program) {
        int sampler=GL20C.glGetUniformLocation(program,"dwSightGrid");if(sampler<0)return;
        GL20C.glUniform1i(sampler,UNIT);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(program,"dwSightActive"),active&&Cutaway.worldMaskActive()&&CameraDiagnostics.sight()?1:0);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(program,"dwSightBackdrop"),backdrop?1:0);
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program,"dwSightOrigin"),(float)(ox-camera.x),(float)(oy-camera.y),(float)(oz-camera.z));
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program,"dwSightEye"),(float)(eye.x-camera.x),(float)(eye.y-camera.y),(float)(eye.z-camera.z));
        int previous=GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0+UNIT);GL11C.glBindTexture(GL12C.GL_TEXTURE_3D,texture);GL13C.glActiveTexture(previous);
    }
    static void backdrop(boolean value){backdrop=value;}
    private VisibilityGrid(){}
}
