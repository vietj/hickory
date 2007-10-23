/*
 RAMFileSystemRegistry.java
 
 *
 To change this template, choose Tools | Template Manager
 and open the template in the editor.
 */

package net.java.dev.hickory.testing.protocol.ramfilesystem;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import javax.tools.JavaFileManager;

/**
 * Provides a way for FileManagers to make themselves known to the protocol handler
 * and to know how to generate URLs for their FileObjects. 
 * @author bchapman
 */
public class RAMFileSystemRegistry {
    static RAMFileSystemRegistry singleton;
    
    /** Creates a new instance of RAMFileSystemRegistry */
    private RAMFileSystemRegistry() {
        String pkgName = RAMFileSystemRegistry.class.getPackage().getName();
        protocolName = pkgName.substring(pkgName.lastIndexOf(".") + 1);
//        System.out.format("handlers at %s%n",System.getProperty("java.protocol.handler.pkgs%n"));
        String pkgs = System.getProperty("java.protocol.handler.pkgs");
        String parentPackage = pkgName.substring(0,pkgName.lastIndexOf("."));
        pkgs = pkgs == null ? parentPackage : pkgs + "|" + parentPackage;
        System.setProperty("java.protocol.handler.pkgs",pkgs);
    }
    
    public static synchronized RAMFileSystemRegistry getInstance() {
        if(singleton == null) {
            singleton = new RAMFileSystemRegistry();
        }
        return singleton;
    }
    
    private String protocolName;
    private Map<String,WeakReference<JavaFileManager>> prefix2jfm = new WeakHashMap<String,WeakReference<JavaFileManager>>();
    private Map<JavaFileManager,String> jfm2prefix = new WeakHashMap<JavaFileManager,String>();
    private int sequence = 0;
    
    public String getUrlPrefix(JavaFileManager jfm) {
        // is it already registered
        if(jfm2prefix.containsKey(jfm)) {
            return jfm2prefix.get(jfm);
        } else {
            String result = protocolName + "://jfm" + (sequence++) + "/";
            jfm2prefix.put(jfm,result);
            prefix2jfm.put(result, new WeakReference<JavaFileManager>(jfm));
            return result;
        }
    }
    
    public JavaFileManager getFileSystem(URL url) {
        String prefix = url.getProtocol() + "://" + url.getHost() + "/";
        if(prefix2jfm.containsKey(prefix)) {
            return prefix2jfm.get(prefix).get();
        } else {
            return null;
        }
    }
    
}
