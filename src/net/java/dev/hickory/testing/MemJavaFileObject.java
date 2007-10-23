/*
 * RAMJavaFileOubject.java
 *
 * Created on 27 September 2007, 11:57
 *
 * based on code posted at 
 * http://www.velocityreviews.com/forums/t318697-javacompilertool.html
 */

package net.java.dev.hickory.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

/**
 * A JavaFileObject that uses RAM instead of disk to store the file. It
 * gets written to by the compiler, and read from by the loader.
 */

class MemJavaFileObject extends SimpleJavaFileObject {
    
    ByteArrayOutputStream baos;
    String name;
    
    MemJavaFileObject(String urlPrefix, String name, Kind kind) {
//        super(URI.create("string:///" + name.replace('.','/') + kind.extension), kind);
        super(URI.create(urlPrefix + name + kind.extension), kind);
        this.name = name;
//        System.out.format("Creating MemJavaFileObject %s (%s)",name,kind);
    }
    
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    throws IOException, IllegalStateException,
            UnsupportedOperationException {
        if(baos == null) throw new FileNotFoundException(name);
        return new String(baos.toByteArray());
    }
    
    @Override
    public InputStream openInputStream() throws IOException,
            IllegalStateException, UnsupportedOperationException {
        if(baos == null) throw new FileNotFoundException(name);
        return new ByteArrayInputStream(baos.toByteArray());
    }
    
    @Override
    public OutputStream openOutputStream() throws IOException,
            IllegalStateException, UnsupportedOperationException {
        return baos = new ByteArrayOutputStream();
    }

    public String getName() {
        return name;
    }
}
