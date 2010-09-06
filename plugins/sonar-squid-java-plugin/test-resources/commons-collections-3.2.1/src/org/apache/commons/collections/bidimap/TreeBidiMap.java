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
package org.apache.commons.collections.bidimap;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.OrderedBidiMap;
import org.apache.commons.collections.OrderedIterator;
import org.apache.commons.collections.OrderedMapIterator;
import org.apache.commons.collections.iterators.EmptyOrderedMapIterator;
import org.apache.commons.collections.keyvalue.UnmodifiableMapEntry;

/**
 * Red-Black tree-based implementation of BidiMap where all objects added
 * implement the <code>Comparable</code> interface.
 * <p>
 * This class guarantees that the map will be in both ascending key order
 * and ascending value order, sorted according to the natural order for
 * the key's and value's classes.
 * <p>
 * This Map is intended for applications that need to be able to look
 * up a key-value pairing by either key or value, and need to do so
 * with equal efficiency.
 * <p>
 * While that goal could be accomplished by taking a pair of TreeMaps
 * and redirecting requests to the appropriate TreeMap (e.g.,
 * containsKey would be directed to the TreeMap that maps values to
 * keys, containsValue would be directed to the TreeMap that maps keys
 * to values), there are problems with that implementation.
 * If the data contained in the TreeMaps is large, the cost of redundant
 * storage becomes significant. The {@link DualTreeBidiMap} and
 * {@link DualHashBidiMap} implementations use this approach.
 * <p>
 * This solution keeps minimizes the data storage by holding data only once.
 * The red-black algorithm is based on java util TreeMap, but has been modified
 * to simultaneously map a tree node by key and by value. This doubles the
 * cost of put operations (but so does using two TreeMaps), and nearly doubles
 * the cost of remove operations (there is a savings in that the lookup of the
 * node to be removed only has to be performed once). And since only one node
 * contains the key and value, storage is significantly less than that
 * required by two TreeMaps.
 * <p>
 * The Map.Entry instances returned by the appropriate methods will
 * not allow setValue() and will throw an
 * UnsupportedOperationException on attempts to call that method.
 *
 * @since Commons Collections 3.0 (previously DoubleOrderedMap v2.0)
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Marc Johnson
 * @author Stephen Colebourne
 */
public class TreeBidiMap implements OrderedBidiMap {

    private static final int KEY = 0;
    private static final int VALUE = 1;
    private static final int MAPENTRY = 2;
    private static final int INVERSEMAPENTRY = 3;
    private static final int SUM_OF_INDICES = KEY + VALUE;
    private static final int FIRST_INDEX = 0;
    private static final int NUMBER_OF_INDICES = 2;
    private static final String[] dataName = new String[] { "key", "value" };
    
    private Node[] rootNode = new Node[2];
    private int nodeCount = 0;
    private int modifications = 0;
    private Set keySet;
    private Set valuesSet;
    private Set entrySet;
    private TreeBidiMap.Inverse inverse = null;

    //-----------------------------------------------------------------------
    /**
     * Constructs a new empty TreeBidiMap.
     */
    public TreeBidiMap() {
        super();
    }

    /**
     * Constructs a new TreeBidiMap by copying an existing Map.
     *
     * @param map  the map to copy
     * @throws ClassCastException if the keys/values in the map are
     *  not Comparable or are not mutually comparable
     * @throws NullPointerException if any key or value in the map is null
     */
    public TreeBidiMap(final Map map) {
        super();
        putAll(map);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return nodeCount;
    }

    /**
     * Checks whether the map is empty or not.
     *
     * @return true if the map is empty
     */
    public boolean isEmpty() {
        return (nodeCount == 0);
    }

    /**
     * Checks whether this map contains the a mapping for the specified key.
     * <p>
     * The key must implement <code>Comparable</code>.
     *
     * @param key  key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    public boolean containsKey(final Object key) {
        checkKey(key);
        return (lookup((Comparable) key, KEY) != null);
    }

    /**
     * Checks whether this map contains the a mapping for the specified value.
     * <p>
     * The value must implement <code>Comparable</code>.
     *
     * @param value  value whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified value
     * @throws ClassCastException if the value is of an inappropriate type
     * @throws NullPointerException if the value is null
     */
    public boolean containsValue(final Object value) {
        checkValue(value);
        return (lookup((Comparable) value, VALUE) != null);
    }

    /**
     * Gets the value to which this map maps the specified key.
     * Returns null if the map contains no mapping for this key.
     * <p>
     * The key must implement <code>Comparable</code>.
     *
     * @param key  key whose associated value is to be returned
     * @return the value to which this map maps the specified key,
     *  or null if the map contains no mapping for this key
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    public Object get(final Object key) {
        return doGet((Comparable) key, KEY);
    }

    /**
     * Puts the key-value pair into the map, replacing any previous pair.
     * <p>
     * When adding a key-value pair, the value may already exist in the map
     * against a different key. That mapping is removed, to ensure that the
     * value only occurs once in the inverse map.
     * <pre>
     *  BidiMap map1 = new TreeBidiMap();
     *  map.put("A","B");  // contains A mapped to B, as per Map
     *  map.put("A","C");  // contains A mapped to C, as per Map
     * 
     *  BidiMap map2 = new TreeBidiMap();
     *  map.put("A","B");  // contains A mapped to B, as per Map
     *  map.put("C","B");  // contains C mapped to B, key A is removed
     * </pre>
     * <p>
     * Both key and value must implement <code>Comparable</code>.
     *
     * @param key  key with which the specified value is to be  associated
     * @param value  value to be associated with the specified key
     * @return the previous value for the key
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    public Object put(final Object key, final Object value) {
        return doPut((Comparable) key, (Comparable) value, KEY);
    }

    /**
     * Puts all the mappings from the specified map into this map.
     * <p>
     * All keys and values must implement <code>Comparable</code>.
     * 
     * @param map  the map to copy from
     */
    public void putAll(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }
        
