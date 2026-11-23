package dev.depthward.camera;
import java.util.regex.Pattern;
/** Screen-space circle, depth-limited and clipped by wall planes, shared by terrain and caps. */
public final class ShaderPatch {
    private static final Pattern MAIN=Pattern.compile("void\\s+main\\s*\\(\\s*(?:void\\s*)?\\)\\s*\\{");
    public static boolean terrain(String name){
        return name.equals("rendertype_solid")||name.equals("rendertype_cutout")
            ||name.equals("rendertype_cutout_mipped")||name.equals("rendertype_translucent")
            ||name.equals("rendertype_tripwire")||name.equals("rendertype_translucent_moving_block");
    }
    public static String patch(String source){
        if(source.contains("uniform vec4 dwCutCircle")||source.contains("uniform vec4 dwObjectCircle"))return source;
        var main=MAIN.matcher(source);
        if(!main.find())throw new IllegalArgumentException("Fragment shader has no main function");
        String declarations="""

            uniform vec4 dwCutCircle;
            uniform vec2 dwCutDepth;
            uniform vec3 dwCutWorld;
            uniform mat4 dwCutInverse;
            uniform vec4 dwCylinder;
            uniform float dwCylinderLength;
            uniform int dwWallCount;
            uniform vec4 dwWallPlanes[10];
            """;
        return source.substring(0,main.start())+declarations
            +source.substring(main.start(),main.end()).replaceFirst("main","dwSceneMain")
            +source.substring(main.end())+"""

            void main(){
                if(dwCutCircle.z>0.0&&dwCutDepth.y>0.0&&dwCylinderLength>0.0){
                    vec4 position=dwCutInverse*vec4(gl_FragCoord.xy/dwCutWorld.xy*2.0-1.0,gl_FragCoord.z*2.0-1.0,1.0);
                    vec3 p=position.xyz/position.w;
                    float along=dot(p,dwCylinder.xyz);
                    float radial=length(gl_FragCoord.xy-dwCutCircle.xy);
                    if(along>=0.0&&along<dwCylinderLength&&radial<dwCutCircle.z){
                        vec3 normal=cross(dFdx(p),dFdy(p));
                        bool support=dwCutDepth.x<0.5&&dwCutWorld.z<=0.1&&abs(normal.y)>0.85*length(normal)&&abs(p.y-dwCutWorld.z)<0.125;
                        bool wall=false;
                        for(int i=0;i<dwWallCount;i++){
                            if(dot(p,dwWallPlanes[i].xyz)>=dwWallPlanes[i].w){wall=true;break;}
                        }
                        if(!support&&!wall){
                            float coverage=1.0-smoothstep(dwCutCircle.z*(1.0-max(dwCutCircle.w,0.0001)),dwCutCircle.z,radial);
                            float noise=fract(52.9829189*fract(dot(floor(gl_FragCoord.xy),vec2(0.06711056,0.00583715))));
                            if(noise<coverage*dwCutDepth.y)discard;
                        }
                    }
                }
                dwSceneMain();
            }
            """;
    }
    public static String objectPatch(String source){return patch(source).replace("dwCutCircle","dwObjectCircle");}
    public static String playerFade(String source){
        if(source.contains("uniform float dwPlayerAlpha"))return source;
        var main=MAIN.matcher(source);if(!main.find())return source;
        return source.substring(0,main.start())+"uniform float dwPlayerAlpha;\n"
            +source.substring(main.start(),main.end())+"\n"
            +"if(dwPlayerAlpha<0.999){float n=fract(52.9829189*fract(dot(floor(gl_FragCoord.xy),vec2(0.06711056,0.00583715))));if(n>=dwPlayerAlpha)discard;}\n"
            +source.substring(main.end());
    }
    private ShaderPatch(){}
}
