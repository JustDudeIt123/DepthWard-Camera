import net.minecraftforge.srgutils.IMappingFile;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/** Development-only mapping of installed Minecraft APIs and newly compiled own classes. */
public final class RuntimeMappings {
 public static void main(String[] args)throws Exception{
  var mappings=IMappingFile.load(new File(args[0])).chain(IMappingFile.load(new File(args[1])));
  if(args[2].equals("export")){
   mappings.write(Path.of(args[3]),IMappingFile.Format.TSRG2,false);return;
  }
  boolean dev=args[2].equals("dev");if(dev)mappings=mappings.reverse();
  final var map=mappings;
  Map<String,String[]> parents=new HashMap<>();
  for(int i=5;i<args.length;i++)readParents(Path.of(args[i]),parents);
  readParents(Path.of(args[3]),parents);
  Remapper remapper=new Remapper(){
   public String map(String name){return map.remapClass(name);}
   String member(String owner,String name,String desc,boolean method,Set<String> seen){
    if(!seen.add(owner))return name;
    var cls=map.getClass(owner);
    if(cls!=null){
     String mapped=method?cls.remapMethod(name,desc):cls.remapField(name);
     if(!mapped.equals(name))return mapped;
    }
    for(String parent:parents.getOrDefault(owner,new String[0])){
     String mapped=member(parent,name,desc,method,seen);if(!mapped.equals(name))return mapped;
    }
    return name;
   }
   public String mapMethodName(String owner,String name,String desc){return member(owner,name,desc,true,new HashSet<>());}
   public String mapFieldName(String owner,String name,String desc){return member(owner,name,desc,false,new HashSet<>());}
  };
  Path out=Path.of(args[4]);Files.createDirectories(out.toAbsolutePath().getParent());
  try(ZipFile input=new ZipFile(args[3]);ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(out))){
   for(var entries=input.entries();entries.hasMoreElements();){
    var entry=entries.nextElement();if(entry.isDirectory())continue;
    byte[] data=input.getInputStream(entry).readAllBytes();String name=entry.getName();
    if(name.endsWith(".class")){
     var writer=new ClassWriter(0);
     new ClassReader(data).accept(new ClassRemapper(writer,remapper){
      @Override protected MethodVisitor createMethodRemapper(MethodVisitor visitor){
       return new MethodRemapper(visitor,remapper){
        @Override public void visitInvokeDynamicInsn(String name,String descriptor,Handle bootstrap,Object... arguments){
         // ASM does not know the functional-interface owner in its generic
         // mapInvokeDynamicMethodName hook. LambdaMetafactory provides the SAM
         // descriptor in argument 0 and the factory return type names its owner.
         if(bootstrap.getOwner().equals("java/lang/invoke/LambdaMetafactory") && arguments.length>0 && arguments[0] instanceof Type sam){
          Type owner=Type.getReturnType(descriptor);
          if(owner.getSort()==Type.OBJECT)name=remapper.mapMethodName(owner.getInternalName(),name,sam.getDescriptor());
         }
         super.visitInvokeDynamicInsn(name,descriptor,bootstrap,arguments);
        }
       };
      }
     },0);data=writer.toByteArray();
     name=remapper.mapType(name.substring(0,name.length()-6))+".class";
    }
    output.putNextEntry(new ZipEntry(name));output.write(data);output.closeEntry();
   }
  }
  System.out.println("Mapped "+args[3]+" -> "+args[4]);
 }
 static void readParents(Path jar,Map<String,String[]> into)throws IOException{
  try(var z=new ZipFile(jar.toFile())){for(var e=z.entries();e.hasMoreElements();){
   var entry=e.nextElement();if(!entry.getName().endsWith(".class"))continue;
   var reader=new ClassReader(z.getInputStream(entry));var names=new ArrayList<String>();
   if(reader.getSuperName()!=null)names.add(reader.getSuperName());Collections.addAll(names,reader.getInterfaces());
   into.putIfAbsent(reader.getClassName(),names.toArray(String[]::new));
  }}
 }
}
