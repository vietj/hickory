/*
 * StateSaverTest.java
 * JUnit based test
 *
 * Created on 10 October 2007, 21:42
 */

package net.java.dev.hickory.incremental;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ServiceLoader;
import junit.framework.*;
import net.java.dev.hickory.testing.Compilation;
import net.java.dev.hickory.testing.RuntimeTest;
import net.java.dev.hickory.testing.TestCode;

/**
 *
 * @author Bruce
 */
public class StateSaverTest extends TestCase {
    
    public StateSaverTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
    static String serviceSrc =
            "package foo.bar;\n" +
            "public interface Service {\n" +
            "    void doIt();\n" +
            "}\n";
    static String serviceFqn = "foo.bar.Service";
    
    static String provider1Src =
            "package bar.foo;\n" +
            "@net.java.dev.hickory.incremental.ServiceProvider(foo.bar.Service.class)\n" +
            "public class ProviderOne implements foo.bar.Service {\n" +
            "    public void doIt() {}\n;" +
            "}";
    static String provider1Fqn = "bar.foo.ProviderOne";

    static String provider2Src =
            "package bar.foo;\n" +
            "@net.java.dev.hickory.incremental.ServiceProvider(foo.bar.Service.class)\n" +
            "public class ProviderTwo implements foo.bar.Service {\n" +
            "    public void doIt() {}\n;" +
            "}";
    static String provider2Fqn = "bar.foo.ProviderTwo";
    
    static TestCode serviceCounter = new TestCode()
            .addFormalParameter("int", "expected")
            .addFormalParameter("ClassLoader","classLoader")
            .addImport("java.util.ServiceLoader")
            .addImport(serviceFqn)
            .addStatements("ServiceLoader<Service> loader = ServiceLoader.load(" + serviceFqn + ".class,classLoader);")
            .addStatements("int rslt=0;")
            .addStatements(" for(Service s : loader) {")
            .addStatements("    rslt++;")
            .addStatements("}")
            .addImport("junit.framework.Assert")
            .addStatements("Assert.assertEquals(expected,rslt);")
            ;

    /**
     * Test of getInstance method, of class net.java.dev.hickory.incremental.StateSaver.
     */
    public void testSingle() throws Exception {
        Compilation compilation = new Compilation();
        compilation.addSource(serviceFqn).addLine(serviceSrc);
        compilation.addSource(provider1Fqn).addLine(provider1Src);
        compilation.useProcessor(new ServiceProviderProcessor());
        compilation.doCompile(new PrintWriter(System.err));
        assertEquals(compilation.getDiagnostics().toString(),0,compilation.getDiagnostics().size());
        InputStream in = compilation.getOutputClassLoader().getResourceAsStream("META-INF/services/foo.bar.Service");
        assertNotNull(in);
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line = r.readLine();
        assertNotNull(line);
        assertEquals(provider1Fqn,line);
        assertNull(r.readLine());
        r.close();
        Class<?> foobarService = compilation.getOutputClass(serviceFqn);
        assertNotNull(foobarService);
        ServiceLoader<?> services = ServiceLoader.load(foobarService,compilation.getOutputClassLoader());
        for(Object o : services) {
            System.out.format("%s is a foo bar service%n",o.getClass());
        }
        RuntimeTest test = compilation.createRuntimeTest(serviceCounter);
        test.run(1,compilation.getOutputClassLoader());
    }

    public void testTwoCompiles() throws Exception {
        // first clean compile, compile Service and one provider
        // assert that service list contains just that one
        Compilation compilation = new Compilation();
        compilation.addSource(serviceFqn).addLine(serviceSrc);
        compilation.addSource(provider1Fqn).addLine(provider1Src);
        compilation.useProcessor(new ServiceProviderProcessor());
        compilation.doCompile(new PrintWriter(System.err));
        assertEquals(compilation.getDiagnostics().toString(),0,compilation.getDiagnostics().size());
        InputStream in = compilation.getOutputClassLoader().getResourceAsStream("META-INF/services/foo.bar.Service");
        assertNotNull(in);
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line = r.readLine();
        assertNotNull(line);
        assertEquals(provider1Fqn,line);
        assertNull(r.readLine());
        r.close();
        
        // second compile (incremental) provider2 only
        // assert service list file contains both providers
        compilation = new Compilation(compilation);
        compilation.addSource(provider2Fqn).addLine(provider2Src);
        compilation.useProcessor(new ServiceProviderProcessor());
        compilation.doCompile(new PrintWriter(System.err));
        assertEquals(compilation.getDiagnostics().toString(),0,compilation.getDiagnostics().size());
        in = compilation.getOutputClassLoader().getResourceAsStream("META-INF/services/foo.bar.Service");
        assertNotNull(in);
        r = new BufferedReader(new InputStreamReader(in));
        line = r.readLine();
        assertNotNull(line);
        assertEquals(provider1Fqn,line);
        line = r.readLine();
        assertNotNull(line);
        assertEquals(provider2Fqn,line);
        assertNull(r.readLine());
        r.close();
    }
}