/*
 * GeneratePrisms.java
 *
 * Created on 12 August 2006, 20:41
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
 * Use this to group several @GeneratePrism annotations
 * together, for example in a package annotation. If any of those annotations 
 * have a member element whose type is another of those annotations, then
 * the Prism for the first will use the Prism for the second.
 *<p>The target is relatively unimportant, however the generated Prism will
 * be a member of the same package as the target.
 * @author Bruce Chapman
 */
    @Target({ElementType.PACKAGE,ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
public @interface GeneratePrisms {
    /** The Prisms to be generated */
    GeneratePrism[] value();
}
