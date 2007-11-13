/*
 * StateSaver.java
 * 
 * Created on 8/09/2007, 20:30:54
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.dev.hickory.incremental;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 *
 *  When a single generated file is generated from more than one compilation unit
 * then normally a processor is not safe for use in incremental compilations.
 * This class saves state between rounds and between runs of a processor allowing
 * generation to occur even if some of the contributing data was obtained in previous
 * runs.
 * <p>
 * Imposes a strict ordering call sequence to enforce correct use and remove the need
 * for the caller to explicitly restore or save the persisted state.
 * <p>
 * Use of this class is illustrated by the following example which generates <i>provider-configuration files</i>
 *for use with {@link java.util.ServiceLoader} using an annotation on the Service Provider
 *which specifies the Service. For simplicity some checks are ommitted, and a Prism is used
 *to obtain the provider from the annotation.
 *<pre>
 *{@literal
   }{@literal @Target(ElementType.TYPE)
   }{@literal @Retention(RetentionPolicy.SOURCE)
public @interface ServiceProvider {
    Class<?> value();
}

    }{@literal @SupportedAnnotationTypes("ServiceProvider")
    }{@literal @SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ServiceProviderProcessor extends AbstractProcessor {
    
    public ServiceProviderProcessor() {}

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        
        if(roundEnv.processingOver() && ! roundEnv.errorRaised()) {
            generate();
        } else if(! roundEnv.errorRaised()) {
            StateSaver<State> stateSaver = StateSaver.getInstance(this, State.class, processingEnv);
            stateSaver.startRound(roundEnv);
            TypeElement serviceProviderElement = processingEnv.getElementUtils().getTypeElement(ServiceProvider.class.getCanonicalName());
            for(Element target : roundEnv.getElementsAnnotatedWith(serviceProviderElement)) {
                TypeElement targetType = (TypeElement)target;
                ServiceProviderPrism prism = ServiceProviderPrism.getInstanceOn(target);
                if(prism.isValid) {
                    DeclaredType service = (DeclaredType)prism.value();
                    // SHOULD check validity here. service is supertype of target, target has public no args constructor etc
                    State state = new State(((TypeElement)service.asElement()).getQualifiedName().toString(),
                            targetType.getQualifiedName().toString());
                    stateSaver.addData(targetType,state);
                }
            }
        }
        return true;
    }

    private void generate() {
        StateSaver<State> stateSaver = StateSaver.getInstance(this, State.class, processingEnv);
        Comparator<State> byService = new Comparator<State>() {
            public int compare(ServiceProviderProcessor.State o1, ServiceProviderProcessor.State o2) {
                return o1.service.compareTo(o2.service);
            }
        };
        Filer filer = processingEnv.getFiler();
        for(List<State> providers : stateSaver.getData(byService)) {
            State example = providers.get(0);
            try {
                FileObject f = filer.createResource(StandardLocation.CLASS_OUTPUT,"","META-INF/services/" + example.service);
                Writer out = f.openWriter();
                for(State provider : providers) {
                    out.write(provider.provider);
                    out.write("\n");
                }
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }
        
    static class State implements Serializable {
        String service;
        String provider;
        State(String service, String provider) {
            this.service = service;
            this.provider = provider;
        }
    }
}

 * }
 * </pre>
 * @param <T> The type of the data that will be saved in this StateSaver.
 * @author Bruce
 */
public class StateSaver<T extends Serializable> {

    static Map<String,StateSaver<?>> cache = new HashMap<String,StateSaver<?>>();
    
    Filer filer;
    static ProcessingEnvironment penv;

    private boolean stateSaved;
    private boolean stateRoundStarted;
    String persistenceName;
    
    
    private StateSaver(String processorFQN, Filer filer) {
        // TODO deserialize data if available
        persistenceName = processorFQN;
        this.filer = filer;
        restore();
    }
    
    
    /** Return an instance for use by the specified processor.
     * @param processor The processor that needs this instance. Is used to
     * determine the name of the file in which state is persisted. Consequently each
     * Processor can have only one StateSaver
     * @param type The Class of the data being managed by this StateSaver 
     * @param penv The current ProcessingEnvironment from which teh Filer can be obtained.
     * @param <K> The type of the data being managed by this StateSaver
     * */ 
    public static <K extends Serializable> StateSaver<K> getInstance(Processor processor, Class<K> type, ProcessingEnvironment penv) {
        // get from cache or make new and read serialized data from previous runs
        if(penv != StateSaver.penv) {
            // new run - save the filer and penv, and reset the cache
            StateSaver.penv = penv;
            cache.clear();
        }
        String key = processor.getClass().getName() + " " + type.getClass().getName();
        if(cache.containsKey(key)) {
            return (StateSaver<K>) cache.get(key);
        } else {
            StateSaver<K> result = new StateSaver<K>(key,penv.getFiler());
            cache.put(key,result);
            return result;
        }
    }
    
