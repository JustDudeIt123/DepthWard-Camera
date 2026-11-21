package dev.depthward.camera;

import java.util.regex.Pattern;

/** Pure source transformation; deliberately limited to terrain fragment programs. */
public final class ShaderPatch {
    private static final Pattern MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*(?:void\\s*)?\\)\\s*\\{");
    public static boolean terrain(String name) {
        return name.equals("rendertype_solid") || name.equals("rendertype_cutout")
            || name.equals("rendertype_cutout_mipped") || name.equals("rendertype_translucent")
            || name.equals("rendertype_tripwire") || name.equals("rendertype_translucent_moving_block");
    }
    public static String patch(String source) {
        if (source.contains("uniform vec4 dwCutCircle") || source.contains("uniform vec4 dwObjectCircle")) return source;
        var m = MAIN.matcher(source);
        if (!m.find()) throw new IllegalArgumentException("Terrain shader has no main function");
        // Wrap main so the rim is applied after normal lighting, including shaders with early returns.
        String declarations="\nuniform vec4 dwCutCircle;\nuniform vec2 dwCutDepth;\nuniform vec3 dwCutWorld;\nuniform mat4 dwCutInverse;\nuniform sampler3D dwSightGrid;\nuniform float dwSightActive;\nuniform float dwSightBackdrop;\nuniform vec3 dwSightOrigin;\nuniform vec3 dwSightEye;\nuniform vec4 dwSightClip;\nuniform mat4 dwSightProjection;\nbool dwCutBlocker(vec3 hit) {\n    // Only geometry removed by the aperture may hide newly exposed space.\n    // Ordinary roofs, floors and walls elsewhere in the scene are not a visibility mask.\n    if(hit.y<=dwCutWorld.z+0.02)return false;\n    vec4 clip=dwSightProjection*vec4(hit,1.0);\n    if(clip.w<=0.0)return true;\n    vec3 ndc=clip.xyz/clip.w;\n    if(ndc.z*0.5+0.5>=dwSightClip.w)return false;\n    vec2 pixel=(ndc.xy*0.5+0.5)*dwCutWorld.xy;\n    return length(pixel-dwSightClip.xy)<dwSightClip.z;\n}\nbool dwHidden(vec3 point) {\n    vec3 start=(dwSightEye-dwSightOrigin)*2.0;\n    vec3 target=(point-dwSightOrigin)*2.0;\n    vec3 delta=target-start;\n    float distance=length(delta);\n    if(distance<0.1)return false;\n    vec3 dir=delta/distance;\n    start+=dir*0.06;\n    ivec3 cell=ivec3(floor(start));\n    ivec3 endCell=ivec3(floor(target-dir*0.06));\n    // A visible face can be reached through another half-cell of its own block.\n    // Exclude that block only when the endpoint actually belongs to solid geometry;\n    // an air endpoint behind a thin wall must still be hidden.\n    bool solidEnd=all(greaterThanEqual(endCell,ivec3(0)))&&all(lessThan(endCell,ivec3(32)))\n        &&texelFetch(dwSightGrid,endCell,0).r>0.5;\n    ivec3 endBlock=ivec3(floor(vec3(endCell)/2.0));\n    ivec3 hop=ivec3(sign(dir));\n    vec3 stride=1.0/max(abs(dir),vec3(0.0000001));\n    vec3 edge;\n    edge.x=abs(dir.x)<0.0000001?1e20:(dir.x>0.0?float(cell.x+1)-start.x:start.x-float(cell.x))*stride.x;\n    edge.y=abs(dir.y)<0.0000001?1e20:(dir.y>0.0?float(cell.y+1)-start.y:start.y-float(cell.y))*stride.y;\n    edge.z=abs(dir.z)<0.0000001?1e20:(dir.z>0.0?float(cell.z+1)-start.z:start.z-float(cell.z))*stride.z;\n    float travel=0.0;\n    for(int i=0;i<96;i++) {\n        if(any(lessThan(cell,ivec3(0)))||any(greaterThanEqual(cell,ivec3(32)))||travel>=distance-0.12)return false;\n        bool ownSurface=solidEnd&&all(equal(ivec3(floor(vec3(cell)/2.0)),endBlock));\n        if(!ownSurface&&any(notEqual(cell,endCell))&&texelFetch(dwSightGrid,cell,0).r>0.5\n            &&dwCutBlocker(dwSightOrigin+(start+dir*(travel+0.001))*0.5))return true;\n        if(edge.x<=edge.y&&edge.x<=edge.z){travel=edge.x;edge.x+=stride.x;cell.x+=hop.x;}\n        else if(edge.y<=edge.z){travel=edge.y;edge.y+=stride.y;cell.y+=hop.y;}\n        else {travel=edge.z;edge.z+=stride.z;cell.z+=hop.z;}\n    }\n    return false;\n}\n";
        return source.substring(0,m.start())+declarations
            +source.substring(m.start(),m.end()).replaceFirst("main","dwSceneMain")+source.substring(m.end())
            +"\nvoid main() {\n    float dwRim = 0.0;\n    bool dwHiddenPixel=false;\n    vec4 dwPosition=vec4(0.0);\n    if(dwSightActive>0.5||dwCutCircle.z>0.0) {\n        dwPosition=dwCutInverse*vec4(gl_FragCoord.xy/dwCutWorld.xy*2.0-1.0,gl_FragCoord.z*2.0-1.0,1.0);\n        // Preserve the actual foreground occluder. The eye-ray mask is only\n        // a background privacy pass, not a replacement for foreground materials.\n        if(dwSightActive>0.5&&length(gl_FragCoord.xy-dwSightClip.xy)<dwSightClip.z&&(dwSightBackdrop>0.5||gl_FragCoord.z>=dwSightClip.w))\n            dwHiddenPixel=dwHidden(dwPosition.xyz/dwPosition.w);\n    }\n    if(dwSightBackdrop>0.5) {\n        if(!dwHiddenPixel)discard;\n        fragColor=vec4(0.0,0.0,0.0,1.0);\n        return;\n    }\n    if (dwCutCircle.z > 0.0 && dwCutDepth.y > 0.0 && gl_FragCoord.z < dwCutDepth.x) {\n        float dwHeight = dwPosition.y / dwPosition.w;\n        float dwR = length(gl_FragCoord.xy - dwCutCircle.xy) / dwCutCircle.z;\n        float dwCoverage = 1.0 - smoothstep(1.0 - max(dwCutCircle.w, 0.0001), 1.0, dwR);\n        float dwNoise = fract(52.9829189 * fract(dot(floor(gl_FragCoord.xy), vec2(0.06711056, 0.00583715))));\n        vec3 dwP=dwPosition.xyz/dwPosition.w;\n        vec3 dwN=cross(dFdx(dwP),dFdy(dwP));\n        bool dwSupport=abs(dwN.y)>0.85*length(dwN) && abs(dwHeight-dwCutWorld.z)<0.125;\n        if (!dwSupport && (dwHeight > dwCutWorld.z + 0.02 || dwCutWorld.z > 0.1)) {\n            if (dwR < 1.0 && dwNoise < dwCoverage * dwCutDepth.y) discard;\n            float dwBand = clamp(dwCutCircle.z * 0.035, 3.0, 12.0) / dwCutCircle.z;\n            dwRim = smoothstep(1.0 - max(dwCutCircle.w, 0.01), 1.0, dwR)\n                * (1.0 - smoothstep(1.0, 1.0 + dwBand, dwR)) * dwCutDepth.y;\n        }\n    }\n    dwSceneMain();\n    fragColor.rgb *= 1.0 - 0.82 * dwRim;\n    if(dwHiddenPixel)fragColor=vec4(0.0,0.0,0.0,1.0);\n}\n";
    }
    public static String objectPatch(String source) {
        return patch(source).replace("dwCutCircle", "dwObjectCircle");
    }
    public static String playerFade(String source) {
        if(source.contains("uniform float dwPlayerAlpha"))return source;
        var main=MAIN.matcher(source);
        if(!main.find())return source;
        return source.substring(0,main.start())+"uniform float dwPlayerAlpha;\n"
            +source.substring(main.start(),main.end())+"\n"
            +"if(dwPlayerAlpha<0.999){float n=fract(52.9829189*fract(dot(floor(gl_FragCoord.xy),vec2(0.06711056,0.00583715))));if(n>=dwPlayerAlpha)discard;}\n"
            +source.substring(main.end());
    }
    private ShaderPatch() {}
}
