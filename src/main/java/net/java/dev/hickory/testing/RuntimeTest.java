/*
 * RuntimeTest.java
 *
 * Created on 5 October 2007, 20:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.testing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * A test method which is generated and compiled at runtime in order
 * to access and test classes resulting from a compilation.
 * <p>
 * For Example, given the Processor
 * <pre>
 * {@literal
}{@literal @SupportedAnnotationTypes("net.java.dev.hickory.testing.GenerateIt")
}{@literal @SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestProcessor extends AbstractProcessor {
    public TestProcessor() {}
    private boolean done;
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(! done) {
            try {
                done = true;
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile("com.test.Generated");
                PrintWriter out = new PrintWriter(jfo.openWriter());
                out.println("package com.test;");
                out.println("public class Generated {");
                out.println("    public final static String HELLO=\"Kia Ora\";");
                out.println("}");
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }
}
* }
 * </pre>
 * The following JUnit test body creates a RuntimeTest to assert the value 
 * of the HELLO String constant in the generated class has been used to initialize
 * the field Foo.hello.
 * <pre>
 * {@literal
 * Compilation compilation = new Compilation();
 * compilation.addSource("com.example.Foo")
 *     .addLine("package com.example;")
 *     .addLine(" @net.java.dev.hickory.testing.GenerateIt")
 *     .addLine(" public class Foo {")
 *     .addLine("   public static String hello=com.test.Generated.HELLO;")
 *     .addLine("}");
 * compilation.useProcessor(new TestProcessor());
 * compilation.doCompile(new PrintWriter(System.out));
 * assertEquals(compilation.getDiagnostics().toString(),
 *     0,compilation.getDiagnostics().size());
 * RuntimeTest test = compilation.createRuntimeTest(
 *     new TestCode().
 *         addImport("junit.framework.Assert").
 *         addStatements("Assert.assertEquals(\"Kia Ora\",com.test.Generated.HELLO);")
 * );
 * test.run();
 * }
 * </pre>
 *@see Compilation#createRuntimeTest(TestCode) 
 * @author Bruce
 */
public class RuntimeTest {
    
    private static int counter = 0;
    private Method testMethod;
    private Class<?> testClass;
    
    /** Creates a new instance of RuntimeTest */
    RuntimeTest(TestCode code, MemFileManager jfm) throws IllegalArgumentException {
//        ClassLoader loader
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        String className = "Generated_RuntimeTest_" + (counter++);
        String fqn = code.packageName == null ? className : code.packageName + "." + className;
        MemSourceFileObject src = new MemSourceFileObject(fqn);
        if(code.packageName != null) {
            src.addLine("package " + code.packageName + ";");
        }
        for(String imp : code.imports) {
            src.addLine("import " + imp + ";");
        }
        src.addLine("final public class " + className + "{");
        String methodName = "run" ;
        StringBuilder b = new StringBuilder();
        b.append("public static ").append(code.returnType).append(" ").append(methodName).append("(");
        String sep = "";
        for(String formal : code.formals) {
            b.append(sep).append(formal);
            sep = ", ";
        }
        b.append(") throws Exception {");
        src.addLine(b.toString());
        src.addLine(code.body.toString());
        src.addLine("}}");
        List<? extends JavaFileObject> jfos = Arrays.asList(src);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,jfm,diagnostics,Arrays.asList("-classpath",System.getProperty("java.class.path")),null,jfos);
        if(task.call()) {
            try {
                testClass = jfm.getClassLoader(StandardLocation.CLASS_OUTPUT).loadClass(fqn);
                testMethod = testClass.getDeclaredMethods()[0];
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("Unexpected exception",ex);
            }
        } else {
            // compiler errors
            // System.out.print(diagnostics.getDiagnostics());
            String sourceCode=null;
            sourceCode = src.getCharContent(false).toString();
            throw new IllegalArgumentException(diagnostics.getDiagnostics().toString() + "\n" + sourceCode);
        }
    }
    
    /**
     * Run the runtime test, passing any arguments for the test defined with {@link TestCode#addFormalParameter(String,String)}
     * @param args the arguments. Types should match those defined for the test method.
     * @return the return value from the method or {@code null} if this method's return type is {@code void}.
     * @throws Exception when a statement in the test body throws that Exception.
     * @throws IllegalArgumentException if the arguments do not match.
     **/
    public Object run(Object... args) throws IllegalArgumentException, Exception {
        try {
            return testMethod.invoke(null,(Object[])args);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if(cause instanceof Exception) throw (Exception)cause;
            if(cause instanceof Error) throw (Error)cause;
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("unexpected",ex);
        }
    }
}