    private Map<String, List<T>> srcCompUnitData;
    
    /** This method must be called at the start of each round. It removes any remembered data 
     * for the compilation units in this round, so that calls to addData() during the round
     * will replace any previous data for those compilation units.
     * @param env The current round Environment.
     * @throws IllegalStateException if this is called after either of the getdata() methods
     * (since calling either getData() method implies that collecting new data for the run
     * is complete and the current collected state should be persisted).
     */
    public void startRound(RoundEnvironment env) {
        stateRoundStarted = true;
        if(stateSaved) throw new IllegalStateException("getData() has already been called in this processor run");
        for(Element ele : env.getRootElements()) {
            if(ele.getKind() == ElementKind.CLASS || ele.getKind() == ElementKind.INTERFACE) {
                TypeElement cu = (TypeElement)ele;
                String fqn = cu.getQualifiedName().toString();
                srcCompUnitData.remove(fqn);
            }
        }
    }
    
    /** Save some state obtained from the specified TypeElement.
     *  @param contributor The TypeElement from which the data was obtained
     *  @param data the Data to save.
     * @throws IllegalStateException if this is called after either of the getdata() methods
     * (since calling either getData() method implies that collecting new data for the run
     * is complete and the current collected state should be persisted).
     * @throws IllegalStateException if startRound() has not been called.
     */
    public void addData(TypeElement contributor, T data) {
        if(! stateRoundStarted) {
            throw new IllegalStateException("startRound() must be called before addData()");
        }
        String ownerFQN = contributor.getQualifiedName().toString();
        List<T> existing = srcCompUnitData.get(ownerFQN);
        if(existing == null) {
            existing = new ArrayList<T>();
            srcCompUnitData.put(ownerFQN,existing);
        }
        existing.add(data);
    }
    
    /** Return all the data that has been saved, during all runs 
     * including the current one.
     * */
    public List<T> getData() {
        if(! stateSaved) {
            save();
            stateSaved = true;
        }
        List<T> result = new ArrayList<T>();
        for(List<T> sub : srcCompUnitData.values() ) {
            result.addAll(sub);
        }
        return result;
    }
    
    /** Return all the data, grouped into lists of items which the 
     * comparator considers equal. If you supply a Comparator<T> which 
     * compares based on the FQN of the file (.java or .class) to be generated
     * then you can generate one file from each returned list.
     */
    public Iterable<List<T>> getData(Comparator<? super T> comparator) {
        List<T> flat = getData();
        Collections.sort(flat,comparator);
        List<List<T>> result = new ArrayList<List<T>>();
        T prev = null;
        List<T> current = null;
        for(T t : flat) {
            if(prev == null || comparator.compare(t, prev) != 0) {
                // start a new current list
                current = new ArrayList<T>();
                result.add(current);
            }
            current.add(t);
            prev = t;
        }
        return result;
    }
    
    private void save() {
        try {
            FileObject fo = filer.createResource(StandardLocation.SOURCE_OUTPUT,
                    StateSaver.class.getName(),
                    persistenceName + ".state");
            ObjectOutputStream oos = new ObjectOutputStream(fo.openOutputStream());
            oos.writeObject(srcCompUnitData);
            oos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void restore() {
        FileObject fo = null;
        try {
            fo = filer.getResource(StandardLocation.SOURCE_OUTPUT,
                    StateSaver.class.getName(),
                    persistenceName + ".state");
//            System.out.format("stateSaver - restoring %s %s %s%n",fo.getClass(),fo.getName(),fo.toUri());
            ObjectInputStream ois = new ObjectInputStream(fo.openInputStream());
            srcCompUnitData = (Map<String, List<T>>)ois.readObject();
            ois.close();
        } catch (FileNotFoundException fnf) {
            // thats OK, maybe this is first run after a clean
            srcCompUnitData = new HashMap<String,List<T>>();
        } catch (IOException ex) {
            System.err.format("reading: %s%n",fo.toUri());
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.err.format("reading: %s%n",fo.toUri());
            ex.printStackTrace();
        }
    }
}
