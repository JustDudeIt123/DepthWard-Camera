"""Build a fresh own mod against locally installed Minecraft/Forge (no foreign JAR changes).

Requires the project's tools/java/RuntimeMappings.java and installed JDK 17+.
"""
from pathlib import Path
import argparse, os, subprocess, tempfile, zipfile

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parent

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--launcher', type=Path, required=True)
    p.add_argument('--jdk', type=Path, required=True)
    p.add_argument('--cache', type=Path, required=True)
    args = p.parse_args()
    libraries = args.launcher / 'libraries'
    cache = args.cache
    cache.mkdir(parents=True, exist_ok=True)
    java, javac = [args.jdk / 'bin' / (n + '.exe') for n in ('java', 'javac')]
    tools = [libraries / n for n in (
        'net/minecraftforge/srgutils/0.5.6/srgutils-0.5.6.jar',
        'org/ow2/asm/asm/9.9.1/asm-9.9.1.jar',
        'org/ow2/asm/asm-commons/9.9.1/asm-commons-9.9.1.jar',
        'org/ow2/asm/asm-tree/9.9.1/asm-tree-9.9.1.jar')]
    run = lambda cmd: subprocess.run(list(map(str, cmd)), check=True)
    cp = lambda paths: os.pathsep.join(map(str, paths))
    run([javac, '-encoding', 'UTF-8', '-cp', cp(tools), '-d', cache,
         ROOT/'tools/java/RuntimeMappings.java', ROOT/'tools/java/RuntimeLinkageCheck.java'])
    mapper = [java, '-cp', cp([cache]+tools), 'RuntimeMappings']
    official = libraries/'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-mappings.txt'
    srg = libraries/'de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/mcp_config-1.20.1-20230612.114412-mappings-merged.txt'
    vanilla = libraries/'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar'
    forge = libraries/'net/minecraftforge/forge/1.20.1-47.4.28/forge-1.20.1-47.4.28-client.jar'
    universal = forge.with_name('forge-1.20.1-47.4.28-universal.jar')
    for src, dest, other in [(vanilla, cache/'minecraft-official.jar', forge), (forge, cache/'forge-official.jar', vanilla)]:
        if not dest.exists(): run(mapper+[official, srg, 'dev', src, dest, other])
    extra = [f for f in libraries.rglob('*.jar') if not any(x in ('client','forge','server') for x in f.relative_to(libraries).parts)]
    devcp = [cache/'forge-official.jar', cache/'minecraft-official.jar', universal]+extra
    output = MODULE/'build/libs/depthward-camera-1.0.18.jar'
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='dw-camera-') as tmp:
        work = Path(tmp)
        classes = work/'classes'
        sources = sorted((MODULE/'src/main/java').rglob('*.java'))
        arguments = ['--release','17','-encoding','UTF-8','-proc:none','-implicit:none','-cp', cp(devcp), '-d',str(classes)]+list(map(str,sources))
        af = work/'compile.args'
        af.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in arguments), encoding='utf-8')
        run([javac, '-J-Dfile.encoding=UTF-8', '@'+str(af)])
        compiled = work/'compiled.jar'
        with zipfile.ZipFile(compiled,'w') as z:
            for f in classes.rglob('*.class'): z.write(f, f.relative_to(classes).as_posix())
        runtime = work/'runtime.jar'
        run(mapper+[official,srg,'runtime',compiled,runtime,forge,vanilla])
        runtimecp = work/'runtime-classpath.txt'
        runtimecp.write_text('\n'.join(map(str,[forge,vanilla,universal]+extra)),encoding='utf-8')
        run([java,'-cp',cp([cache]+tools),'RuntimeLinkageCheck',runtime,runtimecp])
        with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as out:
            with zipfile.ZipFile(runtime) as z:
                for name in z.namelist(): out.writestr(name,z.read(name))
            for f in (MODULE/'src/main/resources').rglob('*'):
                if f.is_file(): out.write(f,f.relative_to(MODULE/'src/main/resources').as_posix())
            out.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\r\nMixinConfigs: depthward_camera.mixins.json\r\n\r\n')
    print(output)

if __name__ == '__main__': main()
