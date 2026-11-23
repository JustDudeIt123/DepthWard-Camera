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
            float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,identity);
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-2);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCutCircle"),32,32,16,0.15f);
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0,1);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCylinder"),0,0,1,0.5f);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwCylinderLength"),0.5f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.2f,false);
            pixel(32,32,true,"Cylinder must remove the camera-side blocker");
            pixel(2,2,false,"Materials outside the cylinder must remain textured");
            GL20C.glUniform1i(GL20C.glGetUniformLocation(p,"dwWallCount"),1);
            GL20C.glUniform4fv(GL20C.glGetUniformLocation(p,"dwWallPlanes[0]"),new float[]{0,0,1,.3f});
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.2f,false);
            pixel(32,32,true,"Player-side wall face must still cut before far boundary");
            GL20C.glUniform1i(GL20C.glGetUniformLocation(p,"dwWallCount"),2);
            GL20C.glUniform4fv(GL20C.glGetUniformLocation(p,"dwWallPlanes[0]"),new float[]{0,0,1,.3f,0,0,1,.19f});
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.2f,false);
            pixel(32,32,false,"Front wall nearest face must remain opaque even inside the cylinder end");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.1f,false);
            pixel(32,32,true,"Blocker before the front wall still cuts");
            GL20C.glUniform1i(GL20C.glGetUniformLocation(p,"dwWallCount"),1);

            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.4f,false);
            pixel(32,32,false,"Far face must terminate cutaway");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"z"),.4f);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"color"),0,0,0,1);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES,0,3);
            ByteBuffer sealed=BufferUtils.createByteBuffer(4);
            GL11C.glReadPixels(32,32,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,sealed);
            check(sealed.get(0)==0&&sealed.get(1)==0&&sealed.get(2)==0,"Far black cap seals the other room");
            GL20C.glUniform4fv(GL20C.glGetUniformLocation(p,"dwWallPlanes[0]"),new float[]{1,0,0,0});
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.2f,false);
            pixel(40,32,false,"Right wall clips the cylinder");
            pixel(24,32,true,"Player side of right wall remains open");
            GL20C.glUniform4fv(GL20C.glGetUniformLocation(p,"dwWallPlanes[0]"),new float[]{-1,0,0,0});
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.2f,false);
            pixel(24,32,false,"Left wall clips the cylinder");
            pixel(40,32,true,"Player side of left wall remains open");
            GL20C.glUniform1i(GL20C.glGetUniformLocation(p,"dwWallCount"),0);

            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.51f,false);
            pixel(32,32,false,"Unselected next block beyond the selected volume remains opaque");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,-0.2f,false);
            pixel(32,32,false,"Behind-camera geometry must not be cut");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"z"),0.2f);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"color"),0,0,0,1);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES,0,3);
            pixel(32,32,true,"Black interior cap must use exactly the same cylinder opening");
            ByteBuffer black=BufferUtils.createByteBuffer(4);
            GL11C.glReadPixels(2,2,1,1,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,black);
            check(black.get(0)==0&&black.get(1)==0&&black.get(2)==0,"Exposed interiors outside cylinder remain sealed");
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCylinder"),0.70710677f,0,0.70710677f,0.5f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.2f,false);
            pixel(40,32,true,"Oblique cylinder must remove points inside its segment");
            pixel(24,32,false,"Oblique cylinder must not extend behind its start");
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwCylinderLength"),0.65f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.45f,false);
            pixel(40,32,true,"Selected corner face must be cut even beyond old end plane");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.7f,false);
            pixel(40,32,false,"Surface outside all selected block volumes must remain opaque");
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{1,0,0,0,0,0,1,0,0,1,0,0,0,0,0,1});
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-0.5f);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCylinder"),0,-1,0,0.8f);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwCylinderLength"),1);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,-0.5f,false);
            pixel(32,32,false,"Horizontal support and carpets at feet must not be cut by cylinder");
            // Same support height and surface, but camera is now embedded in a solid block.
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),1,1);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,-.5f,false);
            pixel(32,32,true,"Embedded camera must cut support without waiting to drop below the feet threshold");
            GL20C.glUniform2f(GL20C.glGetUniformLocation(p,"dwCutDepth"),0,1);

            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,.5f);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwCylinderLength"),.7f);
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCylinder"),0,1,0,.8f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.5f,false);
            pixel(32,32,true,"Floor at player feet must cut when camera is below it");

            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,identity);
            GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(p,"dwCutInverse"),false,new float[]{10,0,0,0,0,10,0,0,0,0,1,0,0,0,0,1});
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCylinder"),0,0,1,.5f);
            GL20C.glUniform3f(GL20C.glGetUniformLocation(p,"dwCutWorld"),64,64,-2);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwCylinderLength"),.5f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,.8f,true);layer(p,.2f,false);
            pixel(40,32,true,"Screen circle radius must be independent of world-space cross section");
            pixel(2,2,false,"Screen circle preserves material outside its pixel radius");
            GL20C.glUniform4f(GL20C.glGetUniformLocation(p,"dwCutCircle"),0,0,0,0.15f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);
            layer(p,0.8f,true);layer(p,0.2f,false);
            pixel(32,32,false,"Disabled cylinder must preserve all textures");
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT|GL11C.GL_DEPTH_BUFFER_BIT);layer(p,0.8f,true);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwPlayerAlpha"),0.3f);layer(p,0.2f,false);
            ByteBuffer pixels=BufferUtils.createByteBuffer(64*64*4);
            GL11C.glReadPixels(0,0,64,64,GL11C.GL_RGBA,GL11C.GL_UNSIGNED_BYTE,pixels);
            int red=0;for(int i=0;i<64*64;i++)if((pixels.get(i*4)&255)>240)red++;
            check(red>1000&&red<1450,"Player fade must reveal background at 30 percent coverage: "+red);
            GL20C.glUniform1f(GL20C.glGetUniformLocation(p,"dwPlayerAlpha"),1);
            layer(p,-0.8f,false);pixel(32,32,false,"Normal opacity restores after player render");
            check(GL11C.glGetError()==GL11C.GL_NO_ERROR,"No OpenGL errors");
            System.out.println("PASS: "+count+" shaders; screen circle, clipped wall planes, depth bounds, black caps, carpet and player fade.");
        } finally { GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }
}