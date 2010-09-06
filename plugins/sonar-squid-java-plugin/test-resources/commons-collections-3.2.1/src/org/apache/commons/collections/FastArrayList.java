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

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>A customized implementation of <code>java.util.ArrayList</code> designed
 * to operate in a multithreaded environment where the large majority of
 * method calls are read-only, instead of structural changes.  When operating
 * in "fast" mode, read calls are non-synchronized and write calls perform the
 * following steps:</p>
 * <ul>
 * <li>Clone the existing collection
 * <li>Perform the modification on the clone
 * <li>Replace the existing collection with the (modified) clone
 * </ul>
 * <p>When first created, objects of this class default to "slow" mode, where
 * all accesses of any type are synchronized but no cloning takes place.  This
 * is appropriate for initially populating the collection, followed by a switch
 * to "fast" mode (by calling <code>setFast(true)</code>) after initialization
 * is complete.</p>
 *
 * <p><strong>NOTE</strong>: If you are creating and accessing an
 * <code>ArrayList</code> only within a single thread, you should use
 * <code>java.util.ArrayList</code> directly (with no synchronization), for
 * maximum performance.</p>
 *
 * <p><strong>NOTE</strong>: <i>This class is not cross-platform.
 * Using it may cause unexpected failures on some architectures.</i>
 * It suffers from the same problems as the double-checked locking idiom.  
 * In particular, the instruction that clones the internal collection and the 
 * instruction that sets the internal reference to the clone can be executed 
 * or perceived out-of-order.  This means that any read operation might fail 
 * unexpectedly, as it may be reading the state of the internal collection
 * before the internal collection is fully formed.
 * For more information on the double-checked locking idiom, see the
 * <a href="http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html">
 * Double-Checked Locking Idiom Is Broken Declaration</a>.</p>
 *
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Craig R. McClanahan
 * @author Stephen Colebourne
 */
public class FastArrayList extends ArrayList {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a an empty list.
     */
    public FastArrayList() {

        super();
        this.list = new ArrayList();

    }


    /**
     * Construct an empty list with the specified capacity.
     *
     * @param capacity The initial capacity of the empty list
     */
    public FastArrayList(int capacity) {

        super();
        this.list = new ArrayList(capacity);

    }


