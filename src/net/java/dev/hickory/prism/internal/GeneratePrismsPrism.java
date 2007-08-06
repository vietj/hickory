/*
Copyright (c) 2006,2007, Bruce Chapman

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation and/or 
      other materials provided with the distribution.
    * Neither the name of the Hickory project nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.java.dev.hickory.prism.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.TypeMirror;

/** A Prism representing an {@code @nz.org.prism.GeneratePrisms} annotation. 
  * <p>The problem: When writing annotation processors the two ways to access
  * the annotations in the code are both awkward. {@code Element.getAnnotation()} can throw
  * Exceptions if the annotation being modelled is not semantically correct, and 
  * the member methods on the returned Annotation can also throw Exceptions 
  * if the annotation being modelled is not semantically correct. Moreover when calling
  * a member with a {@code Class} return type, you need to catch an exception to extract the DeclaredType.
  * <p>On the other hand, AnnotationMirror and AnnotationValue do a good job of
  * modelling both correct and incorrect annotations, but provide no simple mechanism 
  * to determine whether it is correct or incorrect, and provide no convenient functionality
  * to access the member values in a simple type safe way.
  * <p>A Prism provides a solution to this problem by combining the advantages of the 
  * pure reflective model of AnnotationMirror and the runtime (real) model provided
  * by {@code Element.getAnnotation()}.
  * <P>
  * A Mirror is where you look for a reflection whereas a Prism is 
  * where you look for a partial reflection. A {@code GeneratePrismsPrism} provides a 
  * partially reflective and partially real view of an {@code @nz.org.prism.GeneratePrisms}
  * <p>It has the same member methods as {@code @nz.org.prism.GeneratePrisms} 
  * except that the return types are mapped as follows...
  * <ul><li>primitive members return their equivalent wrapper class in the prism.
  * <li>Class members return a {@link javax.lang.model.type.DeclaredType DeclaredType} from the mirror API.
  * <li>enum members return a String representing the enum constant (because the constant
  * value in the mirror API mght not match those available in the runtime it cannot consistently return the appropriate enum).
  * <li>String members return Strings.
  * <li>Annotation members return a Prism of the annotation. Any such Prisms that
  * this Prism depends on are supplied as inner classes of this class.
  * <li>Array members return a {@code List<X>} where X is the appropriate prism mapping of the array 
  * component as above.
  * </ul>
  * If a prism is generated from the mirror of a semantically incorrect annotation
  * then its {@code isValid()} method will return false, and the member with the 
  * erroneous value wll return null. If {@code isValid()} returns {@code true} then
  * no members will return null. AnnotatonProcessors using the prism should ignore
  * any prism instance that is invalid. It can be assumed that the processing tool will indicate
  * an error to the user in this case.
  * 
  * <p>The {@code mirror} field provides access to the underlying
  * AnnotationMirror to assist with using the Messager.
  * <p>The {@code values} field contain a class with methodss which reflect the
  * AnnotationValues mirrorring the annotation(but without defaults).
  * This is useful when using Messager which can take an AnnotationValue as
  * a hint as to the message's position in the source code
  * 
  * 
  * 
  */ 
 class GeneratePrismsPrism extends net.java.dev.hickory.prism.internal.AbstractPrism {
    /** store prism value of value */
    private List<GeneratePrismPrism> value;

    /**
      * An instance of the Values inner class whose
      * methods return the AnnotationValues used to build this prism. 
      * Primarily intended to support using Messager.
      * @return the AnnotationValue structure for this Prism.
      */
     final Values values;
    /** Return a prism representing the {@code @nz.org.prism.GeneratePrisms} annotation on 'e'. 
      * similar to {@code e.getAnnotation(nz.org.prism.GeneratePrisms.class)} except that 
      * an instance of this class rather than an instance of {@code nz.org.prism.GeneratePrisms}
      * is returned.
      */
     static GeneratePrismsPrism getInstanceOn(Element e) {
        AnnotationMirror m = AbstractPrism.getMirror("net.java.dev.hickory.prism.GeneratePrisms",e);
        if(m == null) return null;
        return getInstance(m);
   }

    /** Return a prism of the {@code @nz.org.prism.GeneratePrisms} annotation whose mirror is mirror. 
      */
     static GeneratePrismsPrism getInstance(AnnotationMirror mirror) {
        return new GeneratePrismsPrism(mirror);
    }

    private GeneratePrismsPrism(AnnotationMirror mirror) {
        super(mirror);
        List<AnnotationMirror> valueMirrors = getArrayValues("value",AnnotationMirror.class);
        /* List<GeneratePrismPrism> */ value = new ArrayList<GeneratePrismPrism>(valueMirrors.size());
        for(AnnotationMirror valueMirror : valueMirrors) {
            value.add(GeneratePrismPrism.getInstance(valueMirror));
        }
        this.values = new Values(super.values);
        this.mirror = mirror;
    }

    /** 
      * Returns a List<GeneratePrism> representing the value of the {@code value()} member of the Annotation.
      */ 
     List<GeneratePrismPrism> value() { return value; }

    /**
      * Determine whether the underlying AnnotationMirror has no errors.
      * @return true if the underlying AnnotationMirror has no errors.
      * When true is returned, none of the other methods will return null.
      * When false is returned, a least one member will either return null, or another
      * prism that is not valid.
      */
     boolean isValid() {
        return valid;
    }
    
    /**
      * The underlying AnnotationMirror of the annotation
      * represented by this Prism. 
      * Primarily intended to support using Messager.
      */
     final AnnotationMirror mirror;
    /**
      * A class whose members correspond to the annotations
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
          * member of the annotation, or null when the default value is implied. */
         AnnotationValue value(){ return values.get("value");}
    }
