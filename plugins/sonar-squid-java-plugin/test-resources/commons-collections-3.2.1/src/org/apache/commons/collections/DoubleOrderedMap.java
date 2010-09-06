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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Red-Black tree-based implementation of Map. This class guarantees
 * that the map will be in both ascending key order and ascending
 * value order, sorted according to the natural order for the key's
 * and value's classes.
 * <p>
 * This Map is intended for applications that need to be able to look
 * up a key-value pairing by either key or value, and need to do so
 * with equal efficiency.
 * <p>
 * While that goal could be accomplished by taking a pair of TreeMaps
 * and redirecting requests to the appropriate TreeMap (e.g.,
 * containsKey would be directed to the TreeMap that maps values to
 * keys, containsValue would be directed to the TreeMap that maps keys
 * to values), there are problems with that implementation,
 * particularly when trying to keep the two TreeMaps synchronized with
 * each other. And if the data contained in the TreeMaps is large, the
 * cost of redundant storage becomes significant. (See also the new
 * {@link org.apache.commons.collections.bidimap.DualTreeBidiMap DualTreeBidiMap} and
 * {@link org.apache.commons.collections.bidimap.DualHashBidiMap DualHashBidiMap}
 * implementations.)
 * <p>
 * This solution keeps the data properly synchronized and minimizes
 * the data storage. The red-black algorithm is based on TreeMap's,
 * but has been modified to simultaneously map a tree node by key and
 * by value. This doubles the cost of put operations (but so does
 * using two TreeMaps), and nearly doubles the cost of remove
 * operations (there is a savings in that the lookup of the node to be
 * removed only has to be performed once). And since only one node
 * contains the key and value, storage is significantly less than that
 * required by two TreeMaps.
 * <p>
 * There are some limitations placed on data kept in this Map. The
 * biggest one is this:
 * <p>
 * When performing a put operation, neither the key nor the value may
 * already exist in the Map. In the java.util Map implementations
 * (HashMap, TreeMap), you can perform a put with an already mapped
 * key, and neither cares about duplicate values at all ... but this
 * implementation's put method with throw an IllegalArgumentException
 * if either the key or the value is already in the Map.
 * <p>
 * Obviously, that same restriction (and consequence of failing to
 * heed that restriction) applies to the putAll method.
 * <p>
 * The Map.Entry instances returned by the appropriate methods will
 * not allow setValue() and will throw an
 * UnsupportedOperationException on attempts to call that method.
 * <p>
 * New methods are added to take advantage of the fact that values are
 * kept sorted independently of their keys:
 * <p>
 * Object getKeyForValue(Object value) is the opposite of get; it
 * takes a value and returns its key, if any.
 * <p>
 * Object removeValue(Object value) finds and removes the specified
 * value and returns the now un-used key.
 * <p>
 * Set entrySetByValue() returns the Map.Entry's in a Set whose
 * iterator will iterate over the Map.Entry's in ascending order by
 * their corresponding values.
 * <p>
 * Set keySetByValue() returns the keys in a Set whose iterator will
 * iterate over the keys in ascending order by their corresponding
 * values.
 * <p>
 * Collection valuesByValue() returns the values in a Collection whose
 * iterator will iterate over the values in ascending order.
 *
 * @deprecated Replaced by TreeBidiMap in bidimap subpackage. Due to be removed in v4.0.
 * @see BidiMap
 * @see org.apache.commons.collections.bidimap.DualTreeBidiMap
 * @see org.apache.commons.collections.bidimap.DualHashBidiMap
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Marc Johnson
 */
public final class DoubleOrderedMap extends AbstractMap {
//  final for performance

    private static final int KEY = 0;
    private static final int VALUE = 1;
    private static final int SUM_OF_INDICES = KEY + VALUE;
    private static final int FIRST_INDEX = 0;
    private static final int NUMBER_OF_INDICES = 2;
    private static final String[] dataName = new String[] { "key", "value" };
    
    private Node[] rootNode = new Node[] { null, null };
    private int nodeCount = 0;
    private int modifications = 0;
    private Set[] setOfKeys = new Set[] { null, null };
    private Set[] setOfEntries = new Set[] { null, null };
    private Collection[] collectionOfValues = new Collection[] { null, null };

    /**
     * Construct a new DoubleOrderedMap
     */
    public DoubleOrderedMap() {
    }