    /**
     * Construct a list containing the elements of the specified collection,
     * in the order they are returned by the collection's iterator.
     *
     * @param collection The collection whose elements initialize the contents
     *  of this list
     */
    public FastArrayList(Collection collection) {

        super();
        this.list = new ArrayList(collection);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The underlying list we are managing.
     */
    protected ArrayList list = null;


    // ------------------------------------------------------------- Properties


    /**
     * Are we operating in "fast" mode?
     */
    protected boolean fast = false;


    /**
     *  Returns true if this list is operating in fast mode.
     *
     *  @return true if this list is operating in fast mode
     */
    public boolean getFast() {
        return (this.fast);
    }

    /**
     *  Sets whether this list will operate in fast mode.
     *
     *  @param fast true if the list should operate in fast mode
     */
    public void setFast(boolean fast) {
        this.fast = fast;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Appends the specified element to the end of this list.
     *
     * @param element The element to be appended
     */
    public boolean add(Object element) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                boolean result = temp.add(element);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.add(element));
            }
        }

    }


    /**
     * Insert the specified element at the specified position in this list,
     * and shift all remaining elements up one position.
     *
     * @param index Index at which to insert this element
     * @param element The element to be inserted
     *
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public void add(int index, Object element) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                temp.add(index, element);
                list = temp;
            }
        } else {
            synchronized (list) {
                list.add(index, element);
            }
        }

    }


    /**
     * Append all of the elements in the specified Collection to the end
     * of this list, in the order that they are returned by the specified
     * Collection's Iterator.
     *
     * @param collection The collection to be appended
     */
    public boolean addAll(Collection collection) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                boolean result = temp.addAll(collection);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.addAll(collection));
            }
        }

    }


    /**
     * Insert all of the elements in the specified Collection at the specified
     * position in this list, and shift any previous elements upwards as
     * needed.
     *
     * @param index Index at which insertion takes place
     * @param collection The collection to be added
     *
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public boolean addAll(int index, Collection collection) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                boolean result = temp.addAll(index, collection);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.addAll(index, collection));
            }
        }

    }


    /**
     * Remove all of the elements from this list.  The list will be empty
     * after this call returns.
     *
     * @exception UnsupportedOperationException if <code>clear()</code>
     *  is not supported by this list
     */
    public void clear() {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                temp.clear();
                list = temp;
            }
        } else {
            synchronized (list) {
                list.clear();
            }
        }

    }


    /**
     * Return a shallow copy of this <code>FastArrayList</code> instance.
     * The elements themselves are not copied.
     */
    public Object clone() {

        FastArrayList results = null;
        if (fast) {
            results = new FastArrayList(list);
        } else {
            synchronized (list) {
                results = new FastArrayList(list);
            }
        }
        results.setFast(getFast());
        return (results);

    }


    /**
     * Return <code>true</code> if this list contains the specified element.
     *
     * @param element The element to test for
     */
    public boolean contains(Object element) {

        if (fast) {
            return (list.contains(element));
        } else {
            synchronized (list) {
                return (list.contains(element));
            }
        }

    }


    /**
     * Return <code>true</code> if this list contains all of the elements
     * in the specified Collection.
     *
     * @param collection Collection whose elements are to be checked
     */
    public boolean containsAll(Collection collection) {

        if (fast) {
            return (list.containsAll(collection));
        } else {
            synchronized (list) {
                return (list.containsAll(collection));
            }
        }

    }


    /**
     * Increase the capacity of this <code>ArrayList</code> instance, if
     * necessary, to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument.
     *
     * @param capacity The new minimum capacity
     */
    public void ensureCapacity(int capacity) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                temp.ensureCapacity(capacity);
                list = temp;
            }
        } else {
            synchronized (list) {
                list.ensureCapacity(capacity);
            }
        }

    }


    /**
     * Compare the specified object with this list for equality.  This
     * implementation uses exactly the code that is used to define the
     * list equals function in the documentation for the
     * <code>List.equals</code> method.
     *
     * @param o Object to be compared to this list
     */
    public boolean equals(Object o) {

        // Simple tests that require no synchronization
        if (o == this)
            return (true);
        else if (!(o instanceof List))
            return (false);
        List lo = (List) o;

        // Compare the sets of elements for equality
        if (fast) {
            ListIterator li1 = list.listIterator();
            ListIterator li2 = lo.listIterator();
            while (li1.hasNext() && li2.hasNext()) {
                Object o1 = li1.next();
                Object o2 = li2.next();
                if (!(o1 == null ? o2 == null : o1.equals(o2)))
                    return (false);
            }
            return (!(li1.hasNext() || li2.hasNext()));
        } else {
            synchronized (list) {
                ListIterator li1 = list.listIterator();
                ListIterator li2 = lo.listIterator();
                while (li1.hasNext() && li2.hasNext()) {
                    Object o1 = li1.next();
                    Object o2 = li2.next();
                    if (!(o1 == null ? o2 == null : o1.equals(o2)))
                        return (false);
                }
                return (!(li1.hasNext() || li2.hasNext()));
            }
        }

    }


    /**
     * Return the element at the specified position in the list.
     *
     * @param index The index of the element to return
     *
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public Object get(int index) {

        if (fast) {
            return (list.get(index));
        } else {
            synchronized (list) {
                return (list.get(index));
            }
        }

    }


    /**
     * Return the hash code value for this list.  This implementation uses
     * exactly the code that is used to define the list hash function in the
     * documentation for the <code>List.hashCode</code> method.
     */
    public int hashCode() {

        if (fast) {
            int hashCode = 1;
            java.util.Iterator i = list.iterator();
            while (i.hasNext()) {
                Object o = i.next();
                hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
            }
            return (hashCode);
        } else {
            synchronized (list) {
                int hashCode = 1;
                java.util.Iterator i = list.iterator();
                while (i.hasNext()) {
                    Object o = i.next();
                    hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
                }
                return (hashCode);
            }
        }

    }


    /**
     * Search for the first occurrence of the given argument, testing
     * for equality using the <code>equals()</code> method, and return
     * the corresponding index, or -1 if the object is not found.
     *
     * @param element The element to search for
     */
    public int indexOf(Object element) {

        if (fast) {
            return (list.indexOf(element));
        } else {
            synchronized (list) {
                return (list.indexOf(element));
            }
        }

    }


    /**
     * Test if this list has no elements.
     */
    public boolean isEmpty() {

        if (fast) {
            return (list.isEmpty());
        } else {
            synchronized (list) {
                return (list.isEmpty());
            }
        }

    }


    /**
     * Return an iterator over the elements in this list in proper sequence.
     * <p>
     * <b>Thread safety</b><br />
     * The iterator returned is thread-safe ONLY in FAST mode.
     * In slow mode there is no way to synchronize, or make the iterator thread-safe.
     * <p>
     * In fast mode iteration and modification may occur in parallel on different threads,
     * however there is a restriction. Modification must be EITHER via the Iterator
     * interface methods OR the List interface. If a mixture of modification
     * methods is used a ConcurrentModificationException is thrown from the iterator
     * modification method. If the List modification methods are used the changes are
     * NOT visible in the iterator (it shows the list contents at the time the iterator
     * was created).
     * 
     * @return the iterator
     */
    public Iterator iterator() {
        if (fast) {
            return new ListIter(0);
        } else {
            return list.iterator();
        }
    }


    /**
     * Search for the last occurrence of the given argument, testing
     * for equality using the <code>equals()</code> method, and return
     * the corresponding index, or -1 if the object is not found.
     *
     * @param element The element to search for
     */
    public int lastIndexOf(Object element) {

        if (fast) {
            return (list.lastIndexOf(element));
        } else {
            synchronized (list) {
                return (list.lastIndexOf(element));
            }
        }

    }


    /**
     * Return an iterator of the elements of this list, in proper sequence.
     * <p>
     * <b>Thread safety</b><br />
     * The iterator returned is thread-safe ONLY in FAST mode.
     * In slow mode there is no way to synchronize, or make the iterator thread-safe.
     * <p>
     * In fast mode iteration and modification may occur in parallel on different threads,
     * however there is a restriction. Modification must be EITHER via the Iterator
     * interface methods OR the List interface. If a mixture of modification
     * methods is used a ConcurrentModificationException is thrown from the iterator
     * modification method. If the List modification methods are used the changes are
     * NOT visible in the iterator (it shows the list contents at the time the iterator
     * was created).
     * 
     * @return the list iterator
     */
    public ListIterator listIterator() {
        if (fast) {
            return new ListIter(0);
        } else {
            return list.listIterator();
        }
    }


    /**
     * Return an iterator of the elements of this list, in proper sequence,
     * starting at the specified position.
     * <p>
     * <b>Thread safety</b><br />
     * The iterator returned is thread-safe ONLY in FAST mode.
     * In slow mode there is no way to synchronize, or make the iterator thread-safe.
     * <p>
     * In fast mode iteration and modification may occur in parallel on different threads,
     * however there is a restriction. Modification must be EITHER via the Iterator
     * interface methods OR the List interface. If a mixture of modification
     * methods is used a ConcurrentModificationException is thrown from the iterator
     * modification method. If the List modification methods are used the changes are
     * NOT visible in the iterator (it shows the list contents at the time the iterator
     * was created).
     *
     * @param index The starting position of the iterator to return
     * @return the list iterator
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public ListIterator listIterator(int index) {
        if (fast) {
            return new ListIter(index);
        } else {
            return list.listIterator(index);
        }
    }


    /**
     * Remove the element at the specified position in the list, and shift
     * any subsequent elements down one position.
     *
     * @param index Index of the element to be removed
     *
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public Object remove(int index) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                Object result = temp.remove(index);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.remove(index));
            }
        }

    }


    /**
     * Remove the first occurrence of the specified element from the list,
     * and shift any subsequent elements down one position.
     *
     * @param element Element to be removed
     */
    public boolean remove(Object element) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                boolean result = temp.remove(element);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.remove(element));
            }
        }

    }


    /**
     * Remove from this collection all of its elements that are contained
     * in the specified collection.
     *
     * @param collection Collection containing elements to be removed
     *
     * @exception UnsupportedOperationException if this optional operation
     *  is not supported by this list
     */
    public boolean removeAll(Collection collection) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                boolean result = temp.removeAll(collection);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.removeAll(collection));
            }
        }

    }


    /**
     * Remove from this collection all of its elements except those that are
     * contained in the specified collection.
     *
     * @param collection Collection containing elements to be retained
     *
     * @exception UnsupportedOperationException if this optional operation
     *  is not supported by this list
     */
    public boolean retainAll(Collection collection) {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                boolean result = temp.retainAll(collection);
                list = temp;
                return (result);
            }
        } else {
            synchronized (list) {
                return (list.retainAll(collection));
            }
        }

    }


    /**
     * Replace the element at the specified position in this list with
     * the specified element.  Returns the previous object at that position.
     * <br><br>
     * <strong>IMPLEMENTATION NOTE</strong> - This operation is specifically
     * documented to not be a structural change, so it is safe to be performed
     * without cloning.
     *
     * @param index Index of the element to replace
     * @param element The new element to be stored
     *
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public Object set(int index, Object element) {

        if (fast) {
            return (list.set(index, element));
        } else {
            synchronized (list) {
                return (list.set(index, element));
            }
        }

    }


    /**
     * Return the number of elements in this list.
     */
    public int size() {

        if (fast) {
            return (list.size());
        } else {
            synchronized (list) {
                return (list.size());
            }
        }

    }


    /**
     * Return a view of the portion of this list between fromIndex
     * (inclusive) and toIndex (exclusive).  The returned list is backed
     * by this list, so non-structural changes in the returned list are
     * reflected in this list.  The returned list supports
     * all of the optional list operations supported by this list.
     *
     * @param fromIndex The starting index of the sublist view
     * @param toIndex The index after the end of the sublist view
     *
     * @exception IndexOutOfBoundsException if an index is out of range
     */
    public List subList(int fromIndex, int toIndex) {
        if (fast) {
            return new SubList(fromIndex, toIndex);
        } else {
            return list.subList(fromIndex, toIndex);
        }
    }


    /**
     * Return an array containing all of the elements in this list in the
     * correct order.
     */
    public Object[] toArray() {

        if (fast) {
            return (list.toArray());
        } else {
            synchronized (list) {
                return (list.toArray());
            }
        }

    }


    /**
     * Return an array containing all of the elements in this list in the
     * correct order.  The runtime type of the returned array is that of
     * the specified array.  If the list fits in the specified array, it is
     * returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array, and the size of this list.
     *
     * @param array Array defining the element type of the returned list
     *
     * @exception ArrayStoreException if the runtime type of <code>array</code>
     *  is not a supertype of the runtime type of every element in this list
     */
    public Object[] toArray(Object array[]) {

        if (fast) {
            return (list.toArray(array));
        } else {
            synchronized (list) {
                return (list.toArray(array));
            }
        }

    }


    /**
     * Return a String representation of this object.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("FastArrayList[");
        sb.append(list.toString());
        sb.append("]");
        return (sb.toString());

    }


    /**
     * Trim the capacity of this <code>ArrayList</code> instance to be the
     * list's current size.  An application can use this operation to minimize
     * the storage of an <code>ArrayList</code> instance.
     */
    public void trimToSize() {

        if (fast) {
            synchronized (this) {
                ArrayList temp = (ArrayList) list.clone();
                temp.trimToSize();
                list = temp;
            }
        } else {
            synchronized (list) {
                list.trimToSize();
            }
        }

    }



    private class SubList implements List {

        private int first;
        private int last;
        private List expected;


        public SubList(int first, int last) {
            this.first = first;
            this.last = last;
            this.expected = list;
        }

        private List get(List l) {
            if (list != expected) {
                throw new ConcurrentModificationException();
            }
            return l.subList(first, last);
        }

        public void clear() {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    get(temp).clear();
                    last = first;
                    list = temp;
                    expected = temp;
                }
            } else {
                synchronized (list) {
                    get(expected).clear();
                }
            }
        }

        public boolean remove(Object o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    boolean r = get(temp).remove(o);
                    if (r) last--;
                    list = temp;
                    expected = temp;
                    return r;
                }
            } else {
                synchronized (list) {
                    return get(expected).remove(o);
                }
            }
        }

        public boolean removeAll(Collection o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    List sub = get(temp);
                    boolean r = sub.removeAll(o);
                    if (r) last = first + sub.size();
                    list = temp;
                    expected = temp;
                    return r;
                }
            } else {
                synchronized (list) {
                    return get(expected).removeAll(o);
                }
            }
        }

        public boolean retainAll(Collection o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    List sub = get(temp);
                    boolean r = sub.retainAll(o);
                    if (r) last = first + sub.size();
                    list = temp;
                    expected = temp;
                    return r;
                }
            } else {
                synchronized (list) {
                    return get(expected).retainAll(o);
                }
            }
        }

        public int size() {
            if (fast) {
                return get(expected).size();
            } else {
                synchronized (list) {
                    return get(expected).size();
                }
            }
        }


        public boolean isEmpty() {
            if (fast) {
                return get(expected).isEmpty();
            } else {
                synchronized (list) {
                    return get(expected).isEmpty();
                }
            }
        }

        public boolean contains(Object o) {
            if (fast) {
                return get(expected).contains(o);
            } else {
                synchronized (list) {
                    return get(expected).contains(o);
                }
            }
        }

        public boolean containsAll(Collection o) {
            if (fast) {
                return get(expected).containsAll(o);
            } else {
                synchronized (list) {
                    return get(expected).containsAll(o);
                }
            }
        }

        public Object[] toArray(Object[] o) {
            if (fast) {
                return get(expected).toArray(o);
            } else {
                synchronized (list) {
                    return get(expected).toArray(o);
                }
            }
        }

        public Object[] toArray() {
            if (fast) {
                return get(expected).toArray();
            } else {
                synchronized (list) {
                    return get(expected).toArray();
                }
            }
        }


        public boolean equals(Object o) {
            if (o == this) return true;
            if (fast) {
                return get(expected).equals(o);
            } else {
                synchronized (list) {
                    return get(expected).equals(o);
                }
            }
        }

        public int hashCode() {
            if (fast) {
                return get(expected).hashCode();
            } else {
                synchronized (list) {
                    return get(expected).hashCode();
                }
            }
        }

        public boolean add(Object o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    boolean r = get(temp).add(o);
                    if (r) last++;
                    list = temp;
                    expected = temp;
                    return r;
                }
            } else {
                synchronized (list) {
                    return get(expected).add(o);
                }
            }
        }

        public boolean addAll(Collection o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    boolean r = get(temp).addAll(o);
                    if (r) last += o.size();
                    list = temp;
                    expected = temp;
                    return r;
                }
            } else {
                synchronized (list) {
                    return get(expected).addAll(o);
                }
            }
        }

        public void add(int i, Object o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    get(temp).add(i, o);
                    last++;
                    list = temp;
                    expected = temp;
                }
            } else {
                synchronized (list) {
                    get(expected).add(i, o);
                }
            }
        }

        public boolean addAll(int i, Collection o) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    boolean r = get(temp).addAll(i, o);
                    list = temp;
                    if (r) last += o.size();
                    expected = temp;
                    return r;
                }
            } else {
                synchronized (list) {
                    return get(expected).addAll(i, o);
                }
            }
        }

        public Object remove(int i) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    Object o = get(temp).remove(i);
                    last--;
                    list = temp;
                    expected = temp;
                    return o;
                }
            } else {
                synchronized (list) {
                    return get(expected).remove(i);
                }
            }
        }

        public Object set(int i, Object a) {
            if (fast) {
                synchronized (FastArrayList.this) {
                    ArrayList temp = (ArrayList) list.clone();
                    Object o = get(temp).set(i, a);
                    list = temp;
                    expected = temp;
                    return o;
                }
            } else {
                synchronized (list) {
                    return get(expected).set(i, a);
                }
            }
        }


        public Iterator iterator() {
            return new SubListIter(0);
        }

        public ListIterator listIterator() {
            return new SubListIter(0);
        }

        public ListIterator listIterator(int i) {
            return new SubListIter(i);
        }


        public Object get(int i) {
            if (fast) {
                return get(expected).get(i);
            } else {
                synchronized (list) {
                    return get(expected).get(i);
                }
            }
        }

        public int indexOf(Object o) {
            if (fast) {
                return get(expected).indexOf(o);
            } else {
                synchronized (list) {
                    return get(expected).indexOf(o);
                }
            }
        }


        public int lastIndexOf(Object o) {
            if (fast) {
                return get(expected).lastIndexOf(o);
            } else {
                synchronized (list) {
                    return get(expected).lastIndexOf(o);
                }
            }
        }


        public List subList(int f, int l) {
            if (list != expected) {
                throw new ConcurrentModificationException();
            }
            return new SubList(first + f, f + l);
        }


    private class SubListIter implements ListIterator {

        private List expected;
        private ListIterator iter;
        private int lastReturnedIndex = -1;


        public SubListIter(int i) {
            this.expected = list;
            this.iter = SubList.this.get(expected).listIterator(i);
        }

        private void checkMod() {
            if (list != expected) {
                throw new ConcurrentModificationException();
            }
        }

        List get() {
            return SubList.this.get(expected);
        }

        public boolean hasNext() {
            checkMod();
            return iter.hasNext();     
        }

        public Object next() {
            checkMod();
            lastReturnedIndex = iter.nextIndex();
            return iter.next();
        }

        public boolean hasPrevious() {
            checkMod();
            return iter.hasPrevious();
        }

        public Object previous() {
            checkMod();
            lastReturnedIndex = iter.previousIndex();
            return iter.previous();
        }

        public int previousIndex() {
            checkMod();
            return iter.previousIndex();
        }

        public int nextIndex() {
            checkMod();
            return iter.nextIndex();
        }

        public void remove() {
            checkMod();
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            get().remove(lastReturnedIndex);
            last--;
            expected = list;
            iter = get().listIterator(lastReturnedIndex);
            lastReturnedIndex = -1;
        }

        public void set(Object o) {
            checkMod();
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            get().set(lastReturnedIndex, o);
            expected = list;
            iter = get().listIterator(previousIndex() + 1);
        } 

        public void add(Object o) {
            checkMod();
            int i = nextIndex();
            get().add(i, o);
            last++;
            expected = list;
            iter = get().listIterator(i + 1);
            lastReturnedIndex = -1;
        }

   }


    }



    private class ListIter implements ListIterator {

        private List expected;
        private ListIterator iter;
        private int lastReturnedIndex = -1;


        public ListIter(int i) {
            this.expected = list;
            this.iter = get().listIterator(i);
        }

        private void checkMod() {
            if (list != expected) {
                throw new ConcurrentModificationException();
            }
        }

        List get() {
            return expected;
        }

        public boolean hasNext() {
            return iter.hasNext();     
        }

        public Object next() {
            lastReturnedIndex = iter.nextIndex();
            return iter.next();
        }

        public boolean hasPrevious() {
            return iter.hasPrevious();
        }

        public Object previous() {
            lastReturnedIndex = iter.previousIndex();
            return iter.previous();
        }

        public int previousIndex() {
            return iter.previousIndex();
        }

        public int nextIndex() {
            return iter.nextIndex();
        }

        public void remove() {
            checkMod();
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            get().remove(lastReturnedIndex);
            expected = list;
            iter = get().listIterator(lastReturnedIndex);
            lastReturnedIndex = -1;
        }

        public void set(Object o) {
            checkMod();
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            get().set(lastReturnedIndex, o);
            expected = list;
            iter = get().listIterator(previousIndex() + 1);
        } 

        public void add(Object o) {
            checkMod();
            int i = nextIndex();
            get().add(i, o);
            expected = list;
            iter = get().listIterator(i + 1);
            lastReturnedIndex = -1;
        }

   }
}
