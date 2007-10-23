/*
 * package-info.java
 *
 * Created on 18 October 2007, 21:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * Provides a high level abstraction of the javax.tools API designed
 * for testing annotation processors.
 * <p>
 * This API abstracts a Memory based implementation of a JavaFileManager suppporting
 * the compiler and javax.annotation.processing.Filer operations, along with an associated
 * ClassLoader for loading the compiled classes.
 *<p>
 * Multiple compilations utilising the same file system are supported for tests across
 * multiple compiler "runs".
 * 
 */

package net.java.dev.hickory.testing;
