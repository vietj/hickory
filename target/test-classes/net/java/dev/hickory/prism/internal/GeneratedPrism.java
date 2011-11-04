package net.java.dev.hickory.prism.internal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.TypeMirror;
import net.java.dev.hickory.prism.internal.*;
import java.util.HashMap;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
/** A Prism representing an {@code @javax.annotation.Generated} annotation. 
  */ 
class GeneratedPrism {
    /** store prism value of value */
    private List<String> _value;

    /** store prism value of date */
    private String _date;

    /** store prism value of comments */
    private String _comments;

    /**
      * An instance of the Values inner class whose
      * methods return the AnnotationValues used to build this prism. 
      * Primarily intended to support using Messager.
      */
    final Values values;
    /** Return a prism representing the {@code @javax.annotation.Generated} annotation on 'e'. 
      * similar to {@code e.getAnnotation(javax.annotation.Generated.class)} except that 
      * an instance of this class rather than an instance of {@code javax.annotation.Generated}
      * is returned.
      */
    static GeneratedPrism getInstanceOn(Element e) {
        AnnotationMirror m = getMirror("javax.annotation.Generated",e);
        if(m == null) return null;
        return getInstance(m);
   }

    /** Return a prism of the {@code @javax.annotation.Generated} annotation whose mirror is mirror. 
      */
    static GeneratedPrism getInstance(AnnotationMirror mirror) {
        return new GeneratedPrism(mirror);
    }

    private GeneratedPrism(AnnotationMirror mirror) {
        for(ExecutableElement key : mirror.getElementValues().keySet()) {
            memberValues.put(key.getSimpleName().toString(),mirror.getElementValues().get(key));
        }
        for(ExecutableElement member : ElementFilter.methodsIn(mirror.getAnnotationType().asElement().getEnclosedElements())) {
            defaults.put(member.getSimpleName().toString(),member.getDefaultValue());
        }
        _value = getArrayValues("value",String.class);
        _date = getValue("date",String.class);
        _comments = getValue("comments",String.class);
        this.values = new Values(memberValues);
        this.mirror = mirror;
        this.isValid = valid;
    }

    /** 
      * Returns a List<String> representing the value of the {@code value()} member of the Annotation.
      * @see javax.annotation.Generated#value()
      */ 
    List<String> value() { return _value; }

    /** 
      * Returns a String representing the value of the {@code java.lang.String date()} member of the Annotation.
      * @see javax.annotation.Generated#date()
      */ 
    String date() { return _date; }

    /** 
      * Returns a String representing the value of the {@code java.lang.String comments()} member of the Annotation.
      * @see javax.annotation.Generated#comments()
      */ 
    String comments() { return _comments; }

    /**
      * Determine whether the underlying AnnotationMirror has no errors.
      * True if the underlying AnnotationMirror has no errors.
      * When true is returned, none of the methods will return null.
      * When false is returned, a least one member will either return null, or another
      * prism that is not valid.
      */
    final boolean isValid;
    
    /**
      * The underlying AnnotationMirror of the annotation
      * represented by this Prism. 
      * Primarily intended to support using Messager.
      */
    final AnnotationMirror mirror;
    /**
      * A class whose members corespond to those of javax.annotation.Generated
      * but which each return the AnnotationValue corresponding to
      * that member in the model of the annotations. Returns null for
      * defaulted members. Used for Messager, so default values are not useful.
      */
    static class Values {
       private Map<String,AnnotationValue> values;
       private Values(Map<String,AnnotationValue> values) {
           this.values = values;
       }    
       /** Return the AnnotationValue corresponding to the value() 
         * member of the annotation, or null when the default value is implied.
         */
       AnnotationValue value(){ return values.get("value");}
       /** Return the AnnotationValue corresponding to the date() 
         * member of the annotation, or null when the default value is implied.
         */
       AnnotationValue date(){ return values.get("date");}
       /** Return the AnnotationValue corresponding to the comments() 
         * member of the annotation, or null when the default value is implied.
         */
       AnnotationValue comments(){ return values.get("comments");}
    }
    private Map<String,AnnotationValue> defaults = new HashMap<String,AnnotationValue>(10);
    private Map<String,AnnotationValue> memberValues = new HashMap<String,AnnotationValue>(10);
    private boolean valid = true;

    private <T> T getValue(String name, Class<T> clazz) {
        T result = GeneratedPrism.getValue(memberValues,defaults,name,clazz);
        if(result == null) valid = false;
        return result;
    } 

    private <T> List<T> getArrayValues(String name, final Class<T> clazz) {
        List<T> result = GeneratedPrism.getArrayValues(memberValues,defaults,name,clazz);
        if(result == null) valid = false;
        return result;
    }
    private static AnnotationMirror getMirror(String fqn, Element target) {
        for (AnnotationMirror m :target.getAnnotationMirrors()) {
            CharSequence mfqn = ((TypeElement)m.getAnnotationType().asElement()).getQualifiedName();
            if(fqn.contentEquals(mfqn)) return m;
        }
        return null;
    }
    private static <T> T getValue(Map<String,AnnotationValue> memberValues, Map<String,AnnotationValue> defaults, String name, Class<T> clazz) {
        AnnotationValue av = memberValues.get(name);
        if(av == null) av = defaults.get(name);
        if(av == null) {
            return null;
        }
        if(clazz.isInstance(av.getValue())) return clazz.cast(av.getValue());
        return null;
    }
    private static <T> List<T> getArrayValues(Map<String,AnnotationValue> memberValues, Map<String,AnnotationValue> defaults, String name, final Class<T> clazz) {
        AnnotationValue av = memberValues.get(name);
        if(av == null) av = defaults.get(name);
        if(av == null) {
            return null;
        }
        if(av.getValue() instanceof List) {
            List<T> result = new ArrayList<T>();
            for(AnnotationValue v : getValueAsList(av)) {
                if(clazz.isInstance(v.getValue())) {
                    result.add(clazz.cast(v.getValue()));
                } else{
                    return null;
                }
            }
            return result;
        } else {
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    private static List<AnnotationValue> getValueAsList(AnnotationValue av) {
        return (List<AnnotationValue>)av.getValue();
    }
}
