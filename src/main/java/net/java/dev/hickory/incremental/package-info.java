/*
 * package-info.java
 *
 * Created on 22 October 2007, 21:28
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/** 
 * This package contains utilities to assist writing annotation processors
 * so as to behave correctly in the face of incremental compilation.
 * <p> A processor which generates a single java file from a single compilation 
 * unit need not use these utilities.
 * <p> However when a single generated java file object results from more than one
 * compilation units, the processor should take care to behave correctly in the face of 
 * incremental compilations.
 */

package net.java.dev.hickory.incremental;
