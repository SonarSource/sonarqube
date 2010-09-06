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

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.OrderedIterator;

/**
 * A <code>List</code> implementation that is optimised for fast insertions and
 * removals at any index in the list.
 * <p>
 * This list implementation utilises a tree structure internally to ensure that
 * all insertions and removals are O(log n). This provides much faster performance
 * than both an <code>ArrayList</code> and a <code>LinkedList</code> where elements
 * are inserted and removed repeatedly from anywhere in the list.
 * <p>
 * The following relative performance statistics are indicative of this class:
 * <pre>
 *              get  add  insert  iterate  remove
 * TreeList       3    5       1       2       1
 * ArrayList      1    1      40       1      40
 * LinkedList  5800    1     350       2     325
 * </pre>
 * <code>ArrayList</code> is a good general purpose list implementation.
 * It is faster than <code>TreeList</code> for most operations except inserting
 * and removing in the middle of the list. <code>ArrayList</code> also uses less
 * memory as <code>TreeList</code> uses one object per entry.
 * <p>
 * <code>LinkedList</code> is rarely a good choice of implementation.
 * <code>TreeList</code> is almost always a good replacement for it, although it
 * does use sligtly more memory.
 * 
 * @since Commons Collections 3.1
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Joerg Schmuecker
 * @author Stephen Colebourne
 */
public class TreeList extends AbstractList {
//    add; toArray; iterator; insert; get; indexOf; remove
//    TreeList = 1260;7360;3080;  160;   170;3400;  170;
//   ArrayList =  220;1480;1760; 6870;    50;1540; 7200;
//  LinkedList =  270;7360;3350;55860;290720;2910;55200;

    /** The root node in the AVL tree */
    private AVLNode root;

    /** The current size of the list */
    private int size;

    //-----------------------------------------------------------------------
    /**
     * Constructs a new empty list.
     */
    public TreeList() {
        super();
    }

