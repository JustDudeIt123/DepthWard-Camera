import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Check reobfuscated own bytecode references against the installed runtime hierarchy. */
public final class RuntimeLinkageCheck {
 static final List<ZipFile> jars=new ArrayList<>();
 static final Map<String,ClassNode> classes=new HashMap<>();
 static ClassNode load(String name)throws Exception{
  if(classes.containsKey(name))return classes.get(name);
  InputStream input=null;
  for(var jar:jars){var e=jar.getEntry(name+".class");if(e!=null){input=jar.getInputStream(e);break;}}
  if(input==null)input=ClassLoader.getSystemResourceAsStream(name+".class");
  if(input==null)return null;
  var node=new ClassNode();try(var in=input){new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
  classes.put(name,node);return node;
 }
 static boolean member(String owner,String name,String desc,boolean method,Set<String> seen)throws Exception{
  if(!seen.add(owner))return false;var node=load(owner);if(node==null)return false;
  if(method){for(var m:node.methods)if(m.name.equals(name)&&m.desc.equals(desc))return true;}
  else{for(var f:node.fields)if(f.name.equals(name)&&f.desc.equals(desc))return true;}
  if(name.equals("<init>"))return false;
  if(node.superName!=null && member(node.superName,name,desc,method,seen))return true;
  for(String parent:node.interfaces)if(member(parent,name,desc,method,seen))return true;
  return false;
 }
 public static void main(String[] args)throws Exception{
  for(String path:Files.readAllLines(Path.of(args[1])))if(!path.isBlank())jars.add(new ZipFile(path));
  int checked=0,lambdas=0;
  try(var changed=new ZipFile(args[0])){
   for(var entries=changed.entries();entries.hasMoreElements();){
    var entry=entries.nextElement();if(!entry.getName().endsWith(".class"))continue;
    var cls=new ClassNode();new ClassReader(changed.getInputStream(entry)).accept(cls,0);
    for(var method:cls.methods)for(var insn:method.instructions){
     if(insn instanceof InvokeDynamicInsnNode indy && indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
       && indy.bsmArgs.length>0 && indy.bsmArgs[0] instanceof Type sam){
      Type target=Type.getReturnType(indy.desc);
      if(target.getSort()==Type.OBJECT && target.getInternalName().startsWith("net/minecraft/")){
       if(!member(target.getInternalName(),indy.name,sam.getDescriptor(),true,new HashSet<>()))
        throw new AssertionError(cls.name+"."+method.name+" -> missing lambda SAM "+target.getInternalName()+"."+indy.name+sam.getDescriptor());
       lambdas++;
      }
     }
     String owner,name,desc;boolean invoke;
     if(insn instanceof MethodInsnNode m){owner=m.owner;name=m.name;desc=m.desc;invoke=true;}
     else if(insn instanceof FieldInsnNode f){owner=f.owner;name=f.name;desc=f.desc;invoke=false;}
     else continue;
     if(!owner.startsWith("net/minecraft/") && !owner.startsWith("com/mojang/blaze3d/"))continue;
     if(!member(owner,name,desc,invoke,new HashSet<>()))throw new AssertionError(cls.name+"."+method.name+" -> missing "+owner+"."+name+desc);
     checked++;
    }
   }
  }finally{for(var jar:jars)jar.close();}
  System.out.println("Runtime linkage verified: "+checked+" Minecraft member references, "+lambdas+" Minecraft lambda SAMs in changed classes.");
 }
}
