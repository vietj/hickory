/*
 * MemClassLoader.java
 *
 * Created on 27 September 2007, 11:53
 *
 * based on code posted at 
 * http://www.velocityreviews.com/forums/t318697-javacompilertool.html
 *
 */

package net.java.dev.hickory.testing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;


/**
 * A class loader that loads what's in the cache by preference, and if
 * it can't find the class there, loads from the standard parent.
 *
 */
final class MemClassLoader extends ClassLoader {
    
    final Map<LocationAndKind, Map<String, JavaFileObject>> ramFileSystem;
    final private static LocationAndKind CLASS_KEY = new LocationAndKind(StandardLocation.CLASS_OUTPUT,Kind.CLASS);
    final private static LocationAndKind SOURCE_KEY = new LocationAndKind(StandardLocation.CLASS_OUTPUT,Kind.SOURCE);
    final private static LocationAndKind OTHER_KEY = new LocationAndKind(StandardLocation.CLASS_OUTPUT,Kind.OTHER);
//     TODO need to access file manager for getresource type methods
//             could use FileMagr .list maybe, may need to adjust
//             name if .class file, convert path to fqn
    MemClassLoader(Map<LocationAndKind, Map<String, JavaFileObject>> ramFileSystem) {
        this.ramFileSystem = ramFileSystem;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
//        System.out.format("findClass(%s) from %s%n",name,ramFileSystem.get(CLASS_KEY).keySet());
        JavaFileObject jfo = ramFileSystem.get(CLASS_KEY).get(name);
        if (jfo != null) {
            byte[] bytes = ((MemJavaFileObject) jfo).baos.toByteArray();
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }
    

    public InputStream getResourceAsStream(String name) {
        InputStream retValue;
        JavaFileObject jfo = getFileObject(name);
        if (jfo != null) {
            byte[] bytes = ((MemJavaFileObject) jfo).baos.toByteArray();
            return new ByteArrayInputStream(bytes);
        }
        return null;
    }
    
    private JavaFileObject getFileObject(String name) {
        LocationAndKind key = OTHER_KEY;
        if(name.endsWith(Kind.CLASS.extension)) {
            name = name.replace(".","/") + Kind.CLASS.extension;
            key = CLASS_KEY;
        } else if(name.endsWith(Kind.SOURCE.extension)) {
            name = name.replace(".","/") + Kind.SOURCE.extension;
            key = SOURCE_KEY;
        }
//        System.out.format("classloader.getFileObject() mapped to %s %s%n",name,key);
//        System.out.format("RamClassLoader: getResourceAsStream(%s)%n",name);
        if(! ramFileSystem.containsKey(key)) {
//            System.out.format("\tnone in %s%n",key);
            return null;
        }
        return ramFileSystem.get(key).get(name);
    }

    protected URL findResource(String name) {
        URL retValue;
//        System.out.format("RamClassLoader: findResource(%s)%n",name);
        
        retValue = super.findResource(name);
        if(retValue != null) {
            return retValue;
        } else {
            JavaFileObject jfo = getFileObject(name);
            if(jfo != null) {
                try {
                    return jfo.toUri().toURL();
                } catch (MalformedURLException ex) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> retValue;
//        System.out.format("RamClassLoader: findResources(%s)%n",name);
//        
        retValue = Collections.list(super.getResources(name));
        JavaFileObject jfo = getFileObject(name);
//        System.out.format("jfo for %s is %s%n",name,jfo);
        if(jfo != null) retValue.add(jfo.toUri().toURL());
        return Collections.enumeration(retValue);
    }
}
