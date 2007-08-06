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
