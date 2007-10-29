/*
 * MethodVerifier.java
 *
 * Created on 9 October 2007, 21:32
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.testing;

/*
 MethodVerifier.java
 
 *
 To change this template, choose Tools | Template Manager
 and open the template in the editor.
 */


import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Provides a convenient means to test for the presence of known method signatures in a Class<?>
 * for example
 * <pre>
 * {@literal
 *   public void testLocalGeneric() {
 *        class Example<T> {
 *            public <U> Class<? extends U> asSubclass(Class<U> clazz) {
 *                throw new UnsupportedOperationException();
 *            }
 *        }
 *        MethodVerifier clazz = new MethodVerifier(Class.class);
 *        assertTrue(clazz.hasMethod(Example.class));
 *        assertTrue(clazz.hasMethod("public <U> Class<? extends U> asSubclass(Class<U> clazz)","T"));
 *    }
 * } </pre> 
 * Normally the Class used to construct a MethodVerifier would be obtained by Compilation.getOutputClass.
 * The literal Class {@code Class.class} is used for simplicity in the above example. 
 * 
 * @author bchapman
 */
public class MethodVerifier {
    
    Class<?> target;

    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
    
    /**
     * 
     * Creates a new instance of MethodVerifier in order to test the method signatures of target 
     * 
     * @param target The Class whose method signatures need to be tested.
     */
    public MethodVerifier(Class<?> target) {
        this.target = target;
    }
    