//     
//     static class Author extends nz.org.prism.AbstractPrism {
//        /** store prism value of first */
//        private String first;
//
//        /** store prism value of second */
//        private String second;
//
//        /**
//          * An instance of the Values inner class whose
//          * methods return the AnnotationValues used to build this prism. 
//          * Primarily intended to support using Messager.
//          * @return the AnnotationValue structure for this Prism.
//          */
//         final Values values;
//        /** Return a prism of the {@code @Author} annotation whose mirror is mirror. 
//          */
//        private static Author getInstance(AnnotationMirror mirror) {
//            return new Author(mirror);
//        }
//
//        private Author(AnnotationMirror mirror) {
//            super(mirror);
//            first = getValue("first",String.class);
//            second = getValue("second",String.class);
//            this.values = new Values(super.values);
//            this.mirror = mirror;
//        }
//
//        /** 
//          * Returns a String representing the value of the {@code java.lang.String first()} member of the Annotation.
//          */ 
//         String first() { return first; }
//
//        /** 
//          * Returns a String representing the value of the {@code java.lang.String second()} member of the Annotation.
//          */ 
//         String second() { return second; }
//
//        /**
//          * Determine whether the underlying AnnotationMirror has no errors.
//          * @return true if the underlying AnnotationMirror has no errors.
//          * When true is returned, none of the other methods will return null.
//          * When false is returned, a least one member will either return null, or another
//          * prism that is not valid.
//          */
//         boolean isValid() {
//            return valid;
//        }
//        
//        /**
//          * The underlying AnnotationMirror of the annotation
//          * represented by this Prism. 
//          * Primarily intended to support using Messager.
//          */
//         final AnnotationMirror mirror;
//        /**
//          * A class whose members correspond to the annotations
//          * but which each return the AnnotationValue corresponding to
//          * that member in the model of the annotations. Returns null for
//          * defaulted members. Used for Messager, so default values are not useful.
//          */
//         static class Values {
//           private Map<String,AnnotationValue> values;
//           private Values(Map<String,AnnotationValue> values) {
//               this.values = values;
//           }    
//            /** Return the AnnotationValue corresponding to the first() 
//              * member of the annotation, or null when the default value is implied. */
//             AnnotationValue first(){ return values.get("first");}
//            /** Return the AnnotationValue corresponding to the second() 
//              * member of the annotation, or null when the default value is implied. */
//             AnnotationValue second(){ return values.get("second");}
//        }
//    }
//     static class GeneratePrism extends nz.org.prism.AbstractPrism {
//        /** store prism value of value */
//        private TypeMirror value;
//
//        /** store prism value of name */
//        private String name;
//
//        /** store prism value of publicAccess */
//        private Boolean publicAccess;
//
//        /**
//          * An instance of the Values inner class whose
//          * methods return the AnnotationValues used to build this prism. 
//          * Primarily intended to support using Messager.
//          * @return the AnnotationValue structure for this Prism.
//          */
//         final Values values;
//        /** Return a prism of the {@code @nz.org.prism.GeneratePrism} annotation whose mirror is mirror. 
//          */
//        private static GeneratePrism getInstance(AnnotationMirror mirror) {
//            return new GeneratePrism(mirror);
//        }
//
//        private GeneratePrism(AnnotationMirror mirror) {
//            super(mirror);
//            value = getValue("value",TypeMirror.class);
//            name = getValue("name",String.class);
//            publicAccess = getValue("publicAccess",Boolean.class);
//            this.values = new Values(super.values);
//            this.mirror = mirror;
//        }
//
//        /** 
//          * Returns a TypeMirror representing the value of the {@code java.lang.Class<? extends java.lang.annotation.Annotation> value()} member of the Annotation.
//          */ 
//         TypeMirror value() { return value; }
//
//        /** 
//          * Returns a String representing the value of the {@code java.lang.String name()} member of the Annotation.
//          */ 
//         String name() { return name; }
//
//        /** 
//          * Returns a Boolean representing the value of the {@code boolean publicAccess()} member of the Annotation.
//          */ 
//         Boolean publicAccess() { return publicAccess; }
//
//        /**
//          * Determine whether the underlying AnnotationMirror has no errors.
//          * @return true if the underlying AnnotationMirror has no errors.
//          * When true is returned, none of the other methods will return null.
//          * When false is returned, a least one member will either return null, or another
//          * prism that is not valid.
//          */
//         boolean isValid() {
//            return valid;
//        }
//        
//        /**
//          * The underlying AnnotationMirror of the annotation
//          * represented by this Prism. 
//          * Primarily intended to support using Messager.
//          */
//         final AnnotationMirror mirror;
//        /**
//          * A class whose members correspond to the annotations
//          * but which each return the AnnotationValue corresponding to
//          * that member in the model of the annotations. Returns null for
//          * defaulted members. Used for Messager, so default values are not useful.
//          */
//         static class Values {
//           private Map<String,AnnotationValue> values;
//           private Values(Map<String,AnnotationValue> values) {
//               this.values = values;
//           }    
//            /** Return the AnnotationValue corresponding to the value() 
//              * member of the annotation, or null when the default value is implied. */
//             AnnotationValue value(){ return values.get("value");}
//            /** Return the AnnotationValue corresponding to the name() 
//              * member of the annotation, or null when the default value is implied. */
//             AnnotationValue name(){ return values.get("name");}
//            /** Return the AnnotationValue corresponding to the publicAccess() 
//              * member of the annotation, or null when the default value is implied. */
//             AnnotationValue publicAccess(){ return values.get("publicAccess");}
//        }
//    }
}