    /**
     * Constructs a new DoubleOrderedMap from an existing Map, with keys and
     * values sorted
     *
     * @param map the map whose mappings are to be placed in this map.
     *
     * @throws ClassCastException if the keys in the map are not
     *                               Comparable, or are not mutually
     *                               comparable; also if the values in
     *                               the map are not Comparable, or
     *                               are not mutually Comparable
     * @throws NullPointerException if any key or value in the map
     *                                 is null
     * @throws IllegalArgumentException if there are duplicate keys
     *                                     or duplicate values in the
     *                                     map
     */
    public DoubleOrderedMap(final Map map)
            throws ClassCastException, NullPointerException,
                   IllegalArgumentException {
        putAll(map);
    }

    /**
     * Returns the key to which this map maps the specified value.
     * Returns null if the map contains no mapping for this value.
     *
     * @param value value whose associated key is to be returned.
     *
     * @return the key to which this map maps the specified value, or
     *         null if the map contains no mapping for this value.
     *
     * @throws ClassCastException if the value is of an
     *                               inappropriate type for this map.
     * @throws NullPointerException if the value is null
     */
    public Object getKeyForValue(final Object value)
            throws ClassCastException, NullPointerException {
        return doGet((Comparable) value, VALUE);
    }

    /**
     * Removes the mapping for this value from this map if present
     *
     * @param value value whose mapping is to be removed from the map.
     *
     * @return previous key associated with specified value, or null
     *         if there was no mapping for value.
     */
    public Object removeValue(final Object value) {
        return doRemove((Comparable) value, VALUE);
    }

    /**
     * Returns a set view of the mappings contained in this map. Each
     * element in the returned set is a Map.Entry. The set is backed
     * by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the
     * set is in progress, the results of the iteration are
     * undefined. The set supports element removal, which removes the
     * corresponding mapping from the map, via the Iterator.remove,
     * Set.remove, removeAll, retainAll and clear operations.  It does
     * not support the add or addAll operations.<p>
     *
     * The difference between this method and entrySet is that
     * entrySet's iterator() method returns an iterator that iterates
     * over the mappings in ascending order by key. This method's
     * iterator method iterates over the mappings in ascending order
     * by value.
     *
     * @return a set view of the mappings contained in this map.
     */
    public Set entrySetByValue() {

        if (setOfEntries[VALUE] == null) {
            setOfEntries[VALUE] = new AbstractSet() {

                public Iterator iterator() {

                    return new DoubleOrderedMapIterator(VALUE) {

                        protected Object doGetNext() {
                            return lastReturnedNode;
                        }
                    };
                }

                public boolean contains(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    Map.Entry entry = (Map.Entry) o;
                    Object    key   = entry.getKey();
                    Node      node  = lookup((Comparable) entry.getValue(),
                                             VALUE);

                    return (node != null) && node.getData(KEY).equals(key);
                }

                public boolean remove(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    Map.Entry entry = (Map.Entry) o;
                    Object    key   = entry.getKey();
                    Node      node  = lookup((Comparable) entry.getValue(),
                                             VALUE);

                    if ((node != null) && node.getData(KEY).equals(key)) {
                        doRedBlackDelete(node);

                        return true;
                    }

                    return false;
                }

                public int size() {
                    return DoubleOrderedMap.this.size();
                }

                public void clear() {
                    DoubleOrderedMap.this.clear();
                }
            };
        }

        return setOfEntries[VALUE];
    }

