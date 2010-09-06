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
package org.apache.commons.collections.list;

import java.util.List;

import org.apache.commons.collections.Factory;

/**
 * Decorates another <code>List</code> to create objects in the list on demand.
 * <p>
 * When the {@link #get(int)} method is called with an index greater than
 * the size of the list, the list will automatically grow in size and return
 * a new object from the specified factory. The gaps will be filled by null.
 * If a get method call encounters a null, it will be replaced with a new
 * object from the factory. Thus this list is unsuitable for storing null
 * objects.
 * <p>
 * For instance:
 *
 * <pre>
 * Factory factory = new Factory() {
 *     public Object create() {
 *         return new Date();
 *     }
 * }
 * List lazy = LazyList.decorate(new ArrayList(), factory);
 * Object obj = lazy.get(3);
 * </pre>
 *
 * After the above code is executed, <code>obj</code> will contain
 * a new <code>Date</code> instance.  Furthermore, that <code>Date</code>
 * instance is the fourth element in the list.  The first, second, 
 * and third element are all set to <code>null</code>.
 * <p>
 * This class differs from {@link GrowthList} because here growth occurs on
 * get, where <code>GrowthList</code> grows on set and add. However, they
 * could easily be used together by decorating twice.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @see GrowthList
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 * @author Arron Bates
 * @author Paul Jack
 */
public class LazyList extends AbstractSerializableListDecorator {

    /** Serialization version */
    private static final long serialVersionUID = -1708388017160694542L;

    /** The factory to use to lazily instantiate the objects */
    protected final Factory factory;

    /**
     * Factory method to create a lazily instantiating list.
     * 
     * @param list  the list to decorate, must not be null
     * @param factory  the factory to use for creation, must not be null
     * @throws IllegalArgumentException if list or factory is null
     */
    public static List decorate(List list, Factory factory) {
        return new LazyList(list, factory);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     * 
     * @param list  the list to decorate, must not be null
     * @param factory  the factory to use for creation, must not be null
     * @throws IllegalArgumentException if list or factory is null
     */
    protected LazyList(List list, Factory factory) {
        super(list);
        if (factory == null) {
            throw new IllegalArgumentException("Factory must not be null");
        }
        this.factory = factory;
    }

    //-----------------------------------------------------------------------
    /**
     * Decorate the get method to perform the lazy behaviour.
     * <p>
     * If the requested index is greater than the current size, the list will 
     * grow to the new size and a new object will be returned from the factory.
     * Indexes in-between the old size and the requested size are left with a 
     * placeholder that is replaced with a factory object when requested.
     * 
     * @param index  the index to retrieve
     */
    public Object get(int index) {
        int size = getList().size();
        if (index < size) {
            // within bounds, get the object
            Object object = getList().get(index);
            if (object == null) {
                // item is a place holder, create new one, set and return
                object = factory.create();
                getList().set(index, object);
                return object;
            } else {
                // good and ready to go
                return object;
            }
        } else {
            // we have to grow the list
            for (int i = size; i < index; i++) {
                getList().add(null);
            }
            // create our last object, set and return
            Object object = factory.create();
            getList().add(object);
            return object;
        }
    }


    public List subList(int fromIndex, int toIndex) {
        List sub = getList().subList(fromIndex, toIndex);
        return new LazyList(sub, factory);
    }

}
