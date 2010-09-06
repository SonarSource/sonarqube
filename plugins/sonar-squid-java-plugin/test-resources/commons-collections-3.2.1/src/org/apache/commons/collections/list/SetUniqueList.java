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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.apache.commons.collections.iterators.AbstractListIteratorDecorator;
import org.apache.commons.collections.set.UnmodifiableSet;

/**
 * Decorates a <code>List</code> to ensure that no duplicates are present
 * much like a <code>Set</code>.
 * <p>
 * The <code>List</code> interface makes certain assumptions/requirements.
 * This implementation breaks these in certain ways, but this is merely the
 * result of rejecting duplicates.
 * Each violation is explained in the method, but it should not affect you.
 * Bear in mind that Sets require immutable objects to function correctly.
 * <p>
 * The {@link org.apache.commons.collections.set.ListOrderedSet ListOrderedSet}
 * class provides an alternative approach, by wrapping an existing Set and
 * retaining insertion order in the iterator.
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Matthew Hawthorne
 * @author Stephen Colebourne
 * @author Tom Dunham
 */
public class SetUniqueList extends AbstractSerializableListDecorator {

    /** Serialization version */
    private static final long serialVersionUID = 7196982186153478694L;

    /**
     * Internal Set to maintain uniqueness.
     */
    protected final Set set;

