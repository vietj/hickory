/*
 * ServiceProviderProcessor.java
 *
 * Created on 10 October 2007, 22:01
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.incremental;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 *
 * @author Bruce
 */
@SupportedAnnotationTypes("net.java.dev.hickory.incremental.ServiceProvider")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ServiceProviderProcessor extends AbstractProcessor {
    
    /** Creates a new instance of ServiceProviderProcessor */
    public ServiceProviderProcessor() {
    }

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
            System.out.format("generating service lookup for %s with %d entries%n",example.service, providers.size());
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
