/*
 * MemFileManager.java
 *
 * Created on 27 September 2007, 11:59
 *
 * based on code posted at 
 * http://www.velocityreviews.com/forums/t318697-javacompilertool.html
 */

package net.java.dev.hickory.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import net.java.dev.hickory.testing.internal.ramfilesystem.RAMFileSystemRegistry;

/*
 * A JavaFileManager that presents the contents of the cache as a file
 * system to the compiler. To do this, it must do four things:
 *
 * It remembers our special loader and returns it from getClassLoader()
 *
 * It maintains our cache, adding class "files" to it when the compiler
 * calls getJavaFileForOutput
 *
 * It implements list() to add the classes in our cache to the result
 * when the compiler is asking for the classPath. This is the key
trick:
 * it is what makes it possible for the second compilation task to
 * compile a call to a class from the first task.
 *
 * It implements inferBinaryName to give the right answer for cached
 * classes.
 */

class MemFileManager extends
        ForwardingJavaFileManager<JavaFileManager> {
    /*
     * need a protocol handler for our URL protocol used by JavaFileObject
     * the URL of each file in CLASS_OUTPUT need to specify the protocol, and a
     * portion related to the specific FileManager.
     * FileManager needs to register itself wiht protocol handler to obtain a
     * "key", which is used to generate the urls. 
     * in here the URL returning methods return the URL, but othe code (like ServiceLoader)
     * can try to getInputStream etc from the URL, which triggers the protocol handler
     * which uses the "key" part to find the Map<String, JavaFileObject> and
     * decode the URL, file the JFO in the map, and make a stream.
     * see package doc for java.net for protocol handlers
     **/
    
    final Map<LocationAndKind, Map<String, JavaFileObject>> ramFileSystem;
    
    private final ClassLoader ldr;
    private String urlPrefix;
    
    public MemFileManager(StandardJavaFileManager sjfm) {
        super(sjfm);
        ramFileSystem = new HashMap<LocationAndKind,Map<String,JavaFileObject>>();
        Map<String,JavaFileObject> classLoaderContent = new HashMap<String,JavaFileObject>();
        ramFileSystem.put(
                new LocationAndKind(StandardLocation.CLASS_OUTPUT,Kind.CLASS),classLoaderContent);
        ldr = new MemClassLoader(ramFileSystem,this);
        urlPrefix = RAMFileSystemRegistry.getInstance().getUrlPrefix(this);
//        System.out.format("New RAMFilemanager, url prefix='%s', rfs=%s",urlPrefix, ramFileSystem);
    }
    
  /**
     * 
     * 
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    public FileObject getFileForOutput(JavaFileManager.Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
//Thread.dumpStack();
//        System.out.format("FMgr:getFileForOutput(%s,%s,%s,%s)%n",location,packageName, relativeName, sibling);
        String name = null;
        if("".equals(packageName)) {
            name = relativeName;
        } else {
            name = packageName.replace('.','/') + "/" + relativeName;
        }
        LocationAndKind key = new LocationAndKind(location,JavaFileObject.Kind.OTHER);
//        System.out.format("FMgr:getFileForOutput(%s,%s,%s,%s)%n\tname=%s%n",location,packageName, relativeName, sibling,name);
        if(ramFileSystem.containsKey(key)) {
            JavaFileObject jfo = ramFileSystem.get(key).get(name);
            if(jfo != null) {
//                System.out.format("\t returning existing file%n");
                return jfo;
            }
        }
        JavaFileObject jfo = new MemJavaFileObject(urlPrefix, name, Kind.OTHER);
        register(key,jfo);
        return jfo;
    }

    public FileObject getFileForInput(JavaFileManager.Location location, String packageName, String relativeName) throws IOException {
        throw new UnsupportedOperationException();
//        LocationAndKind key = new LocationAndKind(location,JavaFileObject.Kind.OTHER);
//        String name = packageName.replace('.','/') + "/" + relativeName;
//        if(ramFileSystem.containsKey(location)) {
//            FileObject result = ramFileSystem.get(location).get(name);
////            System.out.format("fmgr: getfileForInput(): location '%s' available - %s -> %s%n",location,name,result);
//            return result;
//        } else {
////            System.out.format("fmgr: getfileForInput(): location '%s' not found%n",location);
//            return null;
//        }
    }
 
    public JavaFileObject getJavaFileForOutput(Location location,
            String name, Kind kind, FileObject sibling)
            throws IOException {
//        System.out.format("FMgr:getJavaFileForOutput(%s,%s,%s,%s)%n",location,name, kind, sibling);
        JavaFileObject jfo = null;
        LocationAndKind key = new LocationAndKind(location,kind);

        if(ramFileSystem.containsKey(key)) {
            jfo = ramFileSystem.get(key).get(name);
            if(jfo != null) {
//                System.out.format("\t returning existing file%n");
                return jfo;
            }
        }
        if(kind == Kind.SOURCE) {
            // TODO can we treat this as same as else clause?
            jfo = new MemSourceFileObject(name);
        } else {
            jfo = new MemJavaFileObject(urlPrefix, name, kind);
        }  
        register(key,jfo);
        return jfo;
    }
    
    public JavaFileObject getJavaFileForInput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind) throws IOException {
        throw new UnsupportedOperationException();
//        JavaFileObject retValue;
//        
//        retValue = super.getJavaFileForInput(location, className, kind);
//        return retValue;
    }

    private void register(LocationAndKind key, JavaFileObject jfo) {
//        System.out.format("Registering %s %s%n",key, jfo.getName());
        if(! ramFileSystem.containsKey(key)) {
//            System.out.format("RFS does not contain %s create it old state=%n\t%s%n",key,ramFileSystem);
            ramFileSystem.put(key,new HashMap<String, JavaFileObject>());
//            System.out.format("new state=%n%s%n",ramFileSystem);
        }
        ramFileSystem.get(key).put(jfo.getName(), jfo);
//        System.out.format("filesystem now%n%s%nFMgr:getJavaFileForOutput- end%n",ramFileSystem);
    }
    
    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return ldr;
    }

    @Override
    public String inferBinaryName(Location loc, JavaFileObject jfo) {
        String result;
        if (loc == StandardLocation.CLASS_PATH && jfo instanceof MemJavaFileObject) {
            result = jfo.getName();
        } else {
            result = super.inferBinaryName(loc, jfo);
        }
        return result;
    }
    
    @Override
    public Iterable<JavaFileObject> list(Location loc, String pkg,
            Set<Kind> kinds, boolean recurse) throws IOException {
        
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();
        for(JavaFileObject f : super.list(loc, pkg, kinds, recurse)) {
            result.add(f);
        }
//        System.out.format("Looking for files in package %s%n",pkg);
        // with this if statement - still can't find clases output in compilation when 
        // compiling a runtime test
        if(loc == StandardLocation.CLASS_PATH) {
            loc = StandardLocation.CLASS_OUTPUT;
        }
//        System.out.format("list() filesystem now%n%s%n",ramFileSystem);
//        if(pkg.equals("com.test")) {
//            System.out.format("looking for com.test kinds=%s loc=%s%n",kinds,loc);
//            System.out.format("  contains CLASS_OUTPUT ? %b%n",ramFileSystem.containsKey(StandardLocation.CLASS_OUTPUT));
//            System.out.format("  CLASS_OUTPUT files = %s%n",ramFileSystem.get(loc));
//        }
        for(Kind kind : kinds) {
            LocationAndKind key = new LocationAndKind(loc,kind);
            if(ramFileSystem.containsKey(key)) {
//        if(! ramFileSystem.containsKey(loc)) return result;
                Map<String, JavaFileObject> locatedFiles = ramFileSystem.get(key);
                for(String name : locatedFiles.keySet()) {
                    String packageName = name.substring(0,name.lastIndexOf("."));
                    // when we had .class on end          packageName = packageName.substring(0,packageName.lastIndexOf("."));
//                    System.out.format("Looking at name %s (pkg = %s)%n", name,packageName);
                    if(recurse ? packageName.startsWith(pkg) : packageName.equals(pkg)) {
                        JavaFileObject candidate = locatedFiles.get(name);
//                        System.out.format("\tpkg name %s (from %s) matches arg %s for name=%s%n",
//                                packageName, name,pkg,candidate.getName());
                        if(kinds.contains(candidate.getKind())) {
                            result.add(candidate);
//                            System.out.format("\tkind %s matches %s%n",candidate.getKind(),kinds);
                        } else {
//                            System.out.format("\tkind %s DOESNT match %s%n",candidate.getKind(),kinds);
                        }
                    }
                }
            }
        }
    return result;
    }

//    public boolean hasLocation(JavaFileManager.Location location) {
//        boolean retValue;
//        
//        retValue = super.hasLocation(location);
//        System.out.format("fmgr: hasLocation(%s) super returns %b%n",location,retValue);
//        return retValue;
//    }

    public boolean isSameFile(FileObject a, FileObject b) {
        boolean retValue;
//        System.out.format("fmgr.isSameFile = %b%n\t%s%n\t%s%n",a==b,a,b);
        return a==b;
//        retValue = super.isSameFile(a, b);
//        return retValue;
    }
    
    

}
