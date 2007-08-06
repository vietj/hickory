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
