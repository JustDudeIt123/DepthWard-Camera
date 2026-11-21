import dev.depthward.camera.ShaderPatch;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.file.*;

/** Real GPU compilation and readback in an invisible window, independent of Minecraft. */
public final class CameraGraphicsTest {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static int compile(int type, String text) {
        int s = GL20C.glCreateShader(type);
        GL20C.glShaderSource(s,text); GL20C.glCompileShader(s);
        check(GL20C.glGetShaderi(s,GL20C.GL_COMPILE_STATUS)!=0,GL20C.glGetShaderInfoLog(s));
        return s;
    }
    static void pixel(int x, int y, boolean blue, String reason) {
        ByteBuffer b=BufferUtils.createByteBuffer(4);
        GL11C.glReadPixels(x,y,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,b);
        check((b.get(blue?2:0)&255)>240,reason+" r="+(b.get(0)&255)+" b="+(b.get(2)&255));
    }
    static void layer(int p, float z, boolean blue) {
        GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"z"), z);
        GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"color"),blue?0:1,0,blue?1:0,1);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES,0,3);
    }
    public static void main(String[] args) throws Exception {
        check(GLFW.glfwInit(),"GLFW initialization");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window=GLFW.glfwCreateWindow(64,64,"DepthWard Camera test",0,0);
        check(window!=0,"Hidden OpenGL context");
        try {
            GLFW.glfwMakeContextCurrent(window); GL.createCapabilities();
            System.out.println("GPU: "+GL11C.glGetString(GL11C.GL_RENDERER));
            int count=0;
            try(var files=Files.list(Path.of(args[0]))) {
                for(Path f:files.filter(p->p.toString().endsWith(".fsh")).toList()) {
                    String original=Files.readString(f);
                    String patched=ShaderPatch.playerFade(ShaderPatch.patch(original));
                    check(patched.equals(ShaderPatch.playerFade(ShaderPatch.patch(patched))),"Idempotence");
                    GL20C.glDeleteShader(compile(GL20C.GL_FRAGMENT_SHADER,patched));
                    GL20C.glDeleteShader(compile(GL20C.GL_FRAGMENT_SHADER,ShaderPatch.playerFade(ShaderPatch.objectPatch(original))));
                    count++;
                }
            }
            int vs=compile(GL20C.GL_VERTEX_SHADER,"#version 330 core\nuniform float z; void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(p*2.0-1.0,z,1.0);}");
            int fs=compile(GL20C.GL_FRAGMENT_SHADER,ShaderPatch.playerFade(ShaderPatch.patch("#version 330 core\nuniform vec4 color;out vec4 fragColor;void main(){fragColor=color;}")));
            int p=GL20C.glCreateProgram();GL20C.glAttachShader(p,vs);GL20C.glAttachShader(p,fs);GL20C.glLinkProgram(p);
            check(GL20C.glGetProgrami(p,GL20C.GL_LINK_STATUS)!=0,GL20C.glGetProgramInfoLog(p));
            GL20C.glUseProgram(p);GL30C.glBindVertexArray(GL30C.glGenVertexArrays());
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwPlayerAlpha"),1);
            GL11C.glViewport(0,0,64,64);GL11C.glEnable(GL11C.GL_DEPTH_TEST);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCutCircle"),32,32,20,0.2f);
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0.5f,1);
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-2);
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1});
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,32,true,"Foreground removed inside circle; background preserved");
            pixel(2,2,false,"Foreground preserved outside circle");
            ByteBuffer rim=BufferUtils.createByteBuffer(4);
            GL11C.glReadPixels(53,32,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,rim);
            check((rim.get(0)&255)>5&&(rim.get(0)&255)<235,"Cut boundary must have a dark rim on surviving texture");
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0.5f,0);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,32,false,"Zero fade preserves the original wall");
            pixel(53,32,false,"Zero fade removes dark rim");
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0.5f,1);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.2f,false);
            pixel(32,32,false,"Surface behind player plane must remain opaque");
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-0.1f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,16,false,"Floor below feet must remain, even inside foreground circle");
            pixel(32,40,true,"Wall above feet must still be removed");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"z"),-0.59f);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"color"),0,0,0,1);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES,0,3);
            ByteBuffer cap=BufferUtils.createByteBuffer(4);
            GL11C.glReadPixels(32,16,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,cap);
            check(cap.get(0)==0&&cap.get(1)==0&&cap.get(2)==0,"Missing inner floor face must be black, not show sky");
            pixel(32,40,true,"Cut opening must preserve actual visible background above feet");
            layer(p,-0.6f,false);
            pixel(2,2,false,"Exposed textured surface must hide inset black cap outside circle");
            pixel(32,16,false,"Existing floor texture must hide deeper black cap");
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{1,0,0,0,0,0,1,0,0,1,0,0,0,0,0,1});
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-0.5f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,32,false,"Rotated camera must reconstruct world height rather than use screen Y");
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCutCircle"),32,32,0,0.2f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,32,false,"Disabled mask preserves all geometry");

            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1});
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,0.5f);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCutCircle"),32,32,20,0.2f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,32,true,"Camera below feet looking up must cut foreground floor instead of becoming black");
            // A thin horizontal support at foot height must remain even with the camera below it.
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{1,0,0,0,0,0,1,0,0,1,0,0,0,1.1f,0,1});
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,0.55f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.6f,false);
            pixel(32,32,false,"Horizontal carpet/support at feet must stay visible with camera below feet");
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1});
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-2);
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0.5f,1);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.6f,true);layer(p,-0.2f,false);
            pixel(32,32,true,"Touching blocker between near and rear player planes must be removed");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.2f,false);
            pixel(32,32,false,"Material beyond the rear player plane must remain textured");
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0.5f,1);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCutCircle"),32,32,0,0.2f);
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-0.5f);
            ByteBuffer grid=BufferUtils.createByteBuffer(32*32*32);
            for(int z=0;z<32;z++)for(int y=0;y<32;y++)for(int x=7;x<=8;x++)grid.put(x+32*(y+32*z),(byte)255);
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0+12);
            int texture=GL11C.glGenTextures();GL11C.glBindTexture(GL12C.GL_TEXTURE_3D,texture);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL11C.GL_TEXTURE_MIN_FILTER,GL11C.GL_NEAREST);
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D,GL11C.GL_TEXTURE_MAG_FILTER,GL11C.GL_NEAREST);
            GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D,0,GL30C.GL_R8,32,32,32,0,GL11C.GL_RED,GL11C.GL_UNSIGNED_BYTE,grid);
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
            GL20C.glUniform1i(GL20C.glGetUniformLocation(p,"dwSightGrid"),12);
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwSightOrigin"),-4,-4,-4);
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwSightEye"),-1,0,0);
            float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,identity);
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwSightProjection"),false,identity);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwSightClip"),32,32,20,0.75f);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwSightActive"),1);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);layer(p,0.6f,false);
            ByteBuffer hidden=BufferUtils.createByteBuffer(4);
            GL11C.glReadPixels(56,32,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,hidden);
            pixel(56,32,false,"Visibility mask must preserve the material outside the circle");
            GL11C.glReadPixels(48,32,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,hidden);
            check(hidden.get(0)==0&&hidden.get(1)==0&&hidden.get(2)==0,"Hidden background inside the circle retains its mask");
            pixel(8,32,false,"Player-side visible space remains textured");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);layer(p,-0.2f,false);
            pixel(56,32,false,"Foreground remains textured");
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwSightActive"),0);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);layer(p,0.6f,false);
            pixel(56,32,false,"F8/spectator mask bypass restores material");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);layer(p,0.6f,true);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwPlayerAlpha"),0.3f);layer(p,-0.6f,false);
            ByteBuffer pixels=BufferUtils.createByteBuffer(64*64*4);
            GL11C.glReadPixels(0,0,64,64,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,pixels);
            int red=0;for(int i=0;i<64*64;i++)if((pixels.get(i*4)&255)>240)red++;
            check(red>1000&&red<1450,"Player fade must reveal background with about 30 percent surface coverage: "+red);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwPlayerAlpha"),1);
            layer(p,-0.8f,false);pixel(32,32,false,"Normal opacity must restore after player rendering");
            check(GL11C.glGetError()==GL11C.GL_NO_ERROR,"No OpenGL errors");
            System.out.println("PASS: "+count+" shaders; cutaway, feet, caps, circle-only privacy mask and player fade.");
        } finally { GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }
}
