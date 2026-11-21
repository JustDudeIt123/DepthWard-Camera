"""Compile installed shader sources and verify the depth mask using a hidden real OpenGL context."""
from pathlib import Path
import argparse, os, re, subprocess, zipfile

MODULE=Path(__file__).resolve().parents[1]

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--launcher',type=Path,required=True)
    p.add_argument('--jdk',type=Path,required=True)
    a=p.parse_args()
    work=MODULE/'build/graphics-check';work.mkdir(parents=True,exist_ok=True)
    libs=a.launcher/'libraries'
    vanilla=libs/'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-extra.jar'
    files={}
    for jar in [vanilla,MODULE.parent/'mods/embeddium-0.3.31+mc1.20.1.jar',MODULE.parent/'mods/epic-fight-20.15.2-mc1.20.1-forge.jar']:
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith('assets/') and '/shaders/' in name and name.endswith(('.fsh','.glsl')):
                    files[name]=z.read(name).decode()
    def flatten(text):
        def include(m):
            name=m[1]
            if ':' in name:
                ns,path=name.split(':',1);key=f'assets/{ns}/shaders/{path}'
            else: key=f'assets/minecraft/shaders/include/{name}'
            return re.sub(r'(?m)^\s*#version[^\n]*', '', flatten(files[key]))
        return re.sub(r'#(?:moj_import|import)\s+<([^>]+)>',include,text)
    shaders=work/'shaders';shaders.mkdir(exist_ok=True)
    for name,text in files.items():
        if name.endswith('.fsh') and ('/rendertype_' in name or '/blocks/' in name or name.endswith(('/position_color.fsh','/clouds.fsh','/solid_model.fsh'))):
            (shaders/Path(name).name).write_text(flatten(text),encoding='utf-8')
    jars=[f for f in (libs/'org/lwjgl').rglob('*.jar')
          if f.parent.name == '3.3.1' and ('natives-' not in f.name or f.name.endswith('-natives-windows.jar'))]
    cp=os.pathsep.join(map(str,jars))
    java=a.jdk/'bin/java.exe';javac=a.jdk/'bin/javac.exe'
    subprocess.run([str(javac),'-encoding','UTF-8','-cp',cp,'-d',str(work),
        str(MODULE/'src/main/java/dev/depthward/camera/ShaderPatch.java'),
        str(MODULE/'src/test/java/CameraGraphicsTest.java')],check=True)
    subprocess.run([str(java),'-cp',str(work)+os.pathsep+cp,'CameraGraphicsTest',str(shaders)],check=True)

if __name__=='__main__':main()