    /**
     * Returns a set view of the keys contained in this map.  The set
     * is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an
     * iteration over the set is in progress, the results of the
     * iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * Iterator.remove, Set.remove, removeAll, retainAll, and clear
     * operations. It does not support the add or addAll
     * operations.<p>
     *
     * The difference between this method and keySet is that keySet's
     * iterator() method returns an iterator that iterates over the
     * keys in ascending order by key. This method's iterator method
     * iterates over the keys in ascending order by value.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySetByValue() {

        if (setOfKeys[VALUE] == null) {
            setOfKeys[VALUE] = new AbstractSet() {

                public Iterator iterator() {

                    return new DoubleOrderedMapIterator(VALUE) {

                        protected Object doGetNext() {
                            return lastReturnedNode.getData(KEY);
                        }
                    };
                }

                public int size() {
                    return DoubleOrderedMap.this.size();
                }

                public boolean contains(Object o) {
                    return containsKey(o);
                }

                public boolean remove(Object o) {

                    int oldnodeCount = nodeCount;

                    DoubleOrderedMap.this.remove(o);

                    return nodeCount != oldnodeCount;
                }

                public void clear() {
                    DoubleOrderedMap.this.clear();
                }
            };
        }

        return setOfKeys[VALUE];
    }

    /**
     * Returns a collection view of the values contained in this
     * map. The collection is backed by the map, so changes to the map
     * are reflected in the collection, and vice-versa. If the map is
     * modified while an iteration over the collection is in progress,
     * the results of the iteration are undefined. The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the Iterator.remove,
     * Collection.remove, removeAll, retainAll and clear operations.
     * It does not support the add or addAll operations.<p>
     *
     * The difference between this method and values is that values's
     * iterator() method returns an iterator that iterates over the
     * values in ascending order by key. This method's iterator method
     * iterates over the values in ascending order by key.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection valuesByValue() {

        if (collectionOfValues[VALUE] == null) {
            collectionOfValues[VALUE] = new AbstractCollection() {

                public Iterator iterator() {

                    return new DoubleOrderedMapIterator(VALUE) {

                        protected Object doGetNext() {
                            return lastReturnedNode.getData(VALUE);
                        }
                    };
                }

                public int size() {
                    return DoubleOrderedMap.this.size();
                }

                public boolean contains(Object o) {
                    return containsValue(o);
                }

                public boolean remove(Object o) {

                    int oldnodeCount = nodeCount;

                    removeValue(o);

                    return nodeCount != oldnodeCount;
                }

                public boolean removeAll(Collection c) {

                    boolean  modified = false;
                    Iterator iter     = c.iterator();

                    while (iter.hasNext()) {
                        if (removeValue(iter.next()) != null) {
                            modified = true;
                        }
                    }

                    return modified;
                }

                public void clear() {
                    DoubleOrderedMap.this.clear();
                }
            };
        }

        return collectionOfValues[VALUE];
    }

    /**
     * common remove logic (remove by key or remove by value)
     *
     * @param o the key, or value, that we're looking for
     * @param index KEY or VALUE
     *
     * @return the key, if remove by value, or the value, if remove by
     *         key. null if the specified key or value could not be
     *         found
     */
    private Object doRemove(final Comparable o, final int index) {

        Node   node = lookup(o, index);
        Object rval = null;

        if (node != null) {
            rval = node.getData(oppositeIndex(index));

            doRedBlackDelete(node);
        }

        return rval;
    }

    /**
     * common get logic, used to get by key or get by value
     *
     * @param o the key or value that we're looking for
     * @param index KEY or VALUE
     *
     * @return the key (if the value was mapped) or the value (if the
     *         key was mapped); null if we couldn't find the specified
     *         object
     */
    private Object doGet(final Comparable o, final int index) {

        checkNonNullComparable(o, index);

        Node node = lookup(o, index);

        return ((node == null)
                ? null
                : node.getData(oppositeIndex(index)));
    }

    /**
     * Get the opposite index of the specified index
     *
     * @param index KEY or VALUE
     *
     * @return VALUE (if KEY was specified), else KEY
     */
    private int oppositeIndex(final int index) {

        // old trick ... to find the opposite of a value, m or n,
        // subtract the value from the sum of the two possible
        // values. (m + n) - m = n; (m + n) - n = m
        return SUM_OF_INDICES - index;
    }

    /**
     * do the actual lookup of a piece of data
     *
     * @param data the key or value to be looked up
     * @param index KEY or VALUE
     *
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
                node = (cmp < 0)
                       ? node.getLeft(index)
                       : node.getRight(index);
            }
        }

        return rval;
    }

    /**
     * Compare two objects
     *
     * @param o1 the first object
     * @param o2 the second object
     *
     * @return negative value if o1 < o2; 0 if o1 == o2; positive
     *         value if o1 > o2
     */
    private static int compare(final Comparable o1, final Comparable o2) {
        return o1.compareTo(o2);
    }

