/*
 Handler.java
 
 *
 To change this template, choose Tools | Template Manager
 and open the template in the editor.
 */

package net.java.dev.hickory.testing.protocol.ramfilesystem;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

/**
 *
 * @author bchapman
 */
public class Handler extends URLStreamHandler {
    
    /** Creates a new instance of Handler */
    public Handler() {
    }

    protected URLConnection openConnection(URL u) throws IOException {
//        RAMFileSystemRegistry registry = RAMFileSystemRegistry.getInstance();
        JavaFileManager jfm = RAMFileSystemRegistry.getInstance().getFileSystem(u);
        if(jfm == null) throw new IOException("ramfilesystem " + u.getHost() + " not found");
        String path = u.getPath();
        
//        System.out.format("URL '%s' path is '%s'%n",u,path);
        // next line probably should be ForInput not ForOutput - just trying Output to see if it works
        FileObject fo = jfm.getFileForOutput(StandardLocation.CLASS_OUTPUT,"",path.substring(1),null);
        if(fo == null) throw new IOException("FileObject for '" + u + "' not found");
//        System.out.format("FileObject to read is %s%n",fo);
        return new Connection(u,fo);
    }
}
