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
package org.apache.commons.collections.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.FactoryTransformer;

/**
 * Decorates another <code>Map</code> returning a default value if the map
 * does not contain the requested key.
 * <p>
 * When the {@link #get(Object)} method is called with a key that does not
 * exist in the map, this map will return the default value specified in
 * the constructor/factory. Only the get method is altered, so the
 * {@link Map#containsKey(Object)} can be used to determine if a key really
 * is in the map or not.
 * <p>
 * The defaulted value is not added to the map.
 * Compare this behaviour with {@link LazyMap}, which does add the value
 * to the map (via a Transformer).
 * <p>
 * For instance:
 * <pre>
 * Map map = new DefaultedMap("NULL");
 * Object obj = map.get("Surname");
 * // obj == "NULL"
 * </pre>
 * After the above code is executed the map is still empty.
 * <p>
 * <strong>Note that DefaultedMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw 
 * exceptions when accessed by concurrent threads without synchronization.
 *
 * @since Commons Collections 3.2
 * @version $Revision: 1.7 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 * @author Rafael U.C. Afonso
 * @see LazyMap
 */
public class DefaultedMap
        extends AbstractMapDecorator
        implements Map, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 19698628745827L;

    /** The transformer to use if the map does not contain a key */
    protected final Object value;

    //-----------------------------------------------------------------------
    /**
     * Factory method to create a defaulting map.
     * <p>
     * The value specified is returned when a missing key is found.
     * 
     * @param map  the map to decorate, must not be null
     * @param defaultValue  the default value to return when the key is not found
     * @throws IllegalArgumentException if map is null
     */
    public static Map decorate(Map map, Object defaultValue) {
        if (defaultValue instanceof Transformer) {
            defaultValue = ConstantTransformer.getInstance(defaultValue);
        }
        return new DefaultedMap(map, defaultValue);
    }

    /**
     * Factory method to create a defaulting map.
     * <p>
     * The factory specified is called when a missing key is found.
     * The result will be returned as the result of the map get(key) method.
     * 
     * @param map  the map to decorate, must not be null
     * @param factory  the factory to use, must not be null
     * @throws IllegalArgumentException if map or factory is null
     */
    public static Map decorate(Map map, Factory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Factory must not be null");
        }
        return new DefaultedMap(map, FactoryTransformer.getInstance(factory));
    }

    /**
     * Factory method to create a defaulting map.
     * <p>
     * The transformer specified is called when a missing key is found.
     * The key is passed to the transformer as the input, and the result
     * will be returned as the result of the map get(key) method.
     * 
     * @param map  the map to decorate, must not be null
     * @param factory  the factory to use, must not be null
     * @throws IllegalArgumentException if map or factory is null
     */
    public static Map decorate(Map map, Transformer factory) {
        if (factory == null) {
           throw new IllegalArgumentException("Transformer must not be null");
       }
       return new DefaultedMap(map, factory);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructs a new empty <code>DefaultedMap</code> that decorates
     * a <code>HashMap</code>.
     * <p>
     * The object passed in will be returned by the map whenever an
     * unknown key is requested.
     * 
     * @param defaultValue  the default value to return when the key is not found
     */
    public DefaultedMap(Object defaultValue) {
        super(new HashMap());
        if (defaultValue instanceof Transformer) {
            defaultValue = ConstantTransformer.getInstance(defaultValue);
        }
        this.value = defaultValue;
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param value  the value to use
     * @throws IllegalArgumentException if map or transformer is null
     */
    protected DefaultedMap(Map map, Object value) {
        super(map);
        this.value = value;
    }

    //-----------------------------------------------------------------------
    /**
     * Write the map out using a custom routine.
     * 
     * @param out  the output stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(map);
    }

    /**
     * Read the map in using a custom routine.
     * 
     * @param in  the input stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        map = (Map) in.readObject();
    }

    //-----------------------------------------------------------------------
    public Object get(Object key) {
        // create value for key if key is not currently in the map
        if (map.containsKey(key) == false) {
            if (value instanceof Transformer) {
                return ((Transformer) value).transform(key);
            }
            return value;
        }
        return map.get(key);
    }

    // no need to wrap keySet, entrySet or values as they are views of
    // existing map entries - you can't do a map-style get on them.
}