    /**
     * find the least node from a given node. very useful for starting
     * a sorting iterator ...
     *
     * @param node the node from which we will start searching
     * @param index KEY or VALUE
     *
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
     * get the next larger node from the specified node
     *
     * @param node the node to be searched from
     * @param index KEY or VALUE
     *
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
            Node child  = node;

            while ((parent != null) && (child == parent.getRight(index))) {
                child  = parent;
                parent = parent.getParent(index);
            }

            rval = parent;
        }

        return rval;
    }

    /**
     * copy the color from one node to another, dealing with the fact
     * that one or both nodes may, in fact, be null
     *
     * @param from the node whose color we're copying; may be null
     * @param to the node whose color we're changing; may be null
     * @param index KEY or VALUE
     */
    private static void copyColor(final Node from, final Node to,
                                  final int index) {

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
     * @param index KEY or VALUE
     */
    private static boolean isRed(final Node node, final int index) {

        return ((node == null)
                ? false
                : node.isRed(index));
    }

    /**
     * is the specified black red? if the node does not exist, sure,
     * it's black, thank you
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static boolean isBlack(final Node node, final int index) {

        return ((node == null)
                ? true
                : node.isBlack(index));
    }

    /**
     * force a node (if it exists) red
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
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
     * @param index KEY or VALUE
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
     * @param index KEY or VALUE
     */
    private static Node getGrandParent(final Node node, final int index) {
        return getParent(getParent(node, index), index);
    }

    /**
     * get a node's parent. mind you, the node, or its parent, may not
     * exist. no problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static Node getParent(final Node node, final int index) {

        return ((node == null)
                ? null
                : node.getParent(index));
    }

    /**
     * get a node's right child. mind you, the node may not exist. no
     * problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static Node getRightChild(final Node node, final int index) {

        return (node == null)
               ? null
               : node.getRight(index);
    }

    /**
     * get a node's left child. mind you, the node may not exist. no
     * problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static Node getLeftChild(final Node node, final int index) {

        return (node == null)
               ? null
               : node.getLeft(index);
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
     * @param index KEY or VALUE
     */
    private static boolean isLeftChild(final Node node, final int index) {

        return (node == null)
               ? true
               : ((node.getParent(index) == null)
                  ? false
                  : (node == node.getParent(index).getLeft(index)));
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
     * @param index KEY or VALUE
     */
    private static boolean isRightChild(final Node node, final int index) {

        return (node == null)
               ? true
               : ((node.getParent(index) == null)
                  ? false
                  : (node == node.getParent(index).getRight(index)));
    }

    /**
     * do a rotate left. standard fare in the world of balanced trees
     *
     * @param node the node to be rotated
     * @param index KEY or VALUE
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
     * @param index KEY or VALUE
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
     * @param index KEY or VALUE
     */
    private void doRedBlackInsert(final Node insertedNode, final int index) {

        Node currentNode = insertedNode;

        makeRed(currentNode, index);

        while ((currentNode != null) && (currentNode != rootNode[index])
                && (isRed(currentNode.getParent(index), index))) {
            if (isLeftChild(getParent(currentNode, index), index)) {
                Node y = getRightChild(getGrandParent(currentNode, index),
                                       index);

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
                        rotateRight(getGrandParent(currentNode, index),
                                    index);
                    }
                }
            } else {

                // just like clause above, except swap left for right
                Node y = getLeftChild(getGrandParent(currentNode, index),
                                      index);

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
            if ((deletedNode.getLeft(index) != null)
                    && (deletedNode.getRight(index) != null)) {
                swapPosition(nextGreater(deletedNode, index), deletedNode,
                             index);
            }

            Node replacement = ((deletedNode.getLeft(index) != null)
                                ? deletedNode.getLeft(index)
                                : deletedNode.getRight(index));

            if (replacement != null) {
                replacement.setParent(deletedNode.getParent(index), index);

                if (deletedNode.getParent(index) == null) {
                    rootNode[index] = replacement;
                } else if (deletedNode
                           == deletedNode.getParent(index).getLeft(index)) {
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
                        if (deletedNode
                                == deletedNode.getParent(index)
                                    .getLeft(index)) {
                            deletedNode.getParent(index).setLeft(null, index);
                        } else {
                            deletedNode.getParent(index).setRight(null,
                                                  index);
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
     * @param index KEY or VALUE
     */
    private void doRedBlackDeleteFixup(final Node replacementNode,
                                       final int index) {

        Node currentNode = replacementNode;

        while ((currentNode != rootNode[index])
                && (isBlack(currentNode, index))) {
            if (isLeftChild(currentNode, index)) {
                Node siblingNode =
                    getRightChild(getParent(currentNode, index), index);

                if (isRed(siblingNode, index)) {
                    makeBlack(siblingNode, index);
                    makeRed(getParent(currentNode, index), index);
                    rotateLeft(getParent(currentNode, index), index);

                    siblingNode = getRightChild(getParent(currentNode, index), index);
                }

                if (isBlack(getLeftChild(siblingNode, index), index)
                        && isBlack(getRightChild(siblingNode, index),
                                   index)) {
                    makeRed(siblingNode, index);

                    currentNode = getParent(currentNode, index);
                } else {
                    if (isBlack(getRightChild(siblingNode, index), index)) {
                        makeBlack(getLeftChild(siblingNode, index), index);
                        makeRed(siblingNode, index);
                        rotateRight(siblingNode, index);

                        siblingNode =
                            getRightChild(getParent(currentNode, index), index);
                    }

                    copyColor(getParent(currentNode, index), siblingNode,
                              index);
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

                        siblingNode =
                            getLeftChild(getParent(currentNode, index), index);
                    }

                    copyColor(getParent(currentNode, index), siblingNode,
                              index);
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
     * @param index KEY or VALUE
     */
    private void swapPosition(final Node x, final Node y, final int index) {

        // Save initial values.
        Node    xFormerParent     = x.getParent(index);
        Node    xFormerLeftChild  = x.getLeft(index);
        Node    xFormerRightChild = x.getRight(index);
        Node    yFormerParent     = y.getParent(index);
        Node    yFormerLeftChild  = y.getLeft(index);
        Node    yFormerRightChild = y.getRight(index);
        boolean xWasLeftChild     =
            (x.getParent(index) != null)
            && (x == x.getParent(index).getLeft(index));
        boolean yWasLeftChild     =
            (y.getParent(index) != null)
            && (y == y.getParent(index).getLeft(index));

        // Swap, handling special cases of one being the other's parent.
        if (x == yFormerParent) {    // x was y's parent
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

        if (y == xFormerParent) {    // y was x's parent
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
     * @param index KEY or VALUE (used to put the right word in the
     *              exception message)
     *
     * @throws NullPointerException if o is null
     * @throws ClassCastException if o is not Comparable
     */
    private static void checkNonNullComparable(final Object o,
                                               final int index) {

        if (o == null) {
            throw new NullPointerException(dataName[index]
                                           + " cannot be null");
        }

        if (!(o instanceof Comparable)) {
            throw new ClassCastException(dataName[index]
                                         + " must be Comparable");
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
    private static void checkKeyAndValue(final Object key,
                                         final Object value) {
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
    private void insertValue(final Node newNode)
            throws IllegalArgumentException {

        Node node = rootNode[VALUE];

        while (true) {
            int cmp = compare(newNode.getData(VALUE), node.getData(VALUE));

            if (cmp == 0) {
                throw new IllegalArgumentException(
                    "Cannot store a duplicate value (\""
                    + newNode.getData(VALUE) + "\") in this Map");
            } else if (cmp < 0) {
                if (node.getLeft(VALUE) != null) {
                    node = node.getLeft(VALUE);
                } else {
                    node.setLeft(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
            } else {    // cmp > 0
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

    /* ********** START implementation of Map ********** */

    /**
     * Returns the number of key-value mappings in this map. If the
     * map contains more than Integer.MAXVALUE elements, returns
     * Integer.MAXVALUE.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return nodeCount;
    }

    /**
     * Returns true if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested.
     *
     * @return true if this map contains a mapping for the specified
     *         key.
     *
     * @throws ClassCastException if the key is of an inappropriate
     *                               type for this map.
     * @throws NullPointerException if the key is null
     */
    public boolean containsKey(final Object key)
            throws ClassCastException, NullPointerException {

        checkKey(key);

        return lookup((Comparable) key, KEY) != null;
    }

    /**
     * Returns true if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     *
     * @return true if this map maps one or more keys to the specified
     *         value.
     */
    public boolean containsValue(final Object value) {

        checkValue(value);

        return lookup((Comparable) value, VALUE) != null;
    }

    /**
     * Returns the value to which this map maps the specified
     * key. Returns null if the map contains no mapping for this key.
     *
     * @param key key whose associated value is to be returned.
     *
     * @return the value to which this map maps the specified key, or
     *         null if the map contains no mapping for this key.
     *
     * @throws ClassCastException if the key is of an inappropriate
     *                               type for this map.
     * @throws NullPointerException if the key is null
     */
    public Object get(final Object key)
            throws ClassCastException, NullPointerException {
        return doGet((Comparable) key, KEY);
    }

    /**
     * Associates the specified value with the specified key in this
     * map.
     *
     * @param key key with which the specified value is to be
     *            associated.
     * @param value value to be associated with the specified key.
     *
     * @return null
     *
     * @throws ClassCastException if the class of the specified key
     *                               or value prevents it from being
     *                               stored in this map.
     * @throws NullPointerException if the specified key or value
     *                                 is null
     * @throws IllegalArgumentException if the key duplicates an
     *                                     existing key, or if the
     *                                     value duplicates an
     *                                     existing value
     */
    public Object put(final Object key, final Object value)
            throws ClassCastException, NullPointerException,
                   IllegalArgumentException {

        checkKeyAndValue(key, value);

        Node node = rootNode[KEY];

        if (node == null) {
            Node root = new Node((Comparable) key, (Comparable) value);

            rootNode[KEY]   = root;
            rootNode[VALUE] = root;

            grow();
        } else {
            while (true) {
                int cmp = compare((Comparable) key, node.getData(KEY));

                if (cmp == 0) {
                    throw new IllegalArgumentException(
                        "Cannot store a duplicate key (\"" + key
                        + "\") in this Map");
                } else if (cmp < 0) {
                    if (node.getLeft(KEY) != null) {
                        node = node.getLeft(KEY);
                    } else {
                        Node newNode = new Node((Comparable) key,
                                                (Comparable) value);

                        insertValue(newNode);
                        node.setLeft(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();

                        break;
                    }
                } else {    // cmp > 0
                    if (node.getRight(KEY) != null) {
                        node = node.getRight(KEY);
                    } else {
                        Node newNode = new Node((Comparable) key,
                                                (Comparable) value);

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

        return null;
    }

    /**
     * Removes the mapping for this key from this map if present
     *
     * @param key key whose mapping is to be removed from the map.
     *
     * @return previous value associated with specified key, or null
     *         if there was no mapping for key.
     */
    public Object remove(final Object key) {
        return doRemove((Comparable) key, KEY);
    }

    /**
     * Removes all mappings from this map
     */
    public void clear() {

        modify();

        nodeCount   = 0;
        rootNode[KEY]   = null;
        rootNode[VALUE] = null;
    }

    /**
     * Returns a set view of the keys contained in this map.  The set
     * is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an
     * iteration over the set is in progress, the results of the
     * iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * Iterator.remove, Set.remove, removeAll, retainAll, and clear
     * operations.  It does not support the add or addAll operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySet() {

        if (setOfKeys[KEY] == null) {
            setOfKeys[KEY] = new AbstractSet() {

                public Iterator iterator() {

                    return new DoubleOrderedMapIterator(KEY) {

                        protected Object doGetNext() {
                            return lastReturnedNode.getData(KEY);
                        }
                    };
                }

                public int size() {
                    return DoubleOrderedMap.this.size();
                }

                public boolean contains(Object o) {
                    return containsKey(o);
                }

                public boolean remove(Object o) {

                    int oldNodeCount = nodeCount;

                    DoubleOrderedMap.this.remove(o);

                    return nodeCount != oldNodeCount;
                }

                public void clear() {
                    DoubleOrderedMap.this.clear();
                }
            };
        }

        return setOfKeys[KEY];
    }

    /**
     * Returns a collection view of the values contained in this
     * map. The collection is backed by the map, so changes to the map
     * are reflected in the collection, and vice-versa. If the map is
     * modified while an iteration over the collection is in progress,
     * the results of the iteration are undefined. The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the Iterator.remove,
     * Collection.remove, removeAll, retainAll and clear operations.
     * It does not support the add or addAll operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection values() {

        if (collectionOfValues[KEY] == null) {
            collectionOfValues[KEY] = new AbstractCollection() {

                public Iterator iterator() {

                    return new DoubleOrderedMapIterator(KEY) {

                        protected Object doGetNext() {
                            return lastReturnedNode.getData(VALUE);
                        }
                    };
                }

                public int size() {
                    return DoubleOrderedMap.this.size();
                }

                public boolean contains(Object o) {
                    return containsValue(o);
                }

                public boolean remove(Object o) {

                    int oldNodeCount = nodeCount;

                    removeValue(o);

                    return nodeCount != oldNodeCount;
                }

                public boolean removeAll(Collection c) {

                    boolean  modified = false;
                    Iterator iter     = c.iterator();

                    while (iter.hasNext()) {
                        if (removeValue(iter.next()) != null) {
                            modified = true;
                        }
                    }

                    return modified;
                }

                public void clear() {
                    DoubleOrderedMap.this.clear();
                }
            };
        }

        return collectionOfValues[KEY];
    }

    /**
     * Returns a set view of the mappings contained in this map. Each
     * element in the returned set is a Map.Entry. The set is backed
     * by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the
     * set is in progress, the results of the iteration are
     * undefined.
     * <p>
     * The set supports element removal, which removes the corresponding
     * mapping from the map, via the Iterator.remove, Set.remove, removeAll,
     * retainAll and clear operations.
     * It does not support the add or addAll operations.
     * The setValue method is not supported on the Map Entry.
     *
     * @return a set view of the mappings contained in this map.
     */
    public Set entrySet() {

        if (setOfEntries[KEY] == null) {
            setOfEntries[KEY] = new AbstractSet() {

                public Iterator iterator() {

                    return new DoubleOrderedMapIterator(KEY) {

                        protected Object doGetNext() {
                            return lastReturnedNode;
                        }
                    };
                }

                public boolean contains(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    Map.Entry entry = (Map.Entry) o;
                    Object    value = entry.getValue();
                    Node      node  = lookup((Comparable) entry.getKey(),
                                             KEY);

                    return (node != null)
                           && node.getData(VALUE).equals(value);
                }

                public boolean remove(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    Map.Entry entry = (Map.Entry) o;
                    Object    value = entry.getValue();
                    Node      node  = lookup((Comparable) entry.getKey(),
                                             KEY);

                    if ((node != null) && node.getData(VALUE).equals(value)) {
                        doRedBlackDelete(node);

                        return true;
                    }

                    return false;
                }

                public int size() {
                    return DoubleOrderedMap.this.size();
                }

                public void clear() {
                    DoubleOrderedMap.this.clear();
                }
            };
        }

        return setOfEntries[KEY];
    }

    /* **********  END  implementation of Map ********** */
    private abstract class DoubleOrderedMapIterator implements Iterator {

        private int    expectedModifications;
        protected Node lastReturnedNode;
        private Node   nextNode;
        private int    iteratorType;

        /**
         * Constructor
         *
         * @param type
         */
        DoubleOrderedMapIterator(final int type) {

            iteratorType          = type;
            expectedModifications = DoubleOrderedMap.this.modifications;
            lastReturnedNode      = null;
            nextNode              = leastNode(rootNode[iteratorType],
                                              iteratorType);
        }

        /**
         * @return 'next', whatever that means for a given kind of
         *         DoubleOrderedMapIterator
         */
        protected abstract Object doGetNext();

        /* ********** START implementation of Iterator ********** */

        /**
         * @return true if the iterator has more elements.
         */
        public final boolean hasNext() {
            return nextNode != null;
        }

        /**
         * @return the next element in the iteration.
         *
         * @throws NoSuchElementException if iteration has no more
         *                                   elements.
         * @throws ConcurrentModificationException if the
         *                                            DoubleOrderedMap is
         *                                            modified behind
         *                                            the iterator's
         *                                            back
         */
        public final Object next()
                throws NoSuchElementException,
                       ConcurrentModificationException {

            if (nextNode == null) {
                throw new NoSuchElementException();
            }

            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }

            lastReturnedNode = nextNode;
            nextNode         = nextGreater(nextNode, iteratorType);

            return doGetNext();
        }

        /**
         * Removes from the underlying collection the last element
         * returned by the iterator. This method can be called only
         * once per call to next. The behavior of an iterator is
         * unspecified if the underlying collection is modified while
         * the iteration is in progress in any way other than by
         * calling this method.
         *
         * @throws IllegalStateException if the next method has not
         *                                  yet been called, or the
         *                                  remove method has already
         *                                  been called after the last
         *                                  call to the next method.
         * @throws ConcurrentModificationException if the
         *                                            DoubleOrderedMap is
         *                                            modified behind
         *                                            the iterator's
         *                                            back
         */
        public final void remove()
                throws IllegalStateException,
                       ConcurrentModificationException {

            if (lastReturnedNode == null) {
                throw new IllegalStateException();
            }

            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }

            doRedBlackDelete(lastReturnedNode);

            expectedModifications++;

            lastReturnedNode = null;
        }

        /* **********  END  implementation of Iterator ********** */
    }    // end private abstract class DoubleOrderedMapIterator

    // final for performance
    private static final class Node implements Map.Entry, KeyValue {

        private Comparable[] data;
        private Node[]       leftNode;
        private Node[]       rightNode;
        private Node[]       parentNode;
        private boolean[]    blackColor;
        private int          hashcodeValue;
        private boolean      calculatedHashCode;

        /**
         * Make a new cell with given key and value, and with null
         * links, and black (true) colors.
         *
         * @param key
         * @param value
         */
        Node(final Comparable key, final Comparable value) {

            data               = new Comparable[]{ key, value };
            leftNode           = new Node[]{ null, null };
            rightNode          = new Node[]{ null, null };
            parentNode         = new Node[]{ null, null };
            blackColor         = new boolean[]{ true, true };
            calculatedHashCode = false;
        }

        /**
         * get the specified data
         *
         * @param index KEY or VALUE
         *
         * @return the key or value
         */
        private Comparable getData(final int index) {
            return data[index];
        }

        /**
         * Set this node's left node
         *
         * @param node the new left node
         * @param index KEY or VALUE
         */
        private void setLeft(final Node node, final int index) {
            leftNode[index] = node;
        }

        /**
         * get the left node
         *
         * @param index KEY or VALUE
         *
         * @return the left node -- may be null
         */
        private Node getLeft(final int index) {
            return leftNode[index];
        }

        /**
         * Set this node's right node
         *
         * @param node the new right node
         * @param index KEY or VALUE
         */
        private void setRight(final Node node, final int index) {
            rightNode[index] = node;
        }

        /**
         * get the right node
         *
         * @param index KEY or VALUE
         *
         * @return the right node -- may be null
         */
        private Node getRight(final int index) {
            return rightNode[index];
        }

        /**
         * Set this node's parent node
         *
         * @param node the new parent node
         * @param index KEY or VALUE
         */
        private void setParent(final Node node, final int index) {
            parentNode[index] = node;
        }

        /**
         * get the parent node
         *
         * @param index KEY or VALUE
         *
         * @return the parent node -- may be null
         */
        private Node getParent(final int index) {
            return parentNode[index];
        }

        /**
         * exchange colors with another node
         *
         * @param node the node to swap with
         * @param index KEY or VALUE
         */
        private void swapColors(final Node node, final int index) {

            // Swap colors -- old hacker's trick
            blackColor[index]      ^= node.blackColor[index];
            node.blackColor[index] ^= blackColor[index];
            blackColor[index]      ^= node.blackColor[index];
        }

        /**
         * is this node black?
         *
         * @param index KEY or VALUE
         *
         * @return true if black (which is represented as a true boolean)
         */
        private boolean isBlack(final int index) {
            return blackColor[index];
        }

        /**
         * is this node red?
         *
         * @param index KEY or VALUE
         *
         * @return true if non-black
         */
        private boolean isRed(final int index) {
            return !blackColor[index];
        }

        /**
         * make this node black
         *
         * @param index KEY or VALUE
         */
        private void setBlack(final int index) {
            blackColor[index] = true;
        }

        /**
         * make this node red
         *
         * @param index KEY or VALUE
         */
        private void setRed(final int index) {
            blackColor[index] = false;
        }

        /**
         * make this node the same color as another
         *
         * @param node the node whose color we're adopting
         * @param index KEY or VALUE
         */
        private void copyColor(final Node node, final int index) {
            blackColor[index] = node.blackColor[index];
        }

        /* ********** START implementation of Map.Entry ********** */

        /**
         * @return the key corresponding to this entry.
         */
        public Object getKey() {
            return data[KEY];
        }

        /**
         * @return the value corresponding to this entry.
         */
        public Object getValue() {
            return data[VALUE];
        }

        /**
         * Optional operation that is not permitted in this
         * implementation
         *
         * @param ignored
         *
         * @return does not return
         *
         * @throws UnsupportedOperationException
         */
        public Object setValue(Object ignored)
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                "Map.Entry.setValue is not supported");
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns true if the given object is also a map entry and
         * the two entries represent the same mapping.
         *
         * @param o object to be compared for equality with this map
         *          entry.
         * @return true if the specified object is equal to this map
         *         entry.
         */
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry e = (Map.Entry) o;

            return data[KEY].equals(e.getKey())
                   && data[VALUE].equals(e.getValue());
        }

        /**
         * @return the hash code value for this map entry.
         */
        public int hashCode() {

            if (!calculatedHashCode) {
                hashcodeValue      = data[KEY].hashCode()
                                     ^ data[VALUE].hashCode();
                calculatedHashCode = true;
            }

            return hashcodeValue;
        }

        /* **********  END  implementation of Map.Entry ********** */
    }
}    // end public class DoubleOrderedMap
