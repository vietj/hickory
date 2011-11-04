/*
 * GeneratedPrismChecker.java
 *
 * Created on 5 November 2007, 22:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.prism.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import net.java.dev.hickory.prism.GeneratePrism;


/**
 * checks @Generated annotations to make sure their element values 
 * correspond to the values used in the constructor of the processor
 * @author Bruce
 */
@SupportedAnnotationTypes("javax.annotation.Generated")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@GeneratePrism(Generated.class)
public class GeneratedPrismChecker extends AbstractProcessor {
    
    /** Creates a new instance of GeneratedPrismChecker */
    public GeneratedPrismChecker() {
    }
    
    String date,comments;
    List<String> value;
    GeneratedPrismChecker(String date, String comments, String... value) {
        this.date = date;
        this.comments = comments;
        this.value = Arrays.asList((String[])value);
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager msg = processingEnv.getMessager();
        for(TypeElement e : annotations) {
            for(Element target : roundEnv.getElementsAnnotatedWith(e)) {
                GeneratedPrism prism = GeneratedPrism.getInstanceOn(target);
                if(! date.equals(prism.date())) {
                    msg.printMessage(Diagnostic.Kind.ERROR,"date wrong - expected " + date + " was " + prism.date());
                }
                if(! comments.equals(prism.comments())) {
                    msg.printMessage(Diagnostic.Kind.ERROR,"comments wrong - expected " + comments + " was " + prism.comments());
                }
                if(! value.equals(prism.value())) {
                    msg.printMessage(Diagnostic.Kind.ERROR,"value wrong - expected " + value + " was " + prism.value());
                }
                
            }
        }
        return false;
    }
    
}
