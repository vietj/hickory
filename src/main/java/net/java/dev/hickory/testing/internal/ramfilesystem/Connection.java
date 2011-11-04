/*
 Connection.java
 
 *
 To change this template, choose Tools | Template Manager
 and open the template in the editor.
 */

package net.java.dev.hickory.testing.internal.ramfilesystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.tools.FileObject;

/**
 *
 * @author bchapman
 */
public class Connection extends URLConnection {
    
    FileObject fo;
    /** Creates a new instance of Connection */
    Connection(URL url, FileObject fo) {
        super(url);
        this.fo = fo;
//        System.out.format("Connection to '%s' is '%s'%n",url,fo.getName());
    }

    public void connect() throws IOException {
    }

    public InputStream getInputStream() throws IOException {
        return fo.openInputStream();
    }
    
    public String getContentType() { 
        return guessContentTypeFromName( url.getFile() ); 
    } 
    
}
