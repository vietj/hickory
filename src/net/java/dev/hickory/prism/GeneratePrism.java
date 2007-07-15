/*
 * GeneratePrism.java
 *
 * Created on 27 June 2006, 21:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.prism;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a Prism for the specified annotation, in the same package
 * as the target. 
 * <p>
 * If multiple @Prism annotations specifying the same value() (and name()) are present
 * within one package, only one Prism will be generated and no error will be raised.
 */
    @Target({ElementType.PACKAGE,ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
public @interface GeneratePrism {
    /** The annotation to generate a prism for. */
    Class<? extends java.lang.annotation.Annotation> value();
    
    /**
     * The name of the generated prism class. Defaults to XXPrism where XX is the 
     * simple name of the annotation specified by value().
     */
    String name() default "";
    
    /** 
     * Set to true for the prism to have public access, otherwise the generated
     * prism and its members will be package visible. The default is sufficient if
     * the prism is generated in the same package as the AnnotationProcessor which 
     * uses them.
     */
    boolean publicAccess() default false;
}
