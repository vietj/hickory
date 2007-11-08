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

/*
 * PrismGenerator.java
 *
 * Created on 27 June 2006, 22:07
 */

package net.java.dev.hickory.prism.internal;

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
import net.java.dev.hickory.prism.*;

/**
 * An AnnotationProcessor for generating prisms. Do not use this class directly. 
 * @author Bruce
 */
//@GeneratePrisms({ 
//    @GeneratePrism(GeneratePrisms.class),
//    @GeneratePrism(GeneratePrism.class)
//})
@SupportedAnnotationTypes({"net.java.dev.hickory.prism.GeneratePrism","net.java.dev.hickory.prism.GeneratePrisms"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrismGenerator extends AbstractProcessor {
    
    /** Creates a new instance of PrismGenerator */
    public PrismGenerator() {
    }
    
    private Map<String,TypeMirror> generated = new HashMap<String,TypeMirror>();

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        if(renv.processingOver()) {
            return true;
        }
        
        TypeElement a = processingEnv.getElementUtils().getTypeElement("net.java.dev.hickory.prism.GeneratePrism");
        TypeElement as = processingEnv.getElementUtils().getTypeElement("net.java.dev.hickory.prism.GeneratePrisms");
        
        for(Element e : renv.getElementsAnnotatedWith(a)) {
            GeneratePrismPrism ann = GeneratePrismPrism.getInstanceOn(e);
            if(ann.isValid)generateIfNew(ann,e,Collections.<DeclaredType,String>emptyMap());
        }
        for(Element e : renv.getElementsAnnotatedWith(as)) {
            GeneratePrismsPrism ann = GeneratePrismsPrism.getInstanceOn(e);
            if(ann.isValid) {
                Map<DeclaredType,String> otherPrisms = new HashMap<DeclaredType,String>();
                for(GeneratePrismPrism inner : ann.value()) {
                    String name = getPrismName(inner);
                    // TODO to check that cast in next line is valid
                    otherPrisms.put((DeclaredType)inner.value(),getPrismName(inner));
                }
                for(GeneratePrismPrism inner : ann.value()) {
                    generateIfNew(inner,e,otherPrisms);
                }
            }
        }
        return false;
    }
    
    private String getPrismName(GeneratePrismPrism ann) {
        String name = ann.name();
        if(name.equals("")) name = ((DeclaredType)ann.value()).asElement().getSimpleName() + "Prism";
        return name;
    }

    private void generateIfNew(GeneratePrismPrism ann, Element e,Map<DeclaredType,String> otherPrisms) {
        String name = getPrismName(ann);
        String packageName = getPackageName(e);
        // workaround for bug that has been fixed in a later build
        if("unnamed package".equals(packageName)) packageName = "";
        String prismFqn = packageName.equals("") ? name : packageName + "." + name;
        if(generated.containsKey(prismFqn)) {
            // if same value dont need to generate, if different then error
            if(generated.get(prismFqn).equals(ann.value())) return;
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    String.format("%s has already been generated for %s",prismFqn,generated.get(prismFqn)),
                    e,ann.mirror);
            return;
        }
        generatePrism(name,packageName,(DeclaredType)ann.value(),ann.publicAccess() ? "public " : "",otherPrisms);
        generated.put(prismFqn,ann.value());
    }

    private String getPackageName(Element e) {
        while(e.getKind() != ElementKind.PACKAGE) e = e.getEnclosingElement();
        return ((PackageElement)e).getQualifiedName().toString();
    }

    private void generatePrism(String name, String packageName, DeclaredType typeMirror, String access, Map<DeclaredType,String> otherPrisms) {
        inners.clear();
        String prismFqn = packageName.equals("") ? name : packageName + "." + name;
        PrintWriter out = null;
        try {
            // out = new PrintWriter(processingEnv.getFiler().createSourceFile(prismFqn));
            out = new PrintWriter(processingEnv.getFiler().createSourceFile(prismFqn).openWriter());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            
            if(!packageName.equals("")) {
                out.format(         "package %s;%n",packageName);
            }
            out.format(             "import java.util.ArrayList;%n");
            out.format(             "import java.util.List;%n");
            out.format(             "import java.util.Map;%n");
            out.format(             "import javax.lang.model.element.AnnotationMirror;%n");
            out.format(             "import javax.lang.model.element.Element;%n");
            out.format(             "import javax.lang.model.element.VariableElement;%n");
            out.format(             "import javax.lang.model.element.AnnotationValue;%n");
            out.format(             "import javax.lang.model.type.TypeMirror;%n");
            out.format(             "import net.java.dev.hickory.prism.internal.*;%n");
            out.format(             "import java.util.HashMap;%n");
            out.format(             "import javax.lang.model.element.ExecutableElement;%n");
            out.format(             "import javax.lang.model.element.TypeElement;%n");
            out.format(             "import javax.lang.model.util.ElementFilter;%n");
            // TODO generate javadocs here
            String annName = ((TypeElement)typeMirror.asElement()).getQualifiedName().toString();
            out.format(             "/** A Prism representing an {@code @%s} annotation. %n",annName);
            out.format(             "  */ %n");
            out.format(             "%sclass %s {%n",access,name); 
            
            // SHOULD make public only if the anotation says so, package by default.
            generateClassBody("",out, name, name, typeMirror,access,otherPrisms);
            
            
            // recurse for inner prisms!!
            for(int n=0; n < inners.size();n++) {
                DeclaredType next = inners.get(n);
                String innerName = next.asElement().getSimpleName().toString();
                String forName = ((TypeElement)typeMirror.asElement()).getQualifiedName().toString();
                out.format(         "    %sstatic class %s {%n",access,innerName);
                generateClassBody("    ",out, name, innerName,next,access,otherPrisms);
                out.format(         "    }%n");
            }
            generateStaticMembers(out);
            out.format("}%n");
        } finally {
            out.close();
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                String.format("Generated prism %s for @%s",prismFqn,typeMirror));
    }
    
    List<DeclaredType> inners = new ArrayList<DeclaredType>();

    private void generateClassBody(final String indent, final PrintWriter out, final String outerName, final String name, final DeclaredType typeMirror, String access, Map<DeclaredType,String> otherPrisms) {
        List<PrismWriter> writers = new ArrayList<PrismWriter>();
        for(ExecutableElement m : ElementFilter.methodsIn(typeMirror.asElement().getEnclosedElements())) {
            writers.add(getWriter(m,access,otherPrisms));
        }
        for(PrismWriter w : writers) w.writeField(indent,out);
        String annName = ((TypeElement)typeMirror.asElement()).getQualifiedName().toString();
        out.format(             "%s    /**%n",indent);
        out.format(             "%s      * An instance of the Values inner class whose%n",indent);
        out.format(             "%s      * methods return the AnnotationValues used to build this prism. %n",indent);
        out.format(             "%s      * Primarily intended to support using Messager.%n",indent);
        out.format(             "%s      */%n",indent);
        
        out.format(             "%s    %sfinal Values values;\n",indent,access);
        boolean inner = ! indent.equals("");
        // write factory methods
        if(!inner) {
            out.format(         "%s    /** Return a prism representing the {@code @%s} annotation on 'e'. %n",indent,annName);
            out.format(         "%s      * similar to {@code e.getAnnotation(%s.class)} except that %n",indent,annName);
            out.format(         "%s      * an instance of this class rather than an instance of {@code %s}%n",indent,annName);
            out.format(         "%s      * is returned.%n",indent);
            out.format(         "%s      */%n",indent);
            out.format(         "%s    %sstatic %s getInstanceOn(Element e) {%n",indent,access,name);
            out.format(         "%s        AnnotationMirror m = getMirror(\"%s\",e);%n",
                        indent,((TypeElement)(typeMirror.asElement())).getQualifiedName());   
            out.format(         "%s        if(m == null) return null;%n",indent);
            out.format(         "%s        return getInstance(m);%n",indent);
            out.format(         "%s   }%n%n",indent);
        }
        out.format(             "%s    /** Return a prism of the {@code @%s} annotation whose mirror is mirror. %n",indent,annName);
        out.format(             "%s      */%n",indent);
        out.format(             "%s    %sstatic %s getInstance(AnnotationMirror mirror) {%n",indent,inner ? "private " : access,name);
        out.format(             "%s        return new %s(mirror);%n",indent,name);
        out.format(             "%s    }%n%n",indent);
        // write constructor
        out.format(             "%s    private %s(AnnotationMirror mirror) {%n",indent,name);
        out.print(
                                "        for(ExecutableElement key : mirror.getElementValues().keySet()) {\n" +
                                "            memberValues.put(key.getSimpleName().toString(),mirror.getElementValues().get(key));\n" +
                                "        }\n" +
                                "        for(ExecutableElement member : ElementFilter.methodsIn(mirror.getAnnotationType().asElement().getEnclosedElements())) {\n" +
                                "            defaults.put(member.getSimpleName().toString(),member.getDefaultValue());\n" +
                                "        }\n" 
                );
        for(PrismWriter w : writers) w.writeInitializer(indent,out);
        out.format(             "%s        this.values = new Values(memberValues);%n",indent);
        out.format(             "%s        this.mirror = mirror;%n",indent);
        out.format(             "%s        this.isValid = valid;%n",indent);
        out.format(             "%s    }%n%n",indent);
        
        // write methods
        for(PrismWriter w : writers) w.writeMethod(indent,out);
        
        // write isValid and getMirror methods
        out.format(             "%s    /**%n",indent);
        out.format(             "%s      * Determine whether the underlying AnnotationMirror has no errors.%n",indent);
        out.format(             "%s      * True if the underlying AnnotationMirror has no errors.%n",indent);
        out.format(             "%s      * When true is returned, none of the methods will return null.%n",indent);
        out.format(             "%s      * When false is returned, a least one member will either return null, or another%n",indent);
        out.format(             "%s      * prism that is not valid.%n",indent);
        out.format(             "%s      */%n",indent);
        out.format(             "%s    %sfinal boolean isValid;%n",indent,access);
        out.format(             "%s    %n",indent);
        out.format(             "%s    /**%n",indent);
        out.format(             "%s      * The underlying AnnotationMirror of the annotation%n",indent);
        out.format(             "%s      * represented by this Prism. %n",indent);
        out.format(             "%s      * Primarily intended to support using Messager.%n",indent);
        out.format(             "%s      */%n",indent);
        out.format(             "%s    %sfinal AnnotationMirror mirror;%n",indent,access);
        
        // write Value class
        out.format(             "%s    /**%n",indent);
        out.format(             "%s      * A class whose members corespond to those of %s%n",indent,annName);
        out.format(             "%s      * but which each return the AnnotationValue corresponding to%n",indent);
        out.format(             "%s      * that member in the model of the annotations. Returns null for%n",indent);
        out.format(             "%s      * defaulted members. Used for Messager, so default values are not useful.%n",indent);
        out.format(             "%s      */%n",indent);
        out.format(             "%s    %sstatic class Values {%n",indent,access);
        out.format(             "%s       private Map<String,AnnotationValue> values;%n",indent);
        out.format(             "%s       private Values(Map<String,AnnotationValue> values) {%n",indent);
        out.format(             "%s           this.values = values;%n",indent);
        out.format(             "%s       }    %n",indent);
        for(PrismWriter w : writers) {
            out.format(         "%s       /** Return the AnnotationValue corresponding to the %s() %n",indent,w.name);
            out.format(         "%s         * member of the annotation, or null when the default value is implied.%n",indent);
            out.format(         "%s         */%n",indent);
            out.format(         "%s       %sAnnotationValue %s(){ return values.get(\"%s\");}%n",indent,access,w.name,w.name);
        }
        out.format(             "%s    }%n",indent);
        generateFixedClassContent(indent,out, outerName);
    }

    
    private class PrismWriter {
        String name;
        String mirrorType;
        String prismType;
        boolean arrayed;
        ExecutableElement m;
        String access; // "public" or ""
        PrismWriter(ExecutableElement m, boolean arrayed,String access) {
            this.m=m;
            this.arrayed = arrayed;
            this.access = access;
            this.name= m.getSimpleName().toString();
        }

        public void setPrismType(String prismType) {
            this.prismType = prismType;
        }

        public void setMirrorType(String mirrorType) {
            this.mirrorType = mirrorType;
        }

         void writeField(String indent, PrintWriter out){
            out.format(         "%s    /** store prism value of %s */%n",indent,name);
            if(arrayed) {
                out.format(     "%s    private List<%s> _%s;%n%n",indent,prismType,name);
            } else {
                out.format(     "%s    private %s _%s;%n%n",indent,prismType,name);
            }
        }
        
        /* return source code that converts an expr of mirrorType to prismType. */
        String mirror2prism(String expr) {
            return String.format(m2pFormat,expr);
        }
        
        String m2pFormat="undefinedConverter(%s)";

        public void setM2pFormat(String m2pFormat) {
            this.m2pFormat = m2pFormat;
        }


        void writeInitializer(String indent, PrintWriter out){
            if(arrayed) {
                if(mirrorType.equals(prismType)) {
                    out.format( "%s        _%s = getArrayValues(\"%s\",%s.class);%n",indent,name,name,prismType);
                } else {
                    out.format( "%s        List<%s> %sMirrors = getArrayValues(\"%s\",%s.class);%n",indent, mirrorType,name,name,mirrorType);
                    out.format( "%s         _%s = new ArrayList<%s>(%sMirrors.size());%n",indent,name,prismType,name);
                    out.format( "%s        for(%s %sMirror : %sMirrors) {%n",indent,mirrorType,name,name);
                    out.format( "%s            _%s.add(%s);%n",indent,name,mirror2prism(name + "Mirror"));
                    out.format( "%s        }%n",indent);
                }
            } else {
                if(mirrorType.equals(prismType)) {
                    out.format( "%s        _%s = getValue(\"%s\",%s.class);%n",indent,name,name,prismType);
                } else {
                    out.format( "%s        %s %sMirror = getValue(\"%s\",%s.class);%n",indent, mirrorType,name,name,mirrorType);
                    out.format( "%s        valid = valid && %sMirror != null;%n",indent,name);
                    out.format( "%s        _%s = %sMirror == null ? null : %s;%n",indent,name,name, mirror2prism(name + "Mirror"));
                }
            }
        }
        void writeMethod(String indent, PrintWriter out) {
            if(arrayed) {
                out.format(         "%s    /** %n",indent);
                out.format(         "%s      * Returns a List<%s> representing the value of the {@code %s} member of the Annotation.%n",indent,prismType,m);
                out.format(         "%s      * @see %s#%s()%n",indent,((TypeElement)m.getEnclosingElement()).getQualifiedName(),name);
                out.format(         "%s      */ %n",indent);
                out.format(         "%s    %sList<%s> %s() { return _%s; }%n%n",indent,access,prismType,name,name);
            } else {
                out.format(         "%s    /** %n",indent);
                out.format(         "%s      * Returns a %s representing the value of the {@code %s %s} member of the Annotation.%n",indent,prismType,m.getReturnType(),m);
                out.format(         "%s      * @see %s#%s()%n",indent,((TypeElement)m.getEnclosingElement()).getQualifiedName(),name);
                out.format(         "%s      */ %n",indent);
                out.format(         "%s    %s%s %s() { return _%s; }%n%n",indent,access,prismType,name,name);
            }
        }
    }
    
    private PrismWriter getWriter(ExecutableElement m,String access, Map<DeclaredType,String> otherPrisms) {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();
        WildcardType q = types.getWildcardType(null,null);
        TypeMirror enumType = types.getDeclaredType(elements.getTypeElement("java.lang.Enum"),q);
        TypeMirror typem = m.getReturnType();
        PrismWriter result = null;
        if(typem.getKind() == TypeKind.ARRAY) {
            typem = ((ArrayType)typem).getComponentType();
            result = new PrismWriter(m,true,access);
        } else {
            result = new PrismWriter(m,false,access);
        }
        if(typem.getKind().isPrimitive()) {
            String typeName = types.boxedClass((PrimitiveType)typem).getSimpleName().toString();
            result.setMirrorType(typeName);
            result.setPrismType(typeName);
        } else if(typem.getKind() == TypeKind.DECLARED) {
            DeclaredType type  = (DeclaredType)typem;
            // String, enum, annotation, or Class<?>
            if(types.isSameType(type,elements.getTypeElement("java.lang.String").asType())) {
                // String
                result.setMirrorType("String");
                result.setPrismType("String");
            } else if (type.asElement().equals(elements.getTypeElement("java.lang.Class"))) {
                // class<? ...>
                result.setMirrorType("TypeMirror");
                result.setPrismType("TypeMirror");
            } else if (types.isSubtype(type,enumType)) {
                //Enum
                result.setMirrorType("VariableElement");
                result.setPrismType("String");
                result.setM2pFormat("%s.getSimpleName().toString()");
            } else if (types.isSubtype(type,elements.getTypeElement("java.lang.annotation.Annotation").asType())) {
                result.setMirrorType("AnnotationMirror");
                DeclaredType annType = (DeclaredType)type;
                String prismName = null;
                for(DeclaredType other : otherPrisms.keySet()) {
                    if(types.isSameType(other,annType)) {
                        prismName = otherPrisms.get(other);
                        break;
                    }
                }
                if(prismName != null) {
                    result.setPrismType(prismName);
                    result.setM2pFormat(prismName + ".getInstance(%s)");
                } else {
                    // generate its prism as inner class
                    String prismType = annType.asElement().getSimpleName().toString();
                    result.setPrismType(prismType);
                    result.setM2pFormat(prismType + ".getInstance(%s)");
                    // force generation of inner prism class for annotation
                    if(! inners.contains(type) ) inners.add(type);
                }
            } else {
                System.out.format("Unprocessed type %s",type);
            }
        }
        return result;        
    }
    private void generateStaticMembers(PrintWriter out) {
        out.print(
            "    private static AnnotationMirror getMirror(String fqn, Element target) {\n" +
            "        for (AnnotationMirror m :target.getAnnotationMirrors()) {\n" +
            "            CharSequence mfqn = ((TypeElement)m.getAnnotationType().asElement()).getQualifiedName();\n" +
            "            if(fqn.contentEquals(mfqn)) return m;\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "    private static <T> T getValue(Map<String,AnnotationValue> memberValues, Map<String,AnnotationValue> defaults, String name, Class<T> clazz) {\n" +
            "        AnnotationValue av = memberValues.get(name);\n" +
            "        if(av == null) av = defaults.get(name);\n" +
            "        if(av == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        if(clazz.isInstance(av.getValue())) return clazz.cast(av.getValue());\n" +
            "        return null;\n" +
            "    }\n" + 
            "    private static <T> List<T> getArrayValues(Map<String,AnnotationValue> memberValues, Map<String,AnnotationValue> defaults, String name, final Class<T> clazz) {\n" +
            "        AnnotationValue av = memberValues.get(name);\n" +
            "        if(av == null) av = defaults.get(name);\n" +
            "        if(av == null) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        if(av.getValue() instanceof List) {\n" +
            "            List<T> result = new ArrayList<T>();\n" +
            "            for(AnnotationValue v : getValueAsList(av)) {\n" +
            "                if(clazz.isInstance(v.getValue())) {\n" +
            "                    result.add(clazz.cast(v.getValue()));\n" +
            "                } else{\n" +
            "                    return null;\n" +
            "                }\n" +
            "            }\n" +
            "            return result;\n" +
            "        } else {\n" +
            "            return null;\n" +
            "        }\n" +
            "    }\n" +
            "    @SuppressWarnings(\"unchecked\")\n" +
            "    private static List<AnnotationValue> getValueAsList(AnnotationValue av) {\n" +
            "        return (List<AnnotationValue>)av.getValue();\n" +
            "    }\n"
        );
    }

    private void generateFixedClassContent(String indent, PrintWriter out, String outerName) {
        out.format("%s    private Map<String,AnnotationValue> defaults = new HashMap<String,AnnotationValue>(10);%n",indent);
        out.format("%s    private Map<String,AnnotationValue> memberValues = new HashMap<String,AnnotationValue>(10);%n",indent);
        out.format("%s    private boolean valid = true;%n",indent);
        out.format("%n");
        out.format("%s    private <T> T getValue(String name, Class<T> clazz) {%n",indent);
        out.format("%s        T result = %s.getValue(memberValues,defaults,name,clazz);%n",indent,outerName);
        out.format("%s        if(result == null) valid = false;%n",indent);
        out.format("%s        return result;%n",indent);
        out.format("%s    } %n",indent);
        out.format("%n");
        out.format("%s    private <T> List<T> getArrayValues(String name, final Class<T> clazz) {%n",indent);
        out.format("%s        List<T> result = %s.getArrayValues(memberValues,defaults,name,clazz);%n",indent,outerName);
        out.format("%s        if(result == null) valid = false;%n",indent);
        out.format("%s        return result;%n",indent);
        out.format("%s    }%n",indent);
    }
}
