/*
 * ServiceProvider.java
 *
 * Created on 10 October 2007, 22:01
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.incremental;

import java.lang.annotation.*;

/**
 *
 * @author Bruce
 */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
public @interface ServiceProvider {
    Class<?> value();
}
