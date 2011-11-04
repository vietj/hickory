/*
 * TypeAndLocation.java
 *
 * Created on 17 October 2007, 21:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.hickory.testing;

import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
/**
 * Used as a key into the Map that holds the file system
 * @author Bruce
 */
class LocationAndKind {
    private final Location location;
    private final Kind kind;
    
    /** Creates a new instance of TypeAndLocation */
    public LocationAndKind(Location location, Kind kind) {
        this.location = location;
        this.kind = kind;
    }

    public String toString() {
        return kind.toString() + "@" + location.toString(); 
    }

    public int hashCode() {
        return kind.hashCode() * 7  + location.hashCode();
    }

    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(! (obj instanceof LocationAndKind)) return false;
        LocationAndKind other = (LocationAndKind)obj;
        return location.equals(other.location) && kind.equals(other.kind);
    }
    
    
    
    
    
}
