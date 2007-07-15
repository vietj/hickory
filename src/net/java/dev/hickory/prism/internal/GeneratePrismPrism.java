/*
  GeneratePrismPrism.java
 
 Created on 30 June 2006, 14:55
 

 */

package net.java.dev.hickory.prism.internal;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * This is a Prism for GeneratePrism. It was hand coded because I needed to use it
 * before I had the processsor written to generate it automatically. It also
 * is a simple example of a Prism. See the source for PrismGenerator to see how a
 * prism is used.
 @author Bruce
 */
class GeneratePrismPrism extends AbstractPrism {
    
    private TypeMirror value;
    private String name;
    private Boolean publicAccess;
    private Values _values = new Values();

    /** Creates a new instance of GeneratePrismPrism */
    private GeneratePrismPrism(AnnotationMirror mirror) {
        super(mirror);
        value = getValue("value",TypeMirror.class);
        name = getValue("name",String.class);
        publicAccess = getValue("publicAccess",Boolean.class);
        _values.name = super.values.get("name");
        _values.value = super.values.get("value");
    }
    
    public static GeneratePrismPrism getInstance(AnnotationMirror mirror) {
        return new GeneratePrismPrism(mirror);
    }
    
    public static GeneratePrismPrism getInstanceOn(Element e) {
        AnnotationMirror m = AbstractPrism.getMirror("net.java.dev.hickory.prism.GeneratePrism",e);
        if(m == null) return null;
        return getInstance(m);
    }

    TypeMirror value() { return value; }
    String name() { return name; }
    Boolean publicAccess() { return publicAccess; }

    
    public class Values {
        AnnotationValue value;
        AnnotationValue name;
        AnnotationValue publicAccess;
    }
    
    public Values getValues() {
        return _values;
    }
    
    public boolean isValid(){  return super.valid; }
    
    public AnnotationMirror getMirror() { return super.mirror; }
}
