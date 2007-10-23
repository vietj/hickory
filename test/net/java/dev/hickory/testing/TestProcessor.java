/*
 * TestProcessor.java
 *
 * Created on 3 October 2007, 20:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.testing;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 *
 * @author Bruce
 */
@SupportedAnnotationTypes("net.java.dev.hickory.testing.GenerateIt")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestProcessor extends AbstractProcessor {
    
    /** Creates a new instance of TestProcessor */
    public TestProcessor() {
//        System.out.format("Constructed Test Processor%n");
    }
    
    private boolean done;

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        System.out.format("testProcessor.process(%s)%n",annotations);
        if(! done) {
            try {
//                System.out.format("Test Processor Running%n");
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
