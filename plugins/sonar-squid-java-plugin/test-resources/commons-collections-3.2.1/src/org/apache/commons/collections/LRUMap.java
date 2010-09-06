/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;

/**
 * <p>
 * An implementation of a Map which has a maximum size and uses a Least Recently Used
 * algorithm to remove items from the Map when the maximum size is reached and new items are added.
 * </p>
 * 
 * <p>
 * A synchronized version can be obtained with:
 * <code>Collections.synchronizedMap( theMapToSynchronize )</code>
 * If it will be accessed by multiple threads, you _must_ synchronize access
 * to this Map.  Even concurrent get(Object) operations produce indeterminate
 * behaviour.
 * </p>
 * 
 * <p>
 * Unlike the Collections 1.0 version, this version of LRUMap does use a true
 * LRU algorithm.  The keys for all gets and puts are moved to the front of
 * the list.  LRUMap is now a subclass of SequencedHashMap, and the "LRU"
 * key is now equivalent to LRUMap.getFirst().
 * </p>
 * 
 * @deprecated Moved to map subpackage. Due to be removed in v4.0.
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @author <a href="mailto:morgand@apache.org">Morgan Delagrange</a>
 */
public class LRUMap extends SequencedHashMap implements Externalizable {
        
    private int maximumSize = 0;

    /**
     * Default constructor, primarily for the purpose of
     * de-externalization.  This constructors sets a default
     * LRU limit of 100 keys, but this value may be overridden
     * internally as a result of de-externalization.
     */
    public LRUMap() {
        this( 100 );
    }

    /**
     * Create a new LRUMap with a maximum capacity of <i>i</i>.
     * Once <i>i</i> capacity is achieved, subsequent gets
     * and puts will push keys out of the map.  See .
     * 
     * @param i      Maximum capacity of the LRUMap
     */
    public LRUMap(int i) {
        super( i );
        maximumSize = i;
    }

    /**
     * <p>Get the value for a key from the Map.  The key
     * will be promoted to the Most Recently Used position.
     * Note that get(Object) operations will modify
     * the underlying Collection.  Calling get(Object)
     * inside of an iteration over keys, values, etc. is
     * currently unsupported.</p>
     * 
     * @param key    Key to retrieve
     * @return Returns the value.  Returns null if the key has a
     *         null value <i>or</i> if the key has no value.
     */
    public Object get(Object key) {
        if(!containsKey(key)) return null;

        Object value = remove(key);
        super.put(key,value);
        return value;
    }

     /**
      * <p>Removes the key and its Object from the Map.</p>
      * 
      * <p>(Note: this may result in the "Least Recently Used"
      * object being removed from the Map.  In that case,
      * the removeLRU() method is called.  See javadoc for
      * removeLRU() for more details.)</p>
      * 
      * @param key    Key of the Object to add.
      * @param value  Object to add
      * @return Former value of the key
      */    
    public Object put( Object key, Object value ) {

        int mapSize = size();
        Object retval = null;

        if ( mapSize >= maximumSize ) {

            // don't retire LRU if you are just
            // updating an existing key
            if (!containsKey(key)) {
                // lets retire the least recently used item in the cache
                removeLRU();
            }
        }

        retval = super.put(key,value);

        return retval;
    }

    /**
     * This method is used internally by the class for 
     * finding and removing the LRU Object.
     */
    protected void removeLRU() {
        Object key = getFirstKey();
        // be sure to call super.get(key), or you're likely to 
        // get infinite promotion recursion
        Object value = super.get(key);
        
        remove(key);

        processRemovedLRU(key,value);
    }

    /**
     * Subclasses of LRUMap may hook into this method to
     * provide specialized actions whenever an Object is
     * automatically removed from the cache.  By default,
     * this method does nothing.
     * 
     * @param key    key that was removed
     * @param value  value of that key (can be null)
     */
    protected void processRemovedLRU(Object key, Object value) {
    }
 
    // Externalizable interface
    //-------------------------------------------------------------------------        
    public void readExternal( ObjectInput in )  throws IOException, ClassNotFoundException {
        maximumSize = in.readInt();
        int size = in.readInt();
        
        for( int i = 0; i < size; i++ )  {
            Object key = in.readObject();
            Object value = in.readObject();
            put(key,value);
        }
    }

    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeInt( maximumSize );
        out.writeInt( size() );
        for( Iterator iterator = keySet().iterator(); iterator.hasNext(); ) {
            Object key = iterator.next();
            out.writeObject( key );
            // be sure to call super.get(key), or you're likely to 
            // get infinite promotion recursion
            Object value = super.get( key );
            out.writeObject( value );
        }
    }
    
    
    // Properties
    //-------------------------------------------------------------------------        
    /** Getter for property maximumSize.
     * @return Value of property maximumSize.
     */
    public int getMaximumSize() {
        return maximumSize;
    }
    /** Setter for property maximumSize.
     * @param maximumSize New value of property maximumSize.
     */
    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
        while (size() > maximumSize) {
            removeLRU();
        }
    }


    // add a serial version uid, so that if we change things in the future
    // without changing the format, we can still deserialize properly.
    private static final long serialVersionUID = 2197433140769957051L;
}
