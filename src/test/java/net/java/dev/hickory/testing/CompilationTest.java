/*
 * CompilationTest.java
 * JUnit based test
 *
 * Created on 27 September 2007, 15:33
 */

package net.java.dev.hickory.testing;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import junit.framework.*;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

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

    public void testFileSystemAccessSourceOutput() throws Exception {
        fileSystemAccessTest(StandardLocation.SOURCE_OUTPUT);
    }
    
    public void testFileSystemAccessClassOutput() throws Exception {
        fileSystemAccessTest(StandardLocation.CLASS_OUTPUT);
    }

    public void testFileSystemAccessClassPath() throws Exception {
        fileSystemAccessTest(StandardLocation.CLASS_PATH);
    }

    public void testFileSystemAccessProcessorPath() throws Exception {
        fileSystemAccessTest(StandardLocation.ANNOTATION_PROCESSOR_PATH);
    }

    private void fileSystemAccessTest(JavaFileManager.Location loc) throws Exception {
        Compilation compilation = new Compilation();
        compilation.addSource("com.example.Foo")
                .addLine("package com.example;")
                .addLine("public class Foo {}");
        FileObject fo = compilation.getFile(loc, "", "resources/test");
        final OutputStream out = fo.openOutputStream();
        out.write(1);
        out.close();
        compilation.doCompile(new PrintWriter(System.out));
        FileObject fodash = compilation.getFile(loc, "", "resources/test");
        final InputStream in = fodash.openInputStream();
        assertEquals(1,in.read());
        in.close();
        assertNull(compilation.getFile(loc, "", "resources/nonexistent"));
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
//        System.out.print(gen);
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
    
    public void testpackageInfo() {
        Compilation compilation = new Compilation();
        compilation.addSource("com.example.MyAnnotation")
            .addLine("package com.example;")
            .addLine("@interface MyAnnotation {}");
        compilation.addSource("com.example.package-info")
        .addLine("/** some javadoc comment */")
        .addLine("@com.example.MyAnnotation")
        .addLine("package com.example;");
        compilation.doCompile(null);
        assertEquals(compilation.getDiagnostics().toString(),
                0,compilation.getDiagnostics().size());

    }

    public void testGeneratedElementsVisibleInNextCompile() {
        Compilation compilation = new Compilation();
        compilation.addSource("com.example.Foo")
                .addLine("package com.example;")
                .addLine(" public class Foo {}");
        ElementFinderProcessor processor = new ElementFinderProcessor();
        compilation.useProcessor(processor);
        compilation.doCompile(null);
        assertTrue(processor.processDone);
        assertTrue(processor.elements.get("com.example.Foo"));
        assertFalse(processor.elements.get("com.example.Bar"));

        // now second compile "com.example.Bar" and see if com.example.Foo is visible
        compilation = new Compilation(compilation);
        compilation.addSource("com.example.Bar")
                .addLine("package com.example;")
                .addLine("public class Bar {}");
        processor = new ElementFinderProcessor();
        compilation.useProcessor(processor);
        compilation.doCompile(null);
        assertTrue(processor.processDone);
        assertTrue(processor.elements.get("com.example.Foo"));
        assertTrue(processor.elements.get("com.example.Bar"));
    }

    @SupportedAnnotationTypes("*")
    static class ElementFinderProcessor extends AbstractProcessor {
        private Map<String,Boolean> elements = new HashMap<String, Boolean>();
        boolean processDone;

        public ElementFinderProcessor() {
            elements.put("com.example.Foo", false);
            elements.put("com.example.Bar", false);
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            processDone = true;
            Elements util = processingEnv.getElementUtils();
            for(String name : elements.keySet()) {
                if(util.getTypeElement(name) != null) {
                    elements.put(name, true);
                }
            }
            return true;
        }
    }
}
