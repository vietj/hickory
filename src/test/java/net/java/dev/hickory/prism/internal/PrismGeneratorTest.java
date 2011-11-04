/*
 * PrismGeneratorTest.java
 * JUnit based test
 *
 * Created on 29 October 2007, 21:57
 */

package net.java.dev.hickory.prism.internal;

import junit.framework.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import net.java.dev.hickory.testing.*;

/**
 *
 * @author Bruce
 */
public class PrismGeneratorTest extends TestCase {
    
    public PrismGeneratorTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testStringMemberType() throws Exception {
        testMemberType("String","String");
    }
    
    public void testByteMemberType() throws Exception {
        testMemberType("byte", "Byte");
    }
    
    public void testCharMemberType() throws Exception {
        testMemberType("char", "Character");
    }
    
    public void testShortMemberType() throws Exception {
        testMemberType("short", "Short");
    }
    
    public void testIntMemberType() throws Exception {
        testMemberType("int", "Integer");
    }
    
    public void testLongMemberType() throws Exception {
        testMemberType("long", "Long");
    }
    public void testFloatMemberType() throws Exception {
        testMemberType("float", "Float");
    }
    
    public void testDoubleMemberType() throws Exception {
        testMemberType("double", "Double");
    }
    
    public void testBooleanMemberType() throws Exception {
        testMemberType("boolean", "Boolean");
    }
    
    public void testPrimitiveArrayMemberType() throws Exception {
        testMemberType("byte[]", "java.util.List<Byte>");
    }
    
    public void testSimpleClassMemberType() throws Exception {
        testMemberType("Class", "javax.lang.model.type.TypeMirror");
    }
    
    public void testGenericClassMemberType() throws Exception {
        testMemberType("Class<? extends Number>", "javax.lang.model.type.TypeMirror");
    }
    
    public void testEnumMemberType() throws Exception {
        testMemberType("java.lang.annotation.ElementType", "String");
    }
    
    public void testAnnotationMemberType() throws Exception {
        testMemberType("java.lang.annotation.Documented", "com.me.TestAnnotationPrism.Documented");
    }
    
    private void testMemberType(String annotationMemberType, String PrismMemberType) throws Exception {
        Compilation comp = new Compilation();
        comp.addSource("com.me.TestAnnotation")
            .addLine("package com.me;")
            .addLine("@interface TestAnnotation {")
            .addLine(annotationMemberType + " member();")
            .addLine("}");
        comp.addSource("com.me.package-info")
            .addLine("@net.java.dev.hickory.prism.GeneratePrism(value=com.me.TestAnnotation.class,publicAccess=true)")
            .addLine("package com.me;");
        comp.doCompile(null);
        assertEquals(comp.getDiagnostics().toString(),
                1,comp.getDiagnostics().size());
        assertEquals(comp.getDiagnostics().get(0).getKind(),Diagnostic.Kind.NOTE);
        Class<?> prismClass = comp.getOutputClass("com.me.TestAnnotationPrism");
        assertNotNull(prismClass);
        MethodVerifier prismVerifier = new MethodVerifier(prismClass);
        assertTrue(prismVerifier.hasMethod("public " + PrismMemberType + " member()",null));
    }
    
    public void testDefaults() {
        testValues("@javax.annotation.Generated(\"hello\")","","","hello");
    }
    
    public void testArraySingleValue() {
        testValues("@javax.annotation.Generated(value={\"hello\"})","","","hello");
    }

    public void testArrayMultiValue() {
        testValues("@javax.annotation.Generated(value={\"A\",\"B\"})","","","A","B");
    }
    
    public void testNonDefaults() {
        testValues("@javax.annotation.Generated(value={\"A\",\"B\"},comments=\"why not\",date=\"today\")",
                "today","why not","A","B");
    }
    
    /* this deliberately fails if uncommented 
     - just a sanity check that we can detect failure
    public void testForTestfailure() {
        testValues("@javax.annotation.Generated(value={\"A\",\"B\"},comments=\"why not\",date=\"today\")",
                "","why","B");
    }
    */
    

    private void testValues(String annotation, String expectedDate, String expectedComments,
            String... expectedValue) {
        Compilation comp = new Compilation();
        comp.addSource("com.me.Test")
            .addLine("package com.me;")
            .addLine(annotation)
            .addLine("class Test {}");
        GeneratedPrismChecker proc = new GeneratedPrismChecker(expectedDate,expectedComments,expectedValue);
        comp.useProcessor(proc);
        comp.doCompile(null);
        assertEquals(comp.getDiagnostics().toString(),0,comp.getDiagnostics().size());
    }
}
