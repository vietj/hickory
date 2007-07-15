/*
 * AbstractPrism.java
 *
 * Created on 27 June 2006, 21:55
 *
 */

package net.java.dev.hickory.prism.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * Provides support for generated prisms. This class should not be accessed directly.
 * @author Bruce
 */
abstract public class AbstractPrism {
    
    /** could be static in concrete subclass, actually could be in source code in generated subclass */
    private Map<String,AnnotationValue> defaults = new HashMap<String,AnnotationValue>(10);
    
    protected Map<String,AnnotationValue> values = new HashMap<String,AnnotationValue>(10);
    protected boolean valid = true;
    protected AnnotationMirror mirror;
    
    
    
    /** Creates a new instance of AbstractPrism */
    protected AbstractPrism(AnnotationMirror mirror) {
        this.mirror = mirror;
        for(ExecutableElement key : mirror.getElementValues().keySet()) {
            values.put(key.getSimpleName().toString(),mirror.getElementValues().get(key));
        }
        for(ExecutableElement member : ElementFilter.methodsIn(mirror.getAnnotationType().asElement().getEnclosedElements())) {
            defaults.put(member.getSimpleName().toString(),member.getDefaultValue());
        }
    }
    
    protected static AnnotationMirror getMirror(String fqn, Element target) {
        for (AnnotationMirror m :target.getAnnotationMirrors()) {
            CharSequence mfqn = ((TypeElement)m.getAnnotationType().asElement()).getQualifiedName();
            if(fqn.contentEquals(mfqn)) return m;
        }
        return null;
    }

    protected <T> T getValue(String name, Class<T> clazz) {
        AnnotationValue av = values.get(name);
        if(av == null) av = defaults.get(name);
        if(av == null) {
            valid = false;
            return null;
        }
        if(clazz.isInstance(av.getValue())) return clazz.cast(av.getValue());
        valid = false;
        return null;
    } 
    
    protected <T> List<T> getArrayValues(String name, final Class<T> clazz) {
        AnnotationValue av = values.get(name);
        if(av == null) av = defaults.get(name);
        if(av == null) {
            valid = false;
            return null;
        }
        if(av.getValue() instanceof List) {
            List<T> result = new ArrayList<T>();
            for(AnnotationValue v : getValueAsList(av)) {
                if(clazz.isInstance(v.getValue())) {
                    result.add(clazz.cast(v.getValue()));
                } else{
                    valid = false;
                    return null;
                }
            }
            return result;
        } else {
            valid = false;
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<AnnotationValue> getValueAsList(AnnotationValue av) {
        return (List<AnnotationValue>)av.getValue();
    }
 }