    /** Checks to see if the target class has the specified signature.
     * @param signature The complete method signature up to but not including ; or {
     * @param classTypeVarNames If the signature refers to the target's type variables
     * these are declared here as a comma separated list, If none, use null.
     * @param imports a varargs list of imports to be used to resolve the types in the signature. 
     * For example {@code "java.util.*"}.
     */
    public boolean hasMethod(String signature, String classTypeVarNames, String... imports) {
        // generate MethodSignatureWrapper and compile it, and load via classloader
        final StringBuilder source = new StringBuilder();
        for(String imported : imports) {
            source.append("import ").append(imported).append(";\n");
        }
        boolean methodIsAbstract = signature.startsWith("abstract ") || signature.contains(" abstract ");
        if(methodIsAbstract) source.append("abstract ");
        source.append("class __Wrapper__");
        if(classTypeVarNames != null) source.append("<").append(classTypeVarNames).append(">");
        source.append(" {\n");
        source.append("    ").append(signature);
        if(methodIsAbstract) {
            source.append(";\n");
        } else {
            source.append(" {\n");
            source.append("        throw new UnsupportedOperationException();\n    }\n");
        }
        source.append("}\n");
//        System.out.format("%s%n",source);
            
        //  compile source and load it
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        MemFileManager rfm = new MemFileManager(compiler.getStandardFileManager(diagnostics,null,null));
        MemSourceFileObject jfo = new MemSourceFileObject("__Wrapper__");
        jfo.addLine(source.toString());
        List<JavaFileObject> jfos = new ArrayList<JavaFileObject>();
        jfos.add(jfo);
        List<String> options = Collections.emptyList();
        boolean ok = compiler.getTask(null,
            rfm, // SHOULD be subclass we can get class file out of it?
            diagnostics,
            options,
            null,
            jfos).call();
        Class<?> example;
        if(!ok) {
            throw new IllegalArgumentException(diagnostics.getDiagnostics().toString());
        }
        try {
            example = rfm.getClassLoader(StandardLocation.CLASS_OUTPUT).loadClass("__Wrapper__");
             return hasMethod(example);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
     }
    
    /** Checks that the target class has a method with the same signature as
     * the only method in the example class.
     * @param example A class which declares a single method
     * @return true if the target class has a method with the same signature  as
     * the method declared by the example.
     * @throws IllegalArgumentException if example declares more than one method.
     */
    public boolean hasMethod(Class<?> example) {
        if(example.getDeclaredMethods().length != 1) {
            throw new IllegalArgumentException("example must declare exactly one method");
        }
         Method sample = example.getDeclaredMethods()[0]; // there's only one
         for(Method maybe : target.getDeclaredMethods()) {
             if(match(maybe,sample)) return true;
         }
         return false;
    }
    
    private boolean match(Method poss, Method sample) {
        if(poss.getModifiers() != sample.getModifiers()) return false;
        if(! poss.getName().equals(sample.getName())) return false;
        if(! matchesTypeParameters(poss.getTypeParameters(),sample.getTypeParameters())) return false;
        if(! orderedMatch(poss.getGenericParameterTypes(),sample.getGenericParameterTypes())) return false;
        if(! sameTypes(poss.getGenericReturnType(),sample.getGenericReturnType())) return false;
        if(! unorderedMatch(poss.getGenericExceptionTypes(), sample.getGenericExceptionTypes())) return false;
        return true;
    }

    private boolean matchesTypeParameters(TypeVariable<Method>[] vars1, TypeVariable<Method>[] vars2) {
        if(vars1.length != vars2.length) return false;
        for(int i=0; i < vars1.length; i++) {
            if(! sameTypes(vars1[i], vars2[i])) return false;
//            if(! vars1[i].getName().equals(vars2[i].getName())) return false;
//            if(! unorderedMatch(vars1[i].getBounds(), vars2[i].getBounds())) return false;
        }
        return true;
    }

    private boolean orderedMatch(Type[] type, Type[] type0) {
        if(type.length != type0.length) return false;
        for(int i=0; i < type.length; i++) {
            if(! sameTypes(type[i],type0[i])) return false;
        }
        return true;
    }

    private boolean unorderedMatch(Type[] ta1, Type[] ta2) {
        if(ta1.length != ta2.length) return false;
        boolean matched[] = new boolean[ta1.length];
outer:
        for(int i=0; i < ta1.length; i++) {
            for(int j=0; j < ta1.length; j++) {
                if((! matched[j]) && sameTypes(ta1[i],ta2[j])) {
                    matched[j] = true;
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    private boolean sameTypes(Type type1, Type type2) {
        if(type1 instanceof Class) {
            return type2 instanceof Class ? ((Class)type1).equals((Class)type2) : false;
        } else if(type1 instanceof GenericArrayType) {
            return type2 instanceof GenericArrayType ? sameTypes((GenericArrayType)type1, (GenericArrayType)type2) : false;
        } else if(type1 instanceof ParameterizedType) {
            return type2 instanceof ParameterizedType ? sameTypes((ParameterizedType)type1, (ParameterizedType)type2) : false;
        } else if(type1 instanceof TypeVariable) {
            return type2 instanceof TypeVariable ? sameTypes((TypeVariable)type1, (TypeVariable)type2) : false;
        } else if(type1 instanceof WildcardType) {
            return type2 instanceof WildcardType ? sameTypes((WildcardType)type1, (WildcardType)type2) : false;
        } else {
            return false;
        }
    }
    
    private boolean sameTypes(GenericArrayType t1, GenericArrayType t2) {
        return sameTypes(t1.getGenericComponentType(), t2.getGenericComponentType());
    }
    
    private boolean sameTypes(ParameterizedType t1, ParameterizedType t2) {
        return sameTypes(t1.getRawType(), t2.getRawType()) &&
            orderedMatch(t1.getActualTypeArguments(), t2.getActualTypeArguments());
    }
    
    private boolean sameTypes(TypeVariable v1, TypeVariable v2) {
        return v1.getName().equals(v2.getName()) && 
            unorderedMatch(v1.getBounds(), v2.getBounds());
    }
    
    private boolean sameTypes(WildcardType w1, WildcardType w2) {
        return unorderedMatch(w1.getLowerBounds(), w2.getLowerBounds()) &&
            unorderedMatch(w1.getUpperBounds(), w2.getUpperBounds());
    }
}

