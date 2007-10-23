/*
 * CompilationTest.java
 * JUnit based test
 *
 * Created on 27 September 2007, 15:33
 */

package net.java.dev.hickory.testing;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import junit.framework.*;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 *
 * @author Bruce
 */
public class CompilationTest extends TestCase {
    
    public CompilationTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
    public void testSuccessfulCompile() throws Exception {
        Compilation compilation = new Compilation();
        compilation.addSource("com.example.Foo")
                .addLine("package com.example;")
                .addLine(" public class Foo {")
                .addLine("    public static int answer() { return 42; }")
                .addLine("}");
        compilation.doCompile(new PrintWriter(System.out));
        assertEquals(0,compilation.getDiagnostics().size());
        Class<?> fooClass = compilation.getOutputClass("com.example.Foo");
        Method answerMethod = fooClass.getDeclaredMethod("answer");
        Object answer = answerMethod.invoke(null);
        assertEquals(Integer.valueOf(42),answer);
    }
    
    public void testBadSourceFile() {
        Compilation compilation = new Compilation();
        compilation.addSource("com.example.Foo")
                .addLine("package com.example;")
                .addLine(" public clazz Foo {}");
        compilation.doCompile(new PrintWriter(System.out));
        assertEquals(1,compilation.getDiagnostics().size());
        Class<?> fooClass;
        try {
            fooClass = compilation.getOutputClass("com.example.Foo");
            fail("expected ClassNotFoundException to be thrown");
        } catch (ClassNotFoundException ex) {
            // as expected
        }
    }
    
    public void testGeneration() throws Exception {
        System.out.format("------- starting faulty test ------%n");
        Compilation compilation = new Compilation();
        compilation.addSource("com.example.Foo")
                .addLine("package com.example;")
                .addLine(" @net.java.dev.hickory.testing.GenerateIt")
                .addLine(" public class Foo {")
                .addLine("   public static String hello=com.test.Generated.HELLO;")
                .addLine("}");
        compilation.useProcessor(new TestProcessor()); 
        compilation.doCompile(new PrintWriter(System.out)); // , "-verbose","-XprintProcessorInfo");
//        System.out.println(compilation.getDiagnostics());
        assertEquals(compilation.getDiagnostics().toString(),
                0,compilation.getDiagnostics().size());
        String gen = compilation.getGeneratedSource("com.test.Generated");
        System.out.print(gen);
        assertTrue(gen, gen.contains("class") && gen.contains(" Generated"));
        Class<?> clazz = compilation.getOutputClass("com.test.Generated");
        assertNotNull(clazz);
//        System.out.print(gen);
        RuntimeTest test = compilation.createRuntimeTest(
                new TestCode().
                addImport("junit.framework.Assert").
                addStatements("Assert.assertEquals(\"Kia Ora\",com.test.Generated.HELLO);")
        );
        test.run();
    }
    
    public void testFilerAssumptions() {
        assertEquals(".class",JavaFileObject.Kind.CLASS.extension);
        assertEquals(".java",JavaFileObject.Kind.SOURCE.extension);
        assertEquals(".html",JavaFileObject.Kind.HTML.extension);
        assertEquals("",JavaFileObject.Kind.OTHER.extension);
    }
}
