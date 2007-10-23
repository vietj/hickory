/*
 * TestCode.java
 *
 * Created on 5 October 2007, 20:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.testing;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the body of a test method.  
 *<p>
 * When testing the content of classes compiled during a test it is
 * normal that such test code cannot be compiled along with the test
 * because the classes and interfaces do not yet exist. Therefore such
 * testing code must exist in the built tests as text. This class permits
 * that textual representation inside the test body. The enclosing class and
 * method signature are generated and compiled when the runtimeTest is created
 * from this body.
 * <p> The methods of a testCode can be called in any order, specifically it is permitted 
 * to mix imports and formal parameters within statements. 
 * <p>
 * The methods of a TestCode return itself in order to chain method calls in the style
 * of StringBuffer.
 * @see Compilation#createRuntimeTest(TestCode) 
 * @see RuntimeTest for an example.
 *
 * @author Bruce
 */
public class TestCode {
    
    List<String> imports = new ArrayList<String>();
    String packageName;
    List<String> formals = new ArrayList<String>();
    StringBuilder body = new StringBuilder();
    String returnType = "void";
    /** 
     * Creates a new instance of TestCode. Initially representing an empty method with no formal
     * parameters.
     */
    public TestCode() {
    }
    
    /** Set the package which the runtime test will be a member of.
     * @param pkg The package name
     * @throws NullPointerException if pkg is null.
     * @throws IllegalStateException if this method has already been called.
     */
    public TestCode setPackage(String pkg) {
        if(packageName != null) throw new IllegalStateException("package has already been set");
        if(pkg == null) throw new NullPointerException("pkg cannot be null");
        packageName = pkg;
        return this;
    }
    
    /** Add an import which statements can be resolved against.
     * @param imported the type or wildcard to be imported. eg {@code "java.util.*"}
     */
    public TestCode addImport(String imported) {
        imports.add(imported);
        return this;
    }
    
    /** Add a formal parameter to the method.
     * @param type The type of the formal parameter either the fqn, or a type that can be resolved
     * against an import.
     * @param name The name of the formal parameter.
     * @see RuntimeTest#run(Object[])
     */
    public TestCode addFormalParameter(String type, String name) {
        formals.add(type + " " + name);
        return this;
    }
    
    /** Adds the java statement(s) to this method body. */
    public TestCode addStatements(String statements) {
        body.append(statements).append('\n');
        return this;
    }
    
    /** 
     * Specify the return type of the test method. Defaults to {@code void} if not set.
     */
    public TestCode setReturnType(String type) {
        if(returnType != "void") {
            // == is OK, both strings are in source code and thus interned.
            throw new IllegalStateException("may only be called once");
        }
        returnType = type;
        return this;
    }
}
