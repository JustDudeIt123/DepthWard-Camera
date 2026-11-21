import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Verify the actual remapped artifact's injection selectors against installed class bytecode. */
public final class CameraHookCheck {
    static Map<String,byte[]> classes=new HashMap<>();
    static Object value(AnnotationNode a,String key) {
        if(a.values!=null)for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals(key))return a.values.get(i+1);
        return null;
    }
    static ClassNode read(String name) {
        byte[] bytes=classes.get(name);
        if(bytes==null)throw new AssertionError("Missing target "+name);
        var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;
    }
    public static void main(String[] args)throws Exception {
        for(String arg:args)try(var z=new ZipFile(arg)) {
            for(var entries=z.entries();entries.hasMoreElements();) {
                var e=entries.nextElement();
                if(e.getName().endsWith(".class"))classes.putIfAbsent(e.getName().replace(".class",""),z.getInputStream(e).readAllBytes());
            }
        }
        int checked=0;
        for(String name:new ArrayList<>(classes.keySet())) {
            if(!name.startsWith("dev/depthward/camera/mixin/"))continue;
            var mixin=read(name);String target=null;
            for(var a:mixin.invisibleAnnotations)if(a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                Object types=value(a,"value"),targets=value(a,"targets");
                if(types!=null)target=((Type)((List<?>)types).get(0)).getInternalName();
                else target=((String)((List<?>)targets).get(0)).replace('.','/');
            }
            if(target==null)throw new AssertionError("Missing mixin target "+name);
            var targetClass=read(target);Map<String,Integer> groups=new HashMap<>();
            for(var method:mixin.methods) {
                if(method.visibleAnnotations==null)continue;
                String group=null;
                for(var a:method.visibleAnnotations)if(a.desc.endsWith("/Group;"))group=(String)value(a,"name");
                for(var a:method.visibleAnnotations) {
                    if(a.desc.endsWith("/ModifyArgs;"))throw new AssertionError("Avoid generated Args classes on this Forge/ModLauncher stack: "+name);
                    if(name.endsWith("/SodiumProgramMixin") && a.desc.endsWith("/Redirect;"))throw new AssertionError("Oculus owns GlProgram redirects; camera must observe bind RETURN instead");
                    if(!a.desc.matches(".*injection/(Inject|Redirect|ModifyArgs|ModifyVariable);"))continue;
                    var selectors=(List<?>)value(a,"method");
                    List<MethodNode> candidates=new ArrayList<>();
                    for(var t:targetClass.methods)for(var s:selectors) {
                        String sel=(String)s;int paren=sel.indexOf('(');
                        if(t.name.equals(paren<0?sel:sel.substring(0,paren)) && (paren<0||t.desc.equals(sel.substring(paren))))candidates.add(t);
                    }
                    if(candidates.isEmpty())throw new AssertionError(name+" missing selector "+selectors);
                    Object at=value(a,"at");List<?> ats=at instanceof List<?> l?l:List.of(at);
                    int hits=0;
                    for(Object point:ats) {
                        var node=(AnnotationNode)point;String kind=(String)value(node,"value");
                        if(kind.equals("INVOKE")) {
                            String call=(String)value(node,"target");int semi=call.indexOf(';'),paren=call.indexOf('(');
                            String owner=call.substring(1,semi),member=call.substring(semi+1,paren),desc=call.substring(paren);
                            for(var t:candidates)for(var insn:t.instructions)if(insn instanceof MethodInsnNode m && m.owner.equals(owner)&&m.name.equals(member)&&m.desc.equals(desc))hits++;
                        } else if(kind.equals("STORE")) {
                            for(var t:candidates)for(var insn:t.instructions)if(insn.getOpcode()==Opcodes.ISTORE)hits++;
                        } else hits+=candidates.size();
                    }
                    boolean optional=Integer.valueOf(0).equals(value(a,"require"));
                    if(hits==0&&!optional)throw new AssertionError(name+" "+method.name+" has no injection site");
                    if(group!=null)groups.merge(group,hits,Integer::sum);
                    if(hits>0)checked++;
                }
            }
            for(var g:groups.entrySet())if(g.getValue()!=1)throw new AssertionError("Group must match exactly once: "+g);
        }
        System.out.println("PASS: "+checked+" injection handlers match installed runtime bytecode, including optional redirect groups.");
    }
}