    /**
     * Factory method to create a SetList using the supplied list to retain order.
     * <p>
     * If the list contains duplicates, these are removed (first indexed one kept).
     * A <code>HashSet</code> is used for the set behaviour.
     * 
     * @param list  the list to decorate, must not be null
     * @throws IllegalArgumentException if list is null
     */
    public static SetUniqueList decorate(List list) {
        if (list == null) {
            throw new IllegalArgumentException("List must not be null");
        }
        if (list.isEmpty()) {
            return new SetUniqueList(list, new HashSet());
        } else {
            List temp = new ArrayList(list);
            list.clear();
            SetUniqueList sl = new SetUniqueList(list, new HashSet());
            sl.addAll(temp);
            return sl;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies) the List and specifies the set to use.
     * <p>
     * The set and list must both be correctly initialised to the same elements.
     * 
     * @param set  the set to decorate, must not be null
     * @param list  the list to decorate, must not be null
     * @throws IllegalArgumentException if set or list is null
     */
    protected SetUniqueList(List list, Set set) {
        super(list);
        if (set == null) {
            throw new IllegalArgumentException("Set must not be null");
        }
        this.set = set;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets an unmodifiable view as a Set.
     * 
     * @return an unmodifiable set view
     */
    public Set asSet() {
        return UnmodifiableSet.decorate(set);
    }

    //-----------------------------------------------------------------------
    /**
     * Adds an element to the list if it is not already present.
     * <p>
     * <i>(Violation)</i>
     * The <code>List</code> interface requires that this method returns
     * <code>true</code> always. However this class may return <code>false</code>
     * because of the <code>Set</code> behaviour.
     * 
     * @param object the object to add
     * @return true if object was added
     */
    public boolean add(Object object) {
        // gets initial size
        final int sizeBefore = size();

        // adds element if unique
        add(size(), object);

        // compares sizes to detect if collection changed
        return (sizeBefore != size());
    }

    /**
     * Adds an element to a specific index in the list if it is not already present.
     * <p>
     * <i>(Violation)</i>
     * The <code>List</code> interface makes the assumption that the element is
     * always inserted. This may not happen with this implementation.
     * 
     * @param index  the index to insert at
     * @param object  the object to add
     */
    public void add(int index, Object object) {
        // adds element if it is not contained already
        if (set.contains(object) == false) {
            super.add(index, object);
            set.add(object);
        }
    }

    /**
     * Adds an element to the end of the list if it is not already present.
     * <p>
     * <i>(Violation)</i>
     * The <code>List</code> interface makes the assumption that the element is
     * always inserted. This may not happen with this implementation.
     * 
     * @param coll  the collection to add
     */
    public boolean addAll(Collection coll) {
        return addAll(size(), coll);
    }

    /**
     * Adds a collection of objects to the end of the list avoiding duplicates.
     * <p>
     * Only elements that are not already in this list will be added, and
     * duplicates from the specified collection will be ignored.
     * <p>
     * <i>(Violation)</i>
     * The <code>List</code> interface makes the assumption that the elements
     * are always inserted. This may not happen with this implementation.
     * 
     * @param index  the index to insert at
     * @param coll  the collection to add in iterator order
     * @return true if this collection changed
     */
    public boolean addAll(int index, Collection coll) {
        // gets initial size
        final int sizeBefore = size();

        // adds all elements
        for (final Iterator it = coll.iterator(); it.hasNext();) {
            add(it.next());
        }

        // compares sizes to detect if collection changed
        return sizeBefore != size();
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the value at the specified index avoiding duplicates.
     * <p>
     * The object is set into the specified index.
     * Afterwards, any previous duplicate is removed
     * If the object is not already in the list then a normal set occurs.
     * If it is present, then the old version is removed.
     * 
     * @param index  the index to insert at
     * @param object  the object to set
     * @return the previous object
     */
    public Object set(int index, Object object) {
        int pos = indexOf(object);
        Object removed = super.set(index, object);
        if (pos == -1 || pos == index) {
            return removed;
        }
        
        // the object is already in the uniq list
        // (and it hasn't been swapped with itself)
        super.remove(pos);  // remove the duplicate by index
        set.remove(removed);  // remove the item deleted by the set
        return removed;  // return the item deleted by the set
    }

    public boolean remove(Object object) {
        boolean result = super.remove(object);
        set.remove(object);
        return result;
    }

    public Object remove(int index) {
        Object result = super.remove(index);
        set.remove(result);
        return result;
    }

    public boolean removeAll(Collection coll) {
        boolean result = super.removeAll(coll);
        set.removeAll(coll);
        return result;
    }

    public boolean retainAll(Collection coll) {
        boolean result = super.retainAll(coll);
        set.retainAll(coll);
        return result;
    }

    public void clear() {
        super.clear();
        set.clear();
    }

    public boolean contains(Object object) {
        return set.contains(object);
    }

    public boolean containsAll(Collection coll) {
        return set.containsAll(coll);
    }

    public Iterator iterator() {
        return new SetListIterator(super.iterator(), set);
    }

    public ListIterator listIterator() {
        return new SetListListIterator(super.listIterator(), set);
    }

    public ListIterator listIterator(int index) {
        return new SetListListIterator(super.listIterator(index), set);
    }

    public List subList(int fromIndex, int toIndex) {
        return new SetUniqueList(super.subList(fromIndex, toIndex), set);
    }

    //-----------------------------------------------------------------------
    /**
     * Inner class iterator.
     */
    static class SetListIterator extends AbstractIteratorDecorator {
        
        protected final Set set;
        protected Object last = null;
        
        protected SetListIterator(Iterator it, Set set) {
            super(it);
            this.set = set;
        }
        
        public Object next() {
            last = super.next();
            return last;
        }

        public void remove() {
            super.remove();
            set.remove(last);
            last = null;
        }
    }
    
    /**
     * Inner class iterator.
     */
    static class SetListListIterator extends AbstractListIteratorDecorator {
        
        protected final Set set;
        protected Object last = null;
        
        protected SetListListIterator(ListIterator it, Set set) {
            super(it);
            this.set = set;
        }
        
        public Object next() {
            last = super.next();
            return last;
        }

        public Object previous() {
            last = super.previous();
            return last;
        }

        public void remove() {
            super.remove();
            set.remove(last);
            last = null;
        }

        public void add(Object object) {
            if (set.contains(object) == false) {
                super.add(object);
                set.add(object);
            }
        }
        
        public void set(Object object) {
            throw new UnsupportedOperationException("ListIterator does not support set");
        }
    }
    
}
