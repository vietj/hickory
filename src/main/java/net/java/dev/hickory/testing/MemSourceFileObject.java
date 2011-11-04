package net.java.dev.hickory.testing;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
/*
 * JUnitSourceFileObject.java
 *
 * Created on 9 April 2006, 16:52
 *
 */

/**
 * Models a java source file to be compiled by during a Compilation.
 * <p> 
 * Use {@link Compilation#addSource(String)} to obtain an instance then
 * populate it with {@link #addLine(String)} or obtain a Writer with {@link #openWriter()}.
 * @author Bruce
 */
public class MemSourceFileObject extends SimpleJavaFileObject {
    
    private StringBuilder contents;
    /** Creates a new instance of JUnitSourceFileObject. */
    MemSourceFileObject(String fqname) {
        super(toUri(fqname),JavaFileObject.Kind.SOURCE);
        contents = new StringBuilder(1000);
    }
    
    private static URI toUri(String fqname) {
        return URI.create(fqname.replace(".","/") + ".java");
    }

    /** Return the contents of this Source File. */
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return contents;
    }
    
    /** Add a line of source code to the contents of this Source File. 
     * @param line The source code fragment to add. May contain newline characters.
     */
    public MemSourceFileObject addLine(String line) {
        contents.append(line).append('\n');
        return this;
    }

    /** Return a Writer for writing to the contents of this Source File. */
    public Writer openWriter() {
        return new Writer() {
            public void flush() {}

            public void close(){}

            public Writer append(CharSequence csq) throws IOException {
                contents.append(csq);
                return this;
            }

            public void write(char[] cbuf, int off, int len) throws IOException {
                contents.append(cbuf,off,len);
            }
        };
    }
}
