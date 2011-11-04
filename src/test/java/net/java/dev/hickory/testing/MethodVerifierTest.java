/*
 * MethodVerifierTest.java
 *
 * Created on 9 October 2007, 21:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.testing;



import java.io.IOException;
import junit.framework.*;

/**
 *
 * @author bchapman
 */
public class MethodVerifierTest extends TestCase {
    
    public MethodVerifierTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
    
    public void testSimple() {
        class Example {
            public String substring(int a, int b) {
                throw new UnsupportedOperationException();
            }
        }
        MethodVerifier string = new MethodVerifier(String.class);
        assertTrue(string.hasMethod(Example.class));
    }
    
    public void testPrimitiveAndWrapperAreDifferent() {
        class Example {
            public String substring(Integer a, Integer b) {
                throw new UnsupportedOperationException();
            }
        }
        MethodVerifier string = new MethodVerifier(String.class);
        assertFalse(string.hasMethod(Example.class));
    }

    public void testArrays() {
        MethodVerifier string = new MethodVerifier(String.class);
        assertTrue(string.hasMethod(StaticArrayMethodWrapper.class));
    }
    static class StaticArrayMethodWrapper {
        public static String copyValueOf(char[] arg) {
            throw new UnsupportedOperationException();
        }
    }
    
    
    public void testGeneric() {
        class Example<T> {
            public T cast(Object obj) {
                throw new UnsupportedOperationException();
            }
        }
        MethodVerifier clazz = new MethodVerifier(Class.class);
        assertTrue(clazz.hasMethod(Example.class));
    }
    
    

    public void testLocalGeneric() {
        class Example<T> {
            public <U> Class<? extends U> asSubclass(Class<U> clazz) {
                throw new UnsupportedOperationException();
            }
        }
        MethodVerifier clazz = new MethodVerifier(Class.class);
        assertTrue(clazz.hasMethod(Example.class));
        assertTrue(clazz.hasMethod("public <U> Class<? extends U> asSubclass(Class<U> clazz)","T"));
    }
    
    public void testException() {
        abstract class Example {
           public abstract int read() throws IOException ;
        }
        MethodVerifier clazz = new MethodVerifier(java.io.InputStream.class);
        assertTrue(clazz.hasMethod(Example.class));
        assertTrue(clazz.hasMethod("public abstract int read() throws IOException",null,"java.io.IOException"));
    }
}