    /**
     * Removes the mapping for this key from this map if present.
     * <p>
     * The key must implement <code>Comparable</code>.
     *
     * @param key  key whose mapping is to be removed from the map.
     * @return previous value associated with specified key,
     *  or null if there was no mapping for key.
     * @throws ClassCastException if the key is of an inappropriate type
     * @throws NullPointerException if the key is null
     */
    public Object remove(final Object key) {
        return doRemove((Comparable) key, KEY);
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        modify();

        nodeCount = 0;
        rootNode[KEY] = null;
        rootNode[VALUE] = null;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the key to which this map maps the specified value.
     * Returns null if the map contains no mapping for this value.
     * <p>
     * The value must implement <code>Comparable</code>.
     *
     * @param value  value whose associated key is to be returned.
     * @return the key to which this map maps the specified value,
     *  or null if the map contains no mapping for this value.
     * @throws ClassCastException if the value is of an inappropriate type
     * @throws NullPointerException if the value is null
     */
    public Object getKey(final Object value) {
        return doGet((Comparable) value, VALUE);
    }

    /**
     * Removes the mapping for this value from this map if present.
     * <p>
     * The value must implement <code>Comparable</code>.
     *
     * @param value  value whose mapping is to be removed from the map
     * @return previous key associated with specified value,
     *  or null if there was no mapping for value.
     * @throws ClassCastException if the value is of an inappropriate type
     * @throws NullPointerException if the value is null
     */
    public Object removeValue(final Object value) {
        return doRemove((Comparable) value, VALUE);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the first (lowest) key currently in this map.
     *
     * @return the first (lowest) key currently in this sorted map
     * @throws NoSuchElementException if this map is empty
     */
    public Object firstKey() {
        if (nodeCount == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return leastNode(rootNode[KEY], KEY).getKey();
    }

    /**
     * Gets the last (highest) key currently in this map.
     *
     * @return the last (highest) key currently in this sorted map
     * @throws NoSuchElementException if this map is empty
     */
    public Object lastKey() {
        if (nodeCount == 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return greatestNode(rootNode[KEY], KEY).getKey();
    }
    
    /**
     * Gets the next key after the one specified.
     * <p>
     * The key must implement <code>Comparable</code>.
     *
     * @param key the key to search for next from
     * @return the next key, null if no match or at end
     */
    public Object nextKey(Object key) {
        checkKey(key);
        Node node = nextGreater(lookup((Comparable) key, KEY), KEY);
        return (node == null ? null : node.getKey());
    }

    /**
     * Gets the previous key before the one specified.
     * <p>
     * The key must implement <code>Comparable</code>.
     *
     * @param key the key to search for previous from
     * @return the previous key, null if no match or at start
     */
    public Object previousKey(Object key) {
        checkKey(key);
        Node node = nextSmaller(lookup((Comparable) key, KEY), KEY);
        return (node == null ? null : node.getKey());
    }
    
    //-----------------------------------------------------------------------
    /**
     * Returns a set view of the keys contained in this map in key order.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an iteration over
     * the set is in progress, the results of the iteration are undefined.
     * <p>
     * The set supports element removal, which removes the corresponding mapping
     * from the map. It does not support the add or addAll operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySet() {
        if (keySet == null) {
            keySet = new View(this, KEY, KEY);
        }
        return keySet;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a set view of the values contained in this map in key order.
     * The returned object can be cast to a Set.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an iteration over
     * the set is in progress, the results of the iteration are undefined.
     * <p>
     * The set supports element removal, which removes the corresponding mapping
     * from the map. It does not support the add or addAll operations.
     *
     * @return a set view of the values contained in this map.
     */
    public Collection values() {
        if (valuesSet == null) {
            valuesSet = new View(this, KEY, VALUE);
        }
        return valuesSet;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a set view of the entries contained in this map in key order.
     * For simple iteration through the map, the MapIterator is quicker.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an iteration over
     * the set is in progress, the results of the iteration are undefined.
     * <p>
     * The set supports element removal, which removes the corresponding mapping
     * from the map. It does not support the add or addAll operations.
     * The returned MapEntry objects do not support setValue.
     *
     * @return a set view of the values contained in this map.
     */
    public Set entrySet() {
        if (entrySet == null) {
            return new EntryView(this, KEY, MAPENTRY);
        }
        return entrySet;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets an iterator over the map entries.
     * <p>
     * For this map, this iterator is the fastest way to iterate over the entries.
     * 
     * @return an iterator
     */
    public MapIterator mapIterator() {
        if (isEmpty()) {
            return EmptyOrderedMapIterator.INSTANCE;
        }
        return new ViewMapIterator(this, KEY);
    }

    /**
     * Gets an ordered iterator over the map entries.
     * <p>
     * This iterator allows both forward and reverse iteration over the entries.
     * 
     * @return an iterator
     */
    public OrderedMapIterator orderedMapIterator() {
        if (isEmpty()) {
            return EmptyOrderedMapIterator.INSTANCE;
        }
        return new ViewMapIterator(this, KEY);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the inverse map for comparison.
     * 
     * @return the inverse map
     */
    public BidiMap inverseBidiMap() {
        return inverseOrderedBidiMap();
    }

    /**
     * Gets the inverse map for comparison.
     * 
     * @return the inverse map
     */
    public OrderedBidiMap inverseOrderedBidiMap() {
        if (inverse == null) {
            inverse = new Inverse(this);
        }
        return inverse;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares for equals as per the API.
     *
     * @param obj  the object to compare to
     * @return true if equal
     */
    public boolean equals(Object obj) {
        return this.doEquals(obj, KEY);
    }
    
    /**
     * Gets the hash code value for this map as per the API.
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        return this.doHashCode(KEY);
    }
    
    /**
     * Returns a string version of this Map in standard format.
     * 
     * @return a standard format string version of the map
     */
    public String toString() {
        return this.doToString(KEY);
    }
    
    //-----------------------------------------------------------------------
    /**
     * Common get logic, used to get by key or get by value
     *
     * @param obj  the key or value that we're looking for
     * @param index  the KEY or VALUE int
     * @return the key (if the value was mapped) or the value (if the
     *         key was mapped); null if we couldn't find the specified
     *         object
     */
    private Object doGet(final Comparable obj, final int index) {
        checkNonNullComparable(obj, index);
        Node node = lookup(obj, index);
        return ((node == null) ? null : node.getData(oppositeIndex(index)));
    }

    /**
     * Common put logic, differing only in the return value.
     * 
     * @param key  the key, always the main map key
     * @param value  the value, always the main map value
     * @param index  the KEY or VALUE int, for the return value only
     * @return the previously mapped value
     */
    private Object doPut(final Comparable key, final Comparable value, final int index) {
        checkKeyAndValue(key, value);
        
        // store previous and remove previous mappings
        Object prev = (index == KEY ? doGet(key, KEY) :  doGet(value, VALUE));
        doRemove(key, KEY);
        doRemove(value, VALUE);
        
        Node node = rootNode[KEY];
        if (node == null) {
            // map is empty
            Node root = new Node(key, value);
            rootNode[KEY] = root;
            rootNode[VALUE] = root;
            grow();
            
        } else {
            // add new mapping
            while (true) {
                int cmp = compare(key, node.getData(KEY));
        
                if (cmp == 0) {
                    // shouldn't happen
                    throw new IllegalArgumentException("Cannot store a duplicate key (\"" + key + "\") in this Map");
                } else if (cmp < 0) {
                    if (node.getLeft(KEY) != null) {
                        node = node.getLeft(KEY);
                    } else {
                        Node newNode = new Node(key, value);
        
                        insertValue(newNode);
                        node.setLeft(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();
        
                        break;
                    }
                } else { // cmp > 0
                    if (node.getRight(KEY) != null) {
                        node = node.getRight(KEY);
                    } else {
                        Node newNode = new Node(key, value);
        
                        insertValue(newNode);
                        node.setRight(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();
        
                        break;
                    }
                }
            }
        }
        return prev;
    }

    /**
     * Remove by object (remove by key or remove by value)
     *
     * @param o the key, or value, that we're looking for
     * @param index  the KEY or VALUE int
     *
     * @return the key, if remove by value, or the value, if remove by
     *         key. null if the specified key or value could not be
     *         found
     */
    private Object doRemove(final Comparable o, final int index) {
        Node node = lookup(o, index);
        Object rval = null;
        if (node != null) {
            rval = node.getData(oppositeIndex(index));
            doRedBlackDelete(node);
        }
        return rval;
    }

    /**
     * do the actual lookup of a piece of data
     *
     * @param data the key or value to be looked up
     * @param index  the KEY or VALUE int
     * @return the desired Node, or null if there is no mapping of the
     *         specified data
     */
    private Node lookup(final Comparable data, final int index) {
        Node rval = null;
        Node node = rootNode[index];

        while (node != null) {
            int cmp = compare(data, node.getData(index));
            if (cmp == 0) {
                rval = node;
                break;
            } else {
                node = (cmp < 0) ? node.getLeft(index) : node.getRight(index);
            }
        }

        return rval;
    }

    /**
     * get the next larger node from the specified node
     *
     * @param node the node to be searched from
     * @param index  the KEY or VALUE int
     * @return the specified node
     */
    private Node nextGreater(final Node node, final int index) {
        Node rval = null;
        if (node == null) {
            rval = null;
        } else if (node.getRight(index) != null) {
            // everything to the node's right is larger. The least of
            // the right node's descendants is the next larger node
            rval = leastNode(node.getRight(index), index);
        } else {
            // traverse up our ancestry until we find an ancestor that
            // is null or one whose left child is our ancestor. If we
            // find a null, then this node IS the largest node in the
            // tree, and there is no greater node. Otherwise, we are
            // the largest node in the subtree on that ancestor's left
            // ... and that ancestor is the next greatest node
            Node parent = node.getParent(index);
            Node child = node;

            while ((parent != null) && (child == parent.getRight(index))) {
                child = parent;
                parent = parent.getParent(index);
            }
            rval = parent;
        }
        return rval;
    }

    /**
     * get the next larger node from the specified node
     *
     * @param node the node to be searched from
     * @param index  the KEY or VALUE int
     * @return the specified node
     */
    private Node nextSmaller(final Node node, final int index) {
        Node rval = null;
        if (node == null) {
            rval = null;
        } else if (node.getLeft(index) != null) {
            // everything to the node's left is smaller. The greatest of
            // the left node's descendants is the next smaller node
            rval = greatestNode(node.getLeft(index), index);
        } else {
            // traverse up our ancestry until we find an ancestor that
            // is null or one whose right child is our ancestor. If we
            // find a null, then this node IS the largest node in the
            // tree, and there is no greater node. Otherwise, we are
            // the largest node in the subtree on that ancestor's right
            // ... and that ancestor is the next greatest node
            Node parent = node.getParent(index);
            Node child = node;

            while ((parent != null) && (child == parent.getLeft(index))) {
                child = parent;
                parent = parent.getParent(index);
            }
            rval = parent;
        }
        return rval;
    }

    //-----------------------------------------------------------------------
    /**
     * Get the opposite index of the specified index
     *
     * @param index  the KEY or VALUE int
     * @return VALUE (if KEY was specified), else KEY
     */
    private static int oppositeIndex(final int index) {
        // old trick ... to find the opposite of a value, m or n,
        // subtract the value from the sum of the two possible
        // values. (m + n) - m = n; (m + n) - n = m
        return SUM_OF_INDICES - index;
    }

    /**
     * Compare two objects
     *
     * @param o1  the first object
     * @param o2  the second object
     *
     * @return negative value if o1 &lt; o2; 0 if o1 == o2; positive
     *         value if o1 &gt; o2
     */
    private static int compare(final Comparable o1, final Comparable o2) {
        return o1.compareTo(o2);
    }

    /**
     * Find the least node from a given node.
     *
     * @param node  the node from which we will start searching
     * @param index  the KEY or VALUE int
     * @return the smallest node, from the specified node, in the
     *         specified mapping
     */
    private static Node leastNode(final Node node, final int index) {
        Node rval = node;
        if (rval != null) {
            while (rval.getLeft(index) != null) {
                rval = rval.getLeft(index);
            }
        }
        return rval;
    }

    /**
     * Find the greatest node from a given node.
     *
     * @param node  the node from which we will start searching
     * @param index  the KEY or VALUE int
     * @return the greatest node, from the specified node
     */
    private static Node greatestNode(final Node node, final int index) {
        Node rval = node;
        if (rval != null) {
            while (rval.getRight(index) != null) {
                rval = rval.getRight(index);
            }
        }
        return rval;
    }

    /**
     * copy the color from one node to another, dealing with the fact
     * that one or both nodes may, in fact, be null
     *
     * @param from the node whose color we're copying; may be null
     * @param to the node whose color we're changing; may be null
     * @param index  the KEY or VALUE int
     */
    private static void copyColor(final Node from, final Node to, final int index) {
        if (to != null) {
            if (from == null) {
                // by default, make it black
                to.setBlack(index);
            } else {
                to.copyColor(from, index);
            }
        }
    }

    /**
     * is the specified node red? if the node does not exist, no, it's
     * black, thank you
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static boolean isRed(final Node node, final int index) {
        return ((node == null) ? false : node.isRed(index));
    }

    /**
     * is the specified black red? if the node does not exist, sure,
     * it's black, thank you
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static boolean isBlack(final Node node, final int index) {
        return ((node == null) ? true : node.isBlack(index));
    }

    /**
     * force a node (if it exists) red
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static void makeRed(final Node node, final int index) {
        if (node != null) {
            node.setRed(index);
        }
    }

    /**
     * force a node (if it exists) black
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static void makeBlack(final Node node, final int index) {
        if (node != null) {
            node.setBlack(index);
        }
    }

    /**
     * get a node's grandparent. mind you, the node, its parent, or
     * its grandparent may not exist. no problem
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static Node getGrandParent(final Node node, final int index) {
        return getParent(getParent(node, index), index);
    }

    /**
     * get a node's parent. mind you, the node, or its parent, may not
     * exist. no problem
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static Node getParent(final Node node, final int index) {
        return ((node == null) ? null : node.getParent(index));
    }

    /**
     * get a node's right child. mind you, the node may not exist. no
     * problem
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static Node getRightChild(final Node node, final int index) {
        return (node == null) ? null : node.getRight(index);
    }

    /**
     * get a node's left child. mind you, the node may not exist. no
     * problem
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static Node getLeftChild(final Node node, final int index) {
        return (node == null) ? null : node.getLeft(index);
    }

    /**
     * is this node its parent's left child? mind you, the node, or
     * its parent, may not exist. no problem. if the node doesn't
     * exist ... it's its non-existent parent's left child. If the
     * node does exist but has no parent ... no, we're not the
     * non-existent parent's left child. Otherwise (both the specified
     * node AND its parent exist), check.
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static boolean isLeftChild(final Node node, final int index) {
        return (node == null)
            ? true
            : ((node.getParent(index) == null) ?
                false : (node == node.getParent(index).getLeft(index)));
    }

    /**
     * is this node its parent's right child? mind you, the node, or
     * its parent, may not exist. no problem. if the node doesn't
     * exist ... it's its non-existent parent's right child. If the
     * node does exist but has no parent ... no, we're not the
     * non-existent parent's right child. Otherwise (both the
     * specified node AND its parent exist), check.
     *
     * @param node the node (may be null) in question
     * @param index  the KEY or VALUE int
     */
    private static boolean isRightChild(final Node node, final int index) {
        return (node == null)
            ? true
            : ((node.getParent(index) == null) ? 
                false : (node == node.getParent(index).getRight(index)));
    }

    /**
     * do a rotate left. standard fare in the world of balanced trees
     *
     * @param node the node to be rotated
     * @param index  the KEY or VALUE int
     */
    private void rotateLeft(final Node node, final int index) {
        Node rightChild = node.getRight(index);
        node.setRight(rightChild.getLeft(index), index);

        if (rightChild.getLeft(index) != null) {
            rightChild.getLeft(index).setParent(node, index);
        }
        rightChild.setParent(node.getParent(index), index);
        
        if (node.getParent(index) == null) {
            // node was the root ... now its right child is the root
            rootNode[index] = rightChild;
        } else if (node.getParent(index).getLeft(index) == node) {
            node.getParent(index).setLeft(rightChild, index);
        } else {
            node.getParent(index).setRight(rightChild, index);
        }

        rightChild.setLeft(node, index);
        node.setParent(rightChild, index);
    }

    /**
     * do a rotate right. standard fare in the world of balanced trees
     *
     * @param node the node to be rotated
     * @param index  the KEY or VALUE int
     */
    private void rotateRight(final Node node, final int index) {
        Node leftChild = node.getLeft(index);
        node.setLeft(leftChild.getRight(index), index);
        if (leftChild.getRight(index) != null) {
            leftChild.getRight(index).setParent(node, index);
        }
        leftChild.setParent(node.getParent(index), index);

        if (node.getParent(index) == null) {
            // node was the root ... now its left child is the root
            rootNode[index] = leftChild;
        } else if (node.getParent(index).getRight(index) == node) {
            node.getParent(index).setRight(leftChild, index);
        } else {
            node.getParent(index).setLeft(leftChild, index);
        }

        leftChild.setRight(node, index);
        node.setParent(leftChild, index);
    }

    /**
     * complicated red-black insert stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable any more
     *
     * @param insertedNode the node to be inserted
     * @param index  the KEY or VALUE int
     */
    private void doRedBlackInsert(final Node insertedNode, final int index) {
        Node currentNode = insertedNode;
        makeRed(currentNode, index);

        while ((currentNode != null)
            && (currentNode != rootNode[index])
            && (isRed(currentNode.getParent(index), index))) {
            if (isLeftChild(getParent(currentNode, index), index)) {
                Node y = getRightChild(getGrandParent(currentNode, index), index);

                if (isRed(y, index)) {
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(y, index);
                    makeRed(getGrandParent(currentNode, index), index);

                    currentNode = getGrandParent(currentNode, index);
                } else {
                    if (isRightChild(currentNode, index)) {
                        currentNode = getParent(currentNode, index);

                        rotateLeft(currentNode, index);
                    }

                    makeBlack(getParent(currentNode, index), index);
                    makeRed(getGrandParent(currentNode, index), index);

                    if (getGrandParent(currentNode, index) != null) {
                        rotateRight(getGrandParent(currentNode, index), index);
                    }
                }
            } else {

                // just like clause above, except swap left for right
                Node y = getLeftChild(getGrandParent(currentNode, index), index);

                if (isRed(y, index)) {
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(y, index);
                    makeRed(getGrandParent(currentNode, index), index);

                    currentNode = getGrandParent(currentNode, index);
                } else {
                    if (isLeftChild(currentNode, index)) {
                        currentNode = getParent(currentNode, index);

                        rotateRight(currentNode, index);
                    }

                    makeBlack(getParent(currentNode, index), index);
                    makeRed(getGrandParent(currentNode, index), index);

                    if (getGrandParent(currentNode, index) != null) {
                        rotateLeft(getGrandParent(currentNode, index), index);
                    }
                }
            }
        }

        makeBlack(rootNode[index], index);
    }

    /**
     * complicated red-black delete stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable any more
     *
     * @param deletedNode the node to be deleted
     */
    private void doRedBlackDelete(final Node deletedNode) {
        for (int index = FIRST_INDEX; index < NUMBER_OF_INDICES; index++) {
            // if deleted node has both left and children, swap with
            // the next greater node
            if ((deletedNode.getLeft(index) != null) && (deletedNode.getRight(index) != null)) {
                swapPosition(nextGreater(deletedNode, index), deletedNode, index);
            }

            Node replacement =
                ((deletedNode.getLeft(index) != null) ? deletedNode.getLeft(index) : deletedNode.getRight(index));

            if (replacement != null) {
                replacement.setParent(deletedNode.getParent(index), index);

                if (deletedNode.getParent(index) == null) {
                    rootNode[index] = replacement;
                } else if (deletedNode == deletedNode.getParent(index).getLeft(index)) {
                    deletedNode.getParent(index).setLeft(replacement, index);
                } else {
                    deletedNode.getParent(index).setRight(replacement, index);
                }

                deletedNode.setLeft(null, index);
                deletedNode.setRight(null, index);
                deletedNode.setParent(null, index);

                if (isBlack(deletedNode, index)) {
                    doRedBlackDeleteFixup(replacement, index);
                }
            } else {

                // replacement is null
                if (deletedNode.getParent(index) == null) {

                    // empty tree
                    rootNode[index] = null;
                } else {

                    // deleted node had no children
                    if (isBlack(deletedNode, index)) {
                        doRedBlackDeleteFixup(deletedNode, index);
                    }

                    if (deletedNode.getParent(index) != null) {
                        if (deletedNode == deletedNode.getParent(index).getLeft(index)) {
                            deletedNode.getParent(index).setLeft(null, index);
                        } else {
                            deletedNode.getParent(index).setRight(null, index);
                        }

                        deletedNode.setParent(null, index);
                    }
                }
            }
        }
        shrink();
    }

    /**
     * complicated red-black delete stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable any more. This
     * rebalances the tree (somewhat, as red-black trees are not
     * perfectly balanced -- perfect balancing takes longer)
     *
     * @param replacementNode the node being replaced
     * @param index  the KEY or VALUE int
     */
    private void doRedBlackDeleteFixup(final Node replacementNode, final int index) {
        Node currentNode = replacementNode;

        while ((currentNode != rootNode[index]) && (isBlack(currentNode, index))) {
            if (isLeftChild(currentNode, index)) {
                Node siblingNode = getRightChild(getParent(currentNode, index), index);

                if (isRed(siblingNode, index)) {
                    makeBlack(siblingNode, index);
                    makeRed(getParent(currentNode, index), index);
                    rotateLeft(getParent(currentNode, index), index);

                    siblingNode = getRightChild(getParent(currentNode, index), index);
                }

                if (isBlack(getLeftChild(siblingNode, index), index)
                    && isBlack(getRightChild(siblingNode, index), index)) {
                    makeRed(siblingNode, index);

                    currentNode = getParent(currentNode, index);
                } else {
                    if (isBlack(getRightChild(siblingNode, index), index)) {
                        makeBlack(getLeftChild(siblingNode, index), index);
                        makeRed(siblingNode, index);
                        rotateRight(siblingNode, index);

                        siblingNode = getRightChild(getParent(currentNode, index), index);
                    }

                    copyColor(getParent(currentNode, index), siblingNode, index);
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(getRightChild(siblingNode, index), index);
                    rotateLeft(getParent(currentNode, index), index);

                    currentNode = rootNode[index];
                }
            } else {
                Node siblingNode = getLeftChild(getParent(currentNode, index), index);

                if (isRed(siblingNode, index)) {
                    makeBlack(siblingNode, index);
                    makeRed(getParent(currentNode, index), index);
                    rotateRight(getParent(currentNode, index), index);

                    siblingNode = getLeftChild(getParent(currentNode, index), index);
                }

                if (isBlack(getRightChild(siblingNode, index), index)
                    && isBlack(getLeftChild(siblingNode, index), index)) {
                    makeRed(siblingNode, index);

                    currentNode = getParent(currentNode, index);
                } else {
                    if (isBlack(getLeftChild(siblingNode, index), index)) {
                        makeBlack(getRightChild(siblingNode, index), index);
                        makeRed(siblingNode, index);
                        rotateLeft(siblingNode, index);

                        siblingNode = getLeftChild(getParent(currentNode, index), index);
                    }

                    copyColor(getParent(currentNode, index), siblingNode, index);
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(getLeftChild(siblingNode, index), index);
                    rotateRight(getParent(currentNode, index), index);

                    currentNode = rootNode[index];
                }
            }
        }

        makeBlack(currentNode, index);
    }

    /**
     * swap two nodes (except for their content), taking care of
     * special cases where one is the other's parent ... hey, it
     * happens.
     *
     * @param x one node
     * @param y another node
     * @param index  the KEY or VALUE int
     */
    private void swapPosition(final Node x, final Node y, final int index) {
        // Save initial values.
        Node xFormerParent = x.getParent(index);
        Node xFormerLeftChild = x.getLeft(index);
        Node xFormerRightChild = x.getRight(index);
        Node yFormerParent = y.getParent(index);
        Node yFormerLeftChild = y.getLeft(index);
        Node yFormerRightChild = y.getRight(index);
        boolean xWasLeftChild = (x.getParent(index) != null) && (x == x.getParent(index).getLeft(index));
        boolean yWasLeftChild = (y.getParent(index) != null) && (y == y.getParent(index).getLeft(index));

        // Swap, handling special cases of one being the other's parent.
        if (x == yFormerParent) { // x was y's parent
            x.setParent(y, index);

            if (yWasLeftChild) {
                y.setLeft(x, index);
                y.setRight(xFormerRightChild, index);
            } else {
                y.setRight(x, index);
                y.setLeft(xFormerLeftChild, index);
            }
        } else {
            x.setParent(yFormerParent, index);

            if (yFormerParent != null) {
                if (yWasLeftChild) {
                    yFormerParent.setLeft(x, index);
                } else {
                    yFormerParent.setRight(x, index);
                }
            }

            y.setLeft(xFormerLeftChild, index);
            y.setRight(xFormerRightChild, index);
        }

        if (y == xFormerParent) { // y was x's parent
            y.setParent(x, index);

            if (xWasLeftChild) {
                x.setLeft(y, index);
                x.setRight(yFormerRightChild, index);
            } else {
                x.setRight(y, index);
                x.setLeft(yFormerLeftChild, index);
            }
        } else {
            y.setParent(xFormerParent, index);

            if (xFormerParent != null) {
                if (xWasLeftChild) {
                    xFormerParent.setLeft(y, index);
                } else {
                    xFormerParent.setRight(y, index);
                }
            }

            x.setLeft(yFormerLeftChild, index);
            x.setRight(yFormerRightChild, index);
        }

        // Fix children's parent pointers
        if (x.getLeft(index) != null) {
            x.getLeft(index).setParent(x, index);
        }

        if (x.getRight(index) != null) {
            x.getRight(index).setParent(x, index);
        }

        if (y.getLeft(index) != null) {
            y.getLeft(index).setParent(y, index);
        }

        if (y.getRight(index) != null) {
            y.getRight(index).setParent(y, index);
        }

        x.swapColors(y, index);

        // Check if root changed
        if (rootNode[index] == x) {
            rootNode[index] = y;
        } else if (rootNode[index] == y) {
            rootNode[index] = x;
        }
    }

    /**
     * check if an object is fit to be proper input ... has to be
     * Comparable and non-null
     *
     * @param o the object being checked
     * @param index  the KEY or VALUE int (used to put the right word in the
     *              exception message)
     *
     * @throws NullPointerException if o is null
     * @throws ClassCastException if o is not Comparable
     */
    private static void checkNonNullComparable(final Object o, final int index) {
        if (o == null) {
            throw new NullPointerException(dataName[index] + " cannot be null");
        }
        if (!(o instanceof Comparable)) {
            throw new ClassCastException(dataName[index] + " must be Comparable");
        }
    }

    /**
     * check a key for validity (non-null and implements Comparable)
     *
     * @param key the key to be checked
     *
     * @throws NullPointerException if key is null
     * @throws ClassCastException if key is not Comparable
     */
    private static void checkKey(final Object key) {
        checkNonNullComparable(key, KEY);
    }

    /**
     * check a value for validity (non-null and implements Comparable)
     *
     * @param value the value to be checked
     *
     * @throws NullPointerException if value is null
     * @throws ClassCastException if value is not Comparable
     */
    private static void checkValue(final Object value) {
        checkNonNullComparable(value, VALUE);
    }

    /**
     * check a key and a value for validity (non-null and implements
     * Comparable)
     *
     * @param key the key to be checked
     * @param value the value to be checked
     *
     * @throws NullPointerException if key or value is null
     * @throws ClassCastException if key or value is not Comparable
     */
    private static void checkKeyAndValue(final Object key, final Object value) {
        checkKey(key);
        checkValue(value);
    }

    /**
     * increment the modification count -- used to check for
     * concurrent modification of the map through the map and through
     * an Iterator from one of its Set or Collection views
     */
    private void modify() {
        modifications++;
    }

    /**
     * bump up the size and note that the map has changed
     */
    private void grow() {
        modify();
        nodeCount++;
    }

    /**
     * decrement the size and note that the map has changed
     */
    private void shrink() {
        modify();
        nodeCount--;
    }

    /**
     * insert a node by its value
     *
     * @param newNode the node to be inserted
     *
     * @throws IllegalArgumentException if the node already exists
     *                                     in the value mapping
     */
    private void insertValue(final Node newNode) throws IllegalArgumentException {
        Node node = rootNode[VALUE];

        while (true) {
            int cmp = compare(newNode.getData(VALUE), node.getData(VALUE));

            if (cmp == 0) {
                throw new IllegalArgumentException(
                    "Cannot store a duplicate value (\"" + newNode.getData(VALUE) + "\") in this Map");
            } else if (cmp < 0) {
                if (node.getLeft(VALUE) != null) {
                    node = node.getLeft(VALUE);
                } else {
                    node.setLeft(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
            } else { // cmp > 0
                if (node.getRight(VALUE) != null) {
                    node = node.getRight(VALUE);
                } else {
                    node.setRight(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
            }
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * Compares for equals as per the API.
     *
     * @param obj  the object to compare to
     * @param type  the KEY or VALUE int
     * @return true if equal
     */
    private boolean doEquals(Object obj, final int type) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Map == false) {
            return false;
        }
        Map other = (Map) obj;
        if (other.size() != size()) {
            return false;
        }

        if (nodeCount > 0) {
            try {
                for (MapIterator it = new ViewMapIterator(this, type); it.hasNext(); ) {
                    Object key = it.next();
                    Object value = it.getValue();
                    if (value.equals(other.get(key)) == false) {
                        return false;
                    }
                }
            } catch (ClassCastException ex) {
                return false;
            } catch (NullPointerException ex) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the hash code value for this map as per the API.
     *
     * @param type  the KEY or VALUE int
     * @return the hash code value for this map
     */
    private int doHashCode(final int type) {
        int total = 0;
        if (nodeCount > 0) {
            for (MapIterator it = new ViewMapIterator(this, type); it.hasNext(); ) {
                Object key = it.next();
                Object value = it.getValue();
                total += (key.hashCode() ^ value.hashCode());
            }
        }
        return total;
    }
    
    /**
     * Gets the string form of this map as per AbstractMap.
     *
     * @param type  the KEY or VALUE int
     * @return the string form of this map
     */
    private String doToString(final int type) {
        if (nodeCount == 0) {
            return "{}";
        }
        StringBuffer buf = new StringBuffer(nodeCount * 32);
        buf.append('{');
        MapIterator it = new ViewMapIterator(this, type);
        boolean hasNext = it.hasNext();
        while (hasNext) {
            Object key = it.next();
            Object value = it.getValue();
            buf.append(key == this ? "(this Map)" : key)
               .append('=')
               .append(value == this ? "(this Map)" : value);

            hasNext = it.hasNext();
            if (hasNext) {
                buf.append(", ");
            }
        }

        buf.append('}');
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * A view of this map.
     */
    static class View extends AbstractSet {
        
        /** The parent map. */
        protected final TreeBidiMap main;
        /** Whether to return KEY or VALUE order. */
        protected final int orderType;
        /** Whether to return KEY, VALUE, MAPENTRY or INVERSEMAPENTRY data. */
        protected final int dataType;

        /**
         * Constructor.
         *
         * @param main  the main map
         * @param orderType  the KEY or VALUE int for the order
         * @param dataType  the KEY, VALUE, MAPENTRY or INVERSEMAPENTRY int
         */
        View(final TreeBidiMap main, final int orderType, final int dataType) {
            super();
            this.main = main;
            this.orderType = orderType;
            this.dataType = dataType;
        }
        
        public Iterator iterator() {
            return new ViewIterator(main, orderType, dataType);
        }

        public int size() {
            return main.size();
        }

        public boolean contains(final Object obj) {
            checkNonNullComparable(obj, dataType);
            return (main.lookup((Comparable) obj, dataType) != null);
        }

        public boolean remove(final Object obj) {
            return (main.doRemove((Comparable) obj, dataType) != null);
        }

        public void clear() {
            main.clear();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * An iterator over the map.
     */
    static class ViewIterator implements OrderedIterator {

        /** The parent map. */
        protected final TreeBidiMap main;
        /** Whether to return KEY or VALUE order. */
        protected final int orderType;
        /** Whether to return KEY, VALUE, MAPENTRY or INVERSEMAPENTRY data. */
        protected final int dataType;
        /** The last node returned by the iterator. */
        protected Node lastReturnedNode;
        /** The next node to be returned by the iterator. */
        protected Node nextNode;
        /** The previous node in the sequence returned by the iterator. */
        protected Node previousNode;
        /** The modification count. */
        private int expectedModifications;

        /**
         * Constructor.
         *
         * @param main  the main map
         * @param orderType  the KEY or VALUE int for the order
         * @param dataType  the KEY, VALUE, MAPENTRY or INVERSEMAPENTRY int
         */
        ViewIterator(final TreeBidiMap main, final int orderType, final int dataType) {
            super();
            this.main = main;
            this.orderType = orderType;
            this.dataType = dataType;
            expectedModifications = main.modifications;
            nextNode = leastNode(main.rootNode[orderType], orderType);
            lastReturnedNode = null;
            previousNode = null;
        }

        public final boolean hasNext() {
            return (nextNode != null);
        }

        public final Object next() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            if (main.modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }
            lastReturnedNode = nextNode;
            previousNode = nextNode;
            nextNode = main.nextGreater(nextNode, orderType);
            return doGetData();
        }

        public boolean hasPrevious() {
            return (previousNode != null);
        }

        public Object previous() {
            if (previousNode == null) {
                throw new NoSuchElementException();
            }
            if (main.modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }
            nextNode = lastReturnedNode;
            if (nextNode == null) {
                nextNode = main.nextGreater(previousNode, orderType);
            }
            lastReturnedNode = previousNode;
            previousNode = main.nextSmaller(previousNode, orderType);
            return doGetData();
        }

        /**
         * Gets the data value for the lastReturnedNode field.
         * @return the data value
         */
        protected Object doGetData() {
            switch (dataType) {
                case KEY:
                    return lastReturnedNode.getKey();
                case VALUE:
                    return lastReturnedNode.getValue();
                case MAPENTRY:
                    return lastReturnedNode;
                case INVERSEMAPENTRY:
                    return new UnmodifiableMapEntry(lastReturnedNode.getValue(), lastReturnedNode.getKey());
            }
            return null;
        }

        public final void remove() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException();
            }
            if (main.modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }
            main.doRedBlackDelete(lastReturnedNode);
            expectedModifications++;
            lastReturnedNode = null;
            if (nextNode == null) {
                previousNode = TreeBidiMap.greatestNode(main.rootNode[orderType], orderType);
            } else {
                previousNode = main.nextSmaller(nextNode, orderType);
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * An iterator over the map.
     */
    static class ViewMapIterator extends ViewIterator implements OrderedMapIterator {

        private final int oppositeType;
        
        /**
         * Constructor.
         *
         * @param main  the main map
         * @param orderType  the KEY or VALUE int for the order
         */
        ViewMapIterator(final TreeBidiMap main, final int orderType) {
            super(main, orderType, orderType);
            this.oppositeType = oppositeIndex(dataType);
        }
        
        public Object getKey() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException("Iterator getKey() can only be called after next() and before remove()");
            }
            return lastReturnedNode.getData(dataType);
        }

        public Object getValue() {
            if (lastReturnedNode == null) {
                throw new IllegalStateException("Iterator getValue() can only be called after next() and before remove()");
            }
            return lastReturnedNode.getData(oppositeType);
        }

        public Object setValue(final Object obj) {
            throw new UnsupportedOperationException();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * A view of this map.
     */
    static class EntryView extends View {
        
        private final int oppositeType;
        
        /**
         * Constructor.
         *
         * @param main  the main map
         * @param orderType  the KEY or VALUE int for the order
         * @param dataType  the MAPENTRY or INVERSEMAPENTRY int for the returned data
         */
        EntryView(final TreeBidiMap main, final int orderType, final int dataType) {
            super(main, orderType, dataType);
            this.oppositeType = TreeBidiMap.oppositeIndex(orderType);
        }
        
        public boolean contains(Object obj) {
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object value = entry.getValue();
            Node node = main.lookup((Comparable) entry.getKey(), orderType);
            return (node != null && node.getData(oppositeType).equals(value));
        }

        public boolean remove(Object obj) {
            if (obj instanceof Map.Entry == false) {
                return false;
            }
            Map.Entry entry = (Map.Entry) obj;
            Object value = entry.getValue();
            Node node = main.lookup((Comparable) entry.getKey(), orderType);
            if (node != null && node.getData(oppositeType).equals(value)) {
                main.doRedBlackDelete(node);
                return true;
            }
            return false;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * A node used to store the data.
     */
    static class Node implements Map.Entry, KeyValue {

        private Comparable[] data;
        private Node[] leftNode;
        private Node[] rightNode;
        private Node[] parentNode;
        private boolean[] blackColor;
        private int hashcodeValue;
        private boolean calculatedHashCode;

        /**
         * Make a new cell with given key and value, and with null
         * links, and black (true) colors.
         *
         * @param key
         * @param value
         */
        Node(final Comparable key, final Comparable value) {
            super();
            data = new Comparable[] { key, value };
            leftNode = new Node[2];
            rightNode = new Node[2];
            parentNode = new Node[2];
            blackColor = new boolean[] { true, true };
            calculatedHashCode = false;
        }

        /**
         * Get the specified data.
         *
         * @param index  the KEY or VALUE int
         * @return the key or value
         */
        private Comparable getData(final int index) {
            return data[index];
        }

        /**
         * Set this node's left node.
         *
         * @param node  the new left node
         * @param index  the KEY or VALUE int
         */
        private void setLeft(final Node node, final int index) {
            leftNode[index] = node;
        }

        /**
         * Get the left node.
         *
         * @param index  the KEY or VALUE int
         * @return the left node, may be null
         */
        private Node getLeft(final int index) {
            return leftNode[index];
        }

        /**
         * Set this node's right node.
         *
         * @param node  the new right node
         * @param index  the KEY or VALUE int
         */
        private void setRight(final Node node, final int index) {
            rightNode[index] = node;
        }

        /**
         * Get the right node.
         *
         * @param index  the KEY or VALUE int
         * @return the right node, may be null
         */
        private Node getRight(final int index) {
            return rightNode[index];
        }

        /**
         * Set this node's parent node.
         *
         * @param node  the new parent node
         * @param index  the KEY or VALUE int
         */
        private void setParent(final Node node, final int index) {
            parentNode[index] = node;
        }

        /**
         * Get the parent node.
         *
         * @param index  the KEY or VALUE int
         * @return the parent node, may be null
         */
        private Node getParent(final int index) {
            return parentNode[index];
        }

        /**
         * Exchange colors with another node.
         *
         * @param node  the node to swap with
         * @param index  the KEY or VALUE int
         */
        private void swapColors(final Node node, final int index) {
            // Swap colors -- old hacker's trick
            blackColor[index]      ^= node.blackColor[index];
            node.blackColor[index] ^= blackColor[index];
            blackColor[index]      ^= node.blackColor[index];
        }

        /**
         * Is this node black?
         *
         * @param index  the KEY or VALUE int
         * @return true if black (which is represented as a true boolean)
         */
        private boolean isBlack(final int index) {
            return blackColor[index];
        }

        /**
         * Is this node red?
         *
         * @param index  the KEY or VALUE int
         * @return true if non-black
         */
        private boolean isRed(final int index) {
            return !blackColor[index];
        }

        /**
         * Make this node black.
         *
         * @param index  the KEY or VALUE int
         */
        private void setBlack(final int index) {
            blackColor[index] = true;
        }

        /**
         * Make this node red.
         *
         * @param index  the KEY or VALUE int
         */
        private void setRed(final int index) {
            blackColor[index] = false;
        }

        /**
         * Make this node the same color as another
         *
         * @param node  the node whose color we're adopting
         * @param index  the KEY or VALUE int
         */
        private void copyColor(final Node node, final int index) {
            blackColor[index] = node.blackColor[index];
        }

        //-------------------------------------------------------------------
        /**
         * Gets the key.
         * 
         * @return the key corresponding to this entry.
         */
        public Object getKey() {
            return data[KEY];
        }

        /**
         * Gets the value.
         * 
         * @return the value corresponding to this entry.
         */
        public Object getValue() {
            return data[VALUE];
        }

        /**
         * Optional operation that is not permitted in this implementation
         *
         * @param ignored
         * @return does not return
         * @throws UnsupportedOperationException always
         */
        public Object setValue(final Object ignored)
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                "Map.Entry.setValue is not supported");
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns true if the given object is also a map entry and
         * the two entries represent the same mapping.
         *
         * @param obj  the object to be compared for equality with this entry.
         * @return true if the specified object is equal to this entry.
         */
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) obj;
            return data[KEY].equals(e.getKey()) && data[VALUE].equals(e.getValue());
        }

        /**
         * @return the hash code value for this map entry.
         */
        public int hashCode() {
            if (!calculatedHashCode) {
                hashcodeValue = data[KEY].hashCode() ^ data[VALUE].hashCode();
                calculatedHashCode = true;
            }
            return hashcodeValue;
        }
    }
    
    //-----------------------------------------------------------------------
    /**
     * A node used to store the data.
     */
    static class Inverse implements OrderedBidiMap {
        
        /** The parent map. */
        private final TreeBidiMap main;
        /** Store the keySet once created. */
        private Set keySet;
        /** Store the valuesSet once created. */
        private Set valuesSet;
        /** Store the entrySet once created. */
        private Set entrySet;
        
        /**
         * Constructor.
         * @param main  the main map
         */
        Inverse(final TreeBidiMap main) {
            super();
            this.main = main;
        }

        public int size() {
            return main.size();
        }

        public boolean isEmpty() {
            return main.isEmpty();
        }

        public Object get(final Object key) {
            return main.getKey(key);
        }

        public Object getKey(final Object value) {
            return main.get(value);
        }

        public boolean containsKey(final Object key) {
            return main.containsValue(key);
        }

        public boolean containsValue(final Object value) {
            return main.containsKey(value);
        }

        public Object firstKey() {
            if (main.nodeCount == 0) {
                throw new NoSuchElementException("Map is empty");
            }
            return TreeBidiMap.leastNode(main.rootNode[VALUE], VALUE).getValue();
        }

        public Object lastKey() {
            if (main.nodeCount == 0) {
                throw new NoSuchElementException("Map is empty");
            }
            return TreeBidiMap.greatestNode(main.rootNode[VALUE], VALUE).getValue();
        }
    
        public Object nextKey(Object key) {
            checkKey(key);
            Node node = main.nextGreater(main.lookup((Comparable) key, VALUE), VALUE);
            return (node == null ? null : node.getValue());
        }

        public Object previousKey(Object key) {
            checkKey(key);
            Node node = main.nextSmaller(main.lookup((Comparable) key, VALUE), VALUE);
            return (node == null ? null : node.getValue());
        }

        public Object put(final Object key, final Object value) {
            return main.doPut((Comparable) value, (Comparable) key, VALUE);
        }

        public void putAll(Map map) {
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                put(entry.getKey(), entry.getValue());
            }
        }
        
        public Object remove(final Object key) {
            return main.removeValue(key);
        }

        public Object removeValue(final Object value) {
            return main.remove(value);
        }

        public void clear() {
            main.clear();
        }

        public Set keySet() {
            if (keySet == null) {
                keySet = new View(main, VALUE, VALUE);
            }
            return keySet;
        }

        public Collection values() {
            if (valuesSet == null) {
                valuesSet = new View(main, VALUE, KEY);
            }
            return valuesSet;
        }

        public Set entrySet() {
            if (entrySet == null) {
                return new EntryView(main, VALUE, INVERSEMAPENTRY);
            }
            return entrySet;
        }
        
        public MapIterator mapIterator() {
            if (isEmpty()) {
                return EmptyOrderedMapIterator.INSTANCE;
            }
            return new ViewMapIterator(main, VALUE);
        }

        public OrderedMapIterator orderedMapIterator() {
            if (isEmpty()) {
                return EmptyOrderedMapIterator.INSTANCE;
            }
            return new ViewMapIterator(main, VALUE);
        }

        public BidiMap inverseBidiMap() {
            return main;
        }
        
        public OrderedBidiMap inverseOrderedBidiMap() {
            return main;
        }
        
        public boolean equals(Object obj) {
            return main.doEquals(obj, VALUE);
        }
    
        public int hashCode() {
            return main.doHashCode(VALUE);
        }
    
        public String toString() {
            return main.doToString(VALUE);
        }
    }

}