    /**
     * Constructs a new empty list that copies the specified list.
     * 
     * @param coll  the collection to copy
     * @throws NullPointerException if the collection is null
     */
    public TreeList(Collection coll) {
        super();
        addAll(coll);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the element at the specified index.
     * 
     * @param index  the index to retrieve
     * @return the element at the specified index
     */
    public Object get(int index) {
        checkInterval(index, 0, size() - 1);
        return root.get(index).getValue();
    }

    /**
     * Gets the current size of the list.
     * 
     * @return the current size
     */
    public int size() {
        return size;
    }

    /**
     * Gets an iterator over the list.
     * 
     * @return an iterator over the list
     */
    public Iterator iterator() {
        // override to go 75% faster
        return listIterator(0);
    }

    /**
     * Gets a ListIterator over the list.
     * 
     * @return the new iterator
     */
    public ListIterator listIterator() {
        // override to go 75% faster
        return listIterator(0);
    }

    /**
     * Gets a ListIterator over the list.
     * 
     * @param fromIndex  the index to start from
     * @return the new iterator
     */
    public ListIterator listIterator(int fromIndex) {
        // override to go 75% faster
        // cannot use EmptyIterator as iterator.add() must work
        checkInterval(fromIndex, 0, size());
        return new TreeListIterator(this, fromIndex);
    }

    /**
     * Searches for the index of an object in the list.
     * 
     * @return the index of the object, -1 if not found
     */
    public int indexOf(Object object) {
        // override to go 75% faster
        if (root == null) {
            return -1;
        }
        return root.indexOf(object, root.relativePosition);
    }

    /**
     * Searches for the presence of an object in the list.
     * 
     * @return true if the object is found
     */
    public boolean contains(Object object) {
        return (indexOf(object) >= 0);
    }

    /**
     * Converts the list into an array.
     * 
     * @return the list as an array
     */
    public Object[] toArray() {
        // override to go 20% faster
        Object[] array = new Object[size()];
        if (root != null) {
            root.toArray(array, root.relativePosition);
        }
        return array;
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a new element to the list.
     * 
     * @param index  the index to add before
     * @param obj  the element to add
     */
    public void add(int index, Object obj) {
        modCount++;
        checkInterval(index, 0, size());
        if (root == null) {
            root = new AVLNode(index, obj, null, null);
        } else {
            root = root.insert(index, obj);
        }
        size++;
    }

    /**
     * Sets the element at the specified index.
     * 
     * @param index  the index to set
     * @param obj  the object to store at the specified index
     * @return the previous object at that index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public Object set(int index, Object obj) {
        checkInterval(index, 0, size() - 1);
        AVLNode node = root.get(index);
        Object result = node.value;
        node.setValue(obj);
        return result;
    }

    /**
     * Removes the element at the specified index.
     * 
     * @param index  the index to remove
     * @return the previous object at that index
     */
    public Object remove(int index) {
        modCount++;
        checkInterval(index, 0, size() - 1);
        Object result = get(index);
        root = root.remove(index);
        size--;
        return result;
    }

    /**
     * Clears the list, removing all entries.
     */
    public void clear() {
        modCount++;
        root = null;
        size = 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the index is valid.
     * 
     * @param index  the index to check
     * @param startIndex  the first allowed index
     * @param endIndex  the last allowed index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    private void checkInterval(int index, int startIndex, int endIndex) {
        if (index < startIndex || index > endIndex) {
            throw new IndexOutOfBoundsException("Invalid index:" + index + ", size=" + size());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implements an AVLNode which keeps the offset updated.
     * <p>
     * This node contains the real work.
     * TreeList is just there to implement {@link java.util.List}.
     * The nodes don't know the index of the object they are holding.  They
     * do know however their position relative to their parent node.
     * This allows to calculate the index of a node while traversing the tree.
     * <p>
     * The Faedelung calculation stores a flag for both the left and right child
     * to indicate if they are a child (false) or a link as in linked list (true).
     */
    static class AVLNode {
        /** The left child node or the predecessor if {@link #leftIsPrevious}.*/
        private AVLNode left;
        /** Flag indicating that left reference is not a subtree but the predecessor. */
        private boolean leftIsPrevious;
        /** The right child node or the successor if {@link #rightIsNext}. */
        private AVLNode right;
        /** Flag indicating that right reference is not a subtree but the successor. */
        private boolean rightIsNext;
        /** How many levels of left/right are below this one. */
        private int height;
        /** The relative position, root holds absolute position. */
        private int relativePosition;
        /** The stored element. */
        private Object value;

        /**
         * Constructs a new node with a relative position.
         * 
         * @param relativePosition  the relative position of the node
         * @param obj  the value for the ndoe
         * @param rightFollower the node with the value following this one
         * @param leftFollower the node with the value leading this one
         */
        private AVLNode(int relativePosition, Object obj, AVLNode rightFollower, AVLNode leftFollower) {
            this.relativePosition = relativePosition;
            value = obj;
            rightIsNext = true;
            leftIsPrevious = true;
            right = rightFollower;
            left = leftFollower;
        }

        /**
         * Gets the value.
         * 
         * @return the value of this node
         */
        Object getValue() {
            return value;
        }

        /**
         * Sets the value.
         * 
         * @param obj  the value to store
         */
        void setValue(Object obj) {
            this.value = obj;
        }

        /**
         * Locate the element with the given index relative to the
         * offset of the parent of this node.
         */
        AVLNode get(int index) {
            int indexRelativeToMe = index - relativePosition;

            if (indexRelativeToMe == 0) {
                return this;
            }

            AVLNode nextNode = ((indexRelativeToMe < 0) ? getLeftSubTree() : getRightSubTree());
            if (nextNode == null) {
                return null;
            }
            return nextNode.get(indexRelativeToMe);
        }

        /**
         * Locate the index that contains the specified object.
         */
        int indexOf(Object object, int index) {
            if (getLeftSubTree() != null) {
                int result = left.indexOf(object, index + left.relativePosition);
                if (result != -1) {
                    return result;
                }
            }
            if (value == null ? value == object : value.equals(object)) {
                return index;
            }
            if (getRightSubTree() != null) {
                return right.indexOf(object, index + right.relativePosition);
            }
            return -1;
        }

        /**
         * Stores the node and its children into the array specified.
         * 
         * @param array the array to be filled
         * @param index the index of this node
         */
        void toArray(Object[] array, int index) {
            array[index] = value;
            if (getLeftSubTree() != null) {
                left.toArray(array, index + left.relativePosition);
            }
            if (getRightSubTree() != null) {
                right.toArray(array, index + right.relativePosition);
            }
        }

        /**
         * Gets the next node in the list after this one.
         * 
         * @return the next node
         */
        AVLNode next() {
            if (rightIsNext || right == null) {
                return right;
            }
            return right.min();
        }

        /**
         * Gets the node in the list before this one.
         * 
         * @return the previous node
         */
        AVLNode previous() {
            if (leftIsPrevious || left == null) {
                return left;
            }
            return left.max();
        }

        /**
         * Inserts a node at the position index.  
         * 
         * @param index is the index of the position relative to the position of 
         * the parent node.
         * @param obj is the object to be stored in the position.
         */
        AVLNode insert(int index, Object obj) {
            int indexRelativeToMe = index - relativePosition;

            if (indexRelativeToMe <= 0) {
                return insertOnLeft(indexRelativeToMe, obj);
            } else {
                return insertOnRight(indexRelativeToMe, obj);
            }
        }

        private AVLNode insertOnLeft(int indexRelativeToMe, Object obj) {
            AVLNode ret = this;

            if (getLeftSubTree() == null) {
                setLeft(new AVLNode(-1, obj, this, left), null);
            } else {
                setLeft(left.insert(indexRelativeToMe, obj), null);
            }

            if (relativePosition >= 0) {
                relativePosition++;
            }
            ret = balance();
            recalcHeight();
            return ret;
        }

        private AVLNode insertOnRight(int indexRelativeToMe, Object obj) {
            AVLNode ret = this;

            if (getRightSubTree() == null) {
                setRight(new AVLNode(+1, obj, right, this), null);
            } else {
                setRight(right.insert(indexRelativeToMe, obj), null);
            }
            if (relativePosition < 0) {
                relativePosition--;
            }
            ret = balance();
            recalcHeight();
            return ret;
        }

        //-----------------------------------------------------------------------
        /**
         * Gets the left node, returning null if its a faedelung.
         */
        private AVLNode getLeftSubTree() {
            return (leftIsPrevious ? null : left);
        }

        /**
         * Gets the right node, returning null if its a faedelung.
         */
        private AVLNode getRightSubTree() {
            return (rightIsNext ? null : right);
        }

        /**
         * Gets the rightmost child of this node.
         * 
         * @return the rightmost child (greatest index)
         */
        private AVLNode max() {
            return (getRightSubTree() == null) ? this : right.max();
        }

        /**
         * Gets the leftmost child of this node.
         * 
         * @return the leftmost child (smallest index)
         */
        private AVLNode min() {
            return (getLeftSubTree() == null) ? this : left.min();
        }

        /**
         * Removes the node at a given position.
         * 
         * @param index is the index of the element to be removed relative to the position of 
         * the parent node of the current node.
         */
        AVLNode remove(int index) {
            int indexRelativeToMe = index - relativePosition;

            if (indexRelativeToMe == 0) {
                return removeSelf();
            }
            if (indexRelativeToMe > 0) {
                setRight(right.remove(indexRelativeToMe), right.right);
                if (relativePosition < 0) {
                    relativePosition++;
                }
            } else {
                setLeft(left.remove(indexRelativeToMe), left.left);
                if (relativePosition > 0) {
                    relativePosition--;
                }
            }
            recalcHeight();
            return balance();
        }

        private AVLNode removeMax() {
            if (getRightSubTree() == null) {
                return removeSelf();
            }
            setRight(right.removeMax(), right.right);
            if (relativePosition < 0) {
                relativePosition++;
            }
            recalcHeight();
            return balance();
        }

        private AVLNode removeMin() {
            if (getLeftSubTree() == null) {
                return removeSelf();
            }
            setLeft(left.removeMin(), left.left);
            if (relativePosition > 0) {
                relativePosition--;
            }
            recalcHeight();
            return balance();
        }

        /**
         * Removes this node from the tree.
         *
         * @return the node that replaces this one in the parent
         */
        private AVLNode removeSelf() {
            if (getRightSubTree() == null && getLeftSubTree() == null) {
                return null;
            }
            if (getRightSubTree() == null) {
                if (relativePosition > 0) {
                    left.relativePosition += relativePosition + (relativePosition > 0 ? 0 : 1);
                }
                left.max().setRight(null, right);
                return left;
            }
            if (getLeftSubTree() == null) {
                right.relativePosition += relativePosition - (relativePosition < 0 ? 0 : 1);
                right.min().setLeft(null, left);
                return right;
            }

            if (heightRightMinusLeft() > 0) {
                // more on the right, so delete from the right
                AVLNode rightMin = right.min();
                value = rightMin.value;
                if (leftIsPrevious) {
                    left = rightMin.left;
                }
                right = right.removeMin();
                if (relativePosition < 0) {
                    relativePosition++;
                }
            } else {
                // more on the left or equal, so delete from the left
                AVLNode leftMax = left.max();
                value = leftMax.value;
                if (rightIsNext) {
                    right = leftMax.right;
                }
                AVLNode leftPrevious = left.left;
                left = left.removeMax();
                if (left == null) {
                    // special case where left that was deleted was a double link
                    // only occurs when height difference is equal
                    left = leftPrevious;
                    leftIsPrevious = true;
                }
                if (relativePosition > 0) {
                    relativePosition--;
                }
            }
            recalcHeight();
            return this;
        }

        //-----------------------------------------------------------------------
        /**
         * Balances according to the AVL algorithm.
         */
        private AVLNode balance() {
            switch (heightRightMinusLeft()) {
                case 1 :
                case 0 :
                case -1 :
                    return this;
                case -2 :
                    if (left.heightRightMinusLeft() > 0) {
                        setLeft(left.rotateLeft(), null);
                    }
                    return rotateRight();
                case 2 :
                    if (right.heightRightMinusLeft() < 0) {
                        setRight(right.rotateRight(), null);
                    }
                    return rotateLeft();
                default :
                    throw new RuntimeException("tree inconsistent!");
            }
        }

        /**
         * Gets the relative position.
         */
        private int getOffset(AVLNode node) {
            if (node == null) {
                return 0;
            }
            return node.relativePosition;
        }

        /**
         * Sets the relative position.
         */
        private int setOffset(AVLNode node, int newOffest) {
            if (node == null) {
                return 0;
            }
            int oldOffset = getOffset(node);
            node.relativePosition = newOffest;
            return oldOffset;
        }

        /**
         * Sets the height by calculation.
         */
        private void recalcHeight() {
            height = Math.max(
                getLeftSubTree() == null ? -1 : getLeftSubTree().height,
                getRightSubTree() == null ? -1 : getRightSubTree().height) + 1;
        }

        /**
         * Returns the height of the node or -1 if the node is null.
         */
        private int getHeight(AVLNode node) {
            return (node == null ? -1 : node.height);
        }

        /**
         * Returns the height difference right - left
         */
        private int heightRightMinusLeft() {
            return getHeight(getRightSubTree()) - getHeight(getLeftSubTree());
        }

        private AVLNode rotateLeft() {
            AVLNode newTop = right; // can't be faedelung!
            AVLNode movedNode = getRightSubTree().getLeftSubTree();

            int newTopPosition = relativePosition + getOffset(newTop);
            int myNewPosition = -newTop.relativePosition;
            int movedPosition = getOffset(newTop) + getOffset(movedNode);

            setRight(movedNode, newTop);
            newTop.setLeft(this, null);

            setOffset(newTop, newTopPosition);
            setOffset(this, myNewPosition);
            setOffset(movedNode, movedPosition);
            return newTop;
        }

        private AVLNode rotateRight() {
            AVLNode newTop = left; // can't be faedelung
            AVLNode movedNode = getLeftSubTree().getRightSubTree();

            int newTopPosition = relativePosition + getOffset(newTop);
            int myNewPosition = -newTop.relativePosition;
            int movedPosition = getOffset(newTop) + getOffset(movedNode);

            setLeft(movedNode, newTop);
            newTop.setRight(this, null);

            setOffset(newTop, newTopPosition);
            setOffset(this, myNewPosition);
            setOffset(movedNode, movedPosition);
            return newTop;
        }

        /**
         * Sets the left field to the node, or the previous node if that is null
         *
         * @param node  the new left subtree node
         * @param previous  the previous node in the linked list
         */
        private void setLeft(AVLNode node, AVLNode previous) {
            leftIsPrevious = (node == null);
            left = (leftIsPrevious ? previous : node);
            recalcHeight();
        }

        /**
         * Sets the right field to the node, or the next node if that is null
         *
         * @param node  the new left subtree node
         * @param next  the next node in the linked list
         */
        private void setRight(AVLNode node, AVLNode next) {
            rightIsNext = (node == null);
            right = (rightIsNext ? next : node);
            recalcHeight();
        }

//      private void checkFaedelung() {
//          AVLNode maxNode = left.max();
//          if (!maxNode.rightIsFaedelung || maxNode.right != this) {
//              throw new RuntimeException(maxNode + " should right-faedel to " + this);
//          }
//          AVLNode minNode = right.min();
//          if (!minNode.leftIsFaedelung || minNode.left != this) {
//              throw new RuntimeException(maxNode + " should left-faedel to " + this);
//          }
//      }
//
//        private int checkTreeDepth() {
//            int hright = (getRightSubTree() == null ? -1 : getRightSubTree().checkTreeDepth());
//            //          System.out.print("checkTreeDepth");
//            //          System.out.print(this);
//            //          System.out.print(" left: ");
//            //          System.out.print(_left);
//            //          System.out.print(" right: ");
//            //          System.out.println(_right);
//
//            int hleft = (left == null ? -1 : left.checkTreeDepth());
//            if (height != Math.max(hright, hleft) + 1) {
//                throw new RuntimeException(
//                    "height should be max" + hleft + "," + hright + " but is " + height);
//            }
//            return height;
//        }
//
//        private int checkLeftSubNode() {
//            if (getLeftSubTree() == null) {
//                return 0;
//            }
//            int count = 1 + left.checkRightSubNode();
//            if (left.relativePosition != -count) {
//                throw new RuntimeException();
//            }
//            return count + left.checkLeftSubNode();
//        }
//        
//        private int checkRightSubNode() {
//            AVLNode right = getRightSubTree();
//            if (right == null) {
//                return 0;
//            }
//            int count = 1;
//            count += right.checkLeftSubNode();
//            if (right.relativePosition != count) {
//                throw new RuntimeException();
//            }
//            return count + right.checkRightSubNode();
//        }

        /**
         * Used for debugging.
         */
        public String toString() {
            return "AVLNode(" + relativePosition + "," + (left != null) + "," + value +
                "," + (getRightSubTree() != null) + ", faedelung " + rightIsNext + " )";
        }
    }

    /**
     * A list iterator over the linked list.
     */
    static class TreeListIterator implements ListIterator, OrderedIterator {
        /** The parent list */
        protected final TreeList parent;
        /**
         * Cache of the next node that will be returned by {@link #next()}.
         */
        protected AVLNode next;
        /**
         * The index of the next node to be returned.
         */
        protected int nextIndex;
        /**
         * Cache of the last node that was returned by {@link #next()}
         * or {@link #previous()}.
         */
        protected AVLNode current;
        /**
         * The index of the last node that was returned.
         */
        protected int currentIndex;
        /**
         * The modification count that the list is expected to have. If the list
         * doesn't have this count, then a
         * {@link java.util.ConcurrentModificationException} may be thrown by
         * the operations.
         */
        protected int expectedModCount;

        /**
         * Create a ListIterator for a list.
         * 
         * @param parent  the parent list
         * @param fromIndex  the index to start at
         */
        protected TreeListIterator(TreeList parent, int fromIndex) throws IndexOutOfBoundsException {
            super();
            this.parent = parent;
            this.expectedModCount = parent.modCount;
            this.next = (parent.root == null ? null : parent.root.get(fromIndex));
            this.nextIndex = fromIndex;
            this.currentIndex = -1;
        }

        /**
         * Checks the modification count of the list is the value that this
         * object expects.
         * 
         * @throws ConcurrentModificationException If the list's modification
         * count isn't the value that was expected.
         */
        protected void checkModCount() {
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            return (nextIndex < parent.size());
        }

        public Object next() {
            checkModCount();
            if (!hasNext()) {
                throw new NoSuchElementException("No element at index " + nextIndex + ".");
            }
            if (next == null) {
                next = parent.root.get(nextIndex);
            }
            Object value = next.getValue();
            current = next;
            currentIndex = nextIndex++;
            next = next.next();
            return value;
        }

        public boolean hasPrevious() {
            return (nextIndex > 0);
        }

        public Object previous() {
            checkModCount();
            if (!hasPrevious()) {
                throw new NoSuchElementException("Already at start of list.");
            }
            if (next == null) {
                next = parent.root.get(nextIndex - 1);
            } else {
                next = next.previous();
            }
            Object value = next.getValue();
            current = next;
            currentIndex = --nextIndex;
            return value;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex() - 1;
        }

        public void remove() {
            checkModCount();
            if (currentIndex == -1) {
                throw new IllegalStateException();
            }
            if (nextIndex == currentIndex) {
                // remove() following previous()
                next = next.next();
                parent.remove(currentIndex);
            } else {
                // remove() following next()
                parent.remove(currentIndex);
                nextIndex--;
            }
            current = null;
            currentIndex = -1;
            expectedModCount++;
        }

        public void set(Object obj) {
            checkModCount();
            if (current == null) {
                throw new IllegalStateException();
            }
            current.setValue(obj);
        }

        public void add(Object obj) {
            checkModCount();
            parent.add(nextIndex, obj);
            current = null;
            currentIndex = -1;
            nextIndex++;
            expectedModCount++;
        }
    }

}
