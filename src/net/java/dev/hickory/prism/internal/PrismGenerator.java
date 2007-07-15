/*
 * PrismGenerator.java
 *
 * Created on 27 June 2006, 22:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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

/**
 * An AnnotationProcessor for generating prisms. Do not use this class directly. 
 * @author Bruce
 */
@SupportedAnnotationTypes({"net.java.dev.hickory.prism.GeneratePrism","net.java.dev.hickory.prism.GeneratePrisms"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PrismGenerator extends AbstractProcessor {
    
    /** Creates a new instance of PrismGenerator */
    public PrismGenerator() {
    }
    
    private Map<String,TypeMirror> generated = new HashMap<String,TypeMirror>();

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        if(renv.processingOver()) {
            if(! generated.isEmpty())copyRequiredClassFiles();
            return true;
        }
        
        TypeElement a = processingEnv.getElementUtils().getTypeElement("net.java.dev.hickory.prism.GeneratePrism");
        TypeElement as = processingEnv.getElementUtils().getTypeElement("net.java.dev.hickory.prism.GeneratePrisms");
        
        for(Element e : renv.getElementsAnnotatedWith(a)) {
            GeneratePrismPrism ann = GeneratePrismPrism.getInstanceOn(e);
            if(ann.isValid())generateIfNew(ann,e,Collections.<DeclaredType,String>emptyMap());
        }
        for(Element e : renv.getElementsAnnotatedWith(as)) {
//                System.out.format("There is an %s on %s%n",a,e);
            GeneratePrismsPrism ann = GeneratePrismsPrism.getInstanceOn(e);
            if(ann.isValid()) {
                Map<DeclaredType,String> otherPrisms = new HashMap<DeclaredType,String>();
                for(GeneratePrismPrism inner : ann.value()) {
                    String name = getPrismName(inner);
                    // TODO to check that cast in next line is valid
                    otherPrisms.put((DeclaredType)inner.value(),getPrismName(inner));
                }
                System.out.format("Other prisms on %s = %s%n",e,otherPrisms);
                for(GeneratePrismPrism inner : ann.value()) {
                    generateIfNew(inner,e,otherPrisms);
                }
            }
        }
        return false;
    }
    
    private void copyRequiredClassFiles() {
        // need to copy over all the support classes
        Class[] supporting = new Class[] {
            // net.java.dev.hickory.prism. classes
            AbstractPrism.class
        };
        for(Class clazz : supporting) {
            // one of these is an inner class - so need to build the flattened name
            StringBuilder fname = new StringBuilder();
            fname.append(clazz.getSimpleName()).append(".class");
/*
            Class clazz2 = clazz;
            while( (clazz2 = clazz2.getEnclosingClass()) != null) {
                fname.insert(0,'$');
                fname.insert(0,clazz2.getSimpleName());
            }
 */
//            System.out.format("Copying %s for %s%n",fname,clazz);
            byte[] buffer = new byte[10240];
            try {
                File simplef = new File(fname.toString());
                JavaFileObject f = processingEnv.getFiler().
                        createClassFile(clazz.getName());
                System.out.format("Copying\t%s%nto\t%s%n",clazz.getResource(fname.toString()),f.toUri());
                // sometimes we have already copied it, and are running the copy so its locked, don't copy over itself
                if(! clazz.getResource(fname.toString()).toString().equals(f.toUri().toString())) {
                    InputStream is = clazz.getResourceAsStream(fname.toString());
                    OutputStream os = f.openOutputStream();
                    int cnt;
                    boolean done = false;
                    while( (cnt = is.read(buffer)) != -1) {
                        done = true;
                        os.write(buffer,0,cnt);
//                        System.out.format("\tCopied %d bytes for %s in package %s%n",cnt,simplef,clazz.getPackage().getName());
                    }
                    if(!done)throw new RuntimeException("no contents copied");
                    os.close();
                    is.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
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
                    e,ann.getMirror());
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                String.format("Generating prism named %s for %s%n",prismFqn,typeMirror));
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
            out.format(             "import net.java.dev.hickory.prism.*;%n");
            // TODO generate javadocs here
            String annName = ((TypeElement)typeMirror.asElement()).getQualifiedName().toString();
            out.format(             "/** A Prism representing an {@code @%s} annotation. %n",annName);
//            out.format(             "  * <p>The problem: When writing annotation processors the two ways to access%n");
//            out.format(             "  * the annotations in the code are both awkward. {@code Element.getAnnotation()} can throw%n");
//            out.format(             "  * Exceptions if the annotation being modelled is not semantically correct, and %n");
//            out.format(             "  * the member methods on the returned Annotation can also throw Exceptions %n");
//            out.format(             "  * if the annotation being modelled is not semantically correct. Moreover when calling%n");
//            out.format(             "  * a member with a {@code Class} return type, you need to catch an exception to extract the DeclaredType.%n");
//            out.format(             "  * <p>On the other hand, AnnotationMirror and AnnotationValue do a good job of%n");
//            out.format(             "  * modelling both correct and incorrect annotations, but provide no simple mechanism %n");
//            out.format(             "  * to determine whether it is correct or incorrect, and provide no convenient functionality%n");
//            out.format(             "  * to access the member values in a simple type safe way.%n");
//            out.format(             "  * <p>A Prism provides a solution to this problem by combining the advantages of the %n");
//            out.format(             "  * pure reflective model of AnnotationMirror and the runtime (real) model provided%n");
//            out.format(             "  * by {@code Element.getAnnotation()}.%n");
//            out.format(             "  * <P>%n");
//            out.format(             "  * A Mirror is where you look for a reflection whereas a Prism is %n");
//            out.format(             "  * where you look for a partial reflection. A {@code %s} provides a %n",name);
//            out.format(             "  * partially reflective and partially real view of an {@code @%s}%n",annName);
//            out.format(             "  * <p>It has the same member methods as {@code @%s} %n",annName);
//            out.format(             "  * except that the return types are mapped as follows...%n");
//            out.format(             "  * <ul><li>primitive members return their equivalent wrapper class in the prism.%n");
//            out.format(             "  * <li>Class members return a {@link javax.lang.model.type.DeclaredType DeclaredType} from the mirror API.%n");
//            out.format(             "  * <li>enum members return a String representing the enum constant (because the constant%n");
//            out.format(             "  * value in the mirror API mght not match those available in the runtime it cannot consistently return the appropriate enum).%n");
//            out.format(             "  * <li>String members return Strings.%n");
//            out.format(             "  * <li>Annotation members return a Prism of the annotation. Any such Prisms that%n");
//            out.format(             "  * this Prism depends on are supplied as inner classes of this class.%n");
//            out.format(             "  * <li>Array members return a {@code List<X>} where X is the appropriate prism mapping of the array %n");
//            out.format(             "  * component as above.%n");
//            out.format(             "  * </ul>%n");
//            out.format(             "  * If a prism is generated from the mirror of a semantically incorrect annotation%n");
//            out.format(             "  * then its {@code isValid()} method will return false, and the member with the %n");
//            out.format(             "  * erroneous value will return null. If {@code isValid()} returns {@code true} then%n");
//            out.format(             "  * no members will return null. AnnotatonProcessors using the prism should ignore%n");
//            out.format(             "  * any prism instance that is invalid. It can be assumed that the processing tool will indicate%n");
//            out.format(             "  * an error to the user in this case.%n");
//            out.format(             "  * %n");
//            out.format(             "  * <p>The {@code mirror} field provides access to the underlying%n");
//            out.format(             "  * AnnotationMirror to assist with using the Messager.%n");
//            out.format(             "  * <p>The {@code values} field contain a class with methodss which reflect the%n");
//            out.format(             "  * AnnotationValues mirrorring the annotation(but without defaults).%n");
//            out.format(             "  * This is useful when using Messager which can take an AnnotationValue as%n");
//            out.format(             "  * a hint as to the message's position in the source code%n");
//            out.format(             "  * %n");
//            out.format(             "  * %n");
//            out.format(             "  * %n");
            out.format(             "  */ %n");
                    
//            out.format(             "    @Prism(%s.class)%n",annName);
            out.format(             "%sclass %s extends %s {%n",access,name,AbstractPrism.class.getName()); 
            // SHOULD make public only if the anotation says so, package by default.
            generateClassBody("",out, name, typeMirror,access,otherPrisms);
            
            
            // recurse for inner prisms!!
            
            for(int n=0; n < inners.size();n++) {
                DeclaredType next = inners.get(n);
                String innerName = next.asElement().getSimpleName().toString();
                String forName = ((TypeElement)typeMirror.asElement()).getQualifiedName().toString();
//                out.format(         "        @Prism(%s.class)%n",forName);
                out.format(         "    %sstatic class %s extends %s {%n",access,innerName,AbstractPrism.class.getName());
                generateClassBody("    ",out, innerName,next,access,otherPrisms);
                out.format(         "    }%n");
            }
            
            out.format("}%n");
        
        } finally {
            out.close();
        }
        
    }
    
    List<DeclaredType> inners = new ArrayList<DeclaredType>();

    private void generateClassBody(final String indent, final PrintWriter out, final String name, final DeclaredType typeMirror, String access, Map<DeclaredType,String> otherPrisms) {
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
            out.format(         "%s        AnnotationMirror m = AbstractPrism.getMirror(\"%s\",e);%n",
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
        out.format(             "%s        super(mirror);%n",indent);
        for(PrismWriter w : writers) w.writeInitializer(indent,out);
        out.format(             "%s        this.values = new Values(super.values);%n",indent);
        out.format(             "%s        this.mirror = mirror;%n",indent);
        out.format(             "%s        this.isValid = valid;%n",indent);
//        for(PrismWriter w : writers) {
//            out.format(         "%s        _values.%s = super.values.get(\"%s\");%n",indent,w.name,w.name);
//        }
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
//        out.format(             "%s        return valid;%n",indent);
//        out.format(             "%s    }%n",indent);
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
    }

    
    private class PrismWriter {
//        PrintWriter out;
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
//            out.format(             "%s    %s AnnotationValue get%sAsValue() {%n",indent,access,name);
//            out.format(             "%s        return values.get(\"%s\");%n",indent,name);
//            out.format(             "%s    }%n",indent);
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
//                System.out.format("XXXXXX element is another annotation%n");
                String prismName = null;
                for(DeclaredType other : otherPrisms.keySet()) {
                    if(types.isSameType(other,annType)) {
                        prismName = otherPrisms.get(other);
                        break;
                    }
                }
                if(prismName != null) {
//                    System.out.format("element has sibling prism%n");
//                    String prismName = otherPrisms.get(annType);
                    result.setPrismType(prismName);
                    result.setM2pFormat(prismName + ".getInstance(%s)");
                } else {
//                    System.out.format("generate as inner%n");
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
}
