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
package org.apache.commons.collections.comparators;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Reverses the order of another comparator by reversing the arguments
 * to its {@link #compare(Object, Object) compare} method.
 * 
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Henri Yandell
 * @author Michael A. Smith
 * 
 * @see java.util.Collections#reverseOrder()
 */
public class ReverseComparator implements Comparator, Serializable {

    /** Serialization version from Collections 2.0. */
    private static final long serialVersionUID = 2858887242028539265L;

    /** The comparator being decorated. */
    private Comparator comparator;

    //-----------------------------------------------------------------------
    /**
     * Creates a comparator that compares objects based on the inverse of their
     * natural ordering.  Using this Constructor will create a ReverseComparator
     * that is functionally identical to the Comparator returned by
     * java.util.Collections.<b>reverseOrder()</b>.
     * 
     * @see java.util.Collections#reverseOrder()
     */
    public ReverseComparator() {
        this(null);
    }

    /**
     * Creates a comparator that inverts the comparison
     * of the given comparator.  If you pass in <code>null</code>,
     * the ReverseComparator defaults to reversing the
     * natural order, as per 
     * {@link java.util.Collections#reverseOrder()}</b>.
     * 
     * @param comparator Comparator to reverse
     */
    public ReverseComparator(Comparator comparator) {
        if(comparator != null) {
            this.comparator = comparator;
        } else {
            this.comparator = ComparableComparator.getInstance();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Compares two objects in reverse order.
     * 
     * @param obj1  the first object to compare
     * @param obj2  the second object to compare
     * @return negative if obj1 is less, positive if greater, zero if equal
     */
    public int compare(Object obj1, Object obj2) {
        return comparator.compare(obj2, obj1);
    }

    //-----------------------------------------------------------------------
    /**
     * Implement a hash code for this comparator that is consistent with
     * {@link #equals(Object) equals}.
     * 
     * @return a suitable hash code
     * @since Commons Collections 3.0
     */
    public int hashCode() {
        return "ReverseComparator".hashCode() ^ comparator.hashCode();
    }

    /**
     * Returns <code>true</code> iff <i>that</i> Object is 
     * is a {@link Comparator} whose ordering is known to be 
     * equivalent to mine.
     * <p>
     * This implementation returns <code>true</code>
     * iff <code><i>object</i>.{@link Object#getClass() getClass()}</code>
     * equals <code>this.getClass()</code>, and the underlying 
     * comparators are equal.
     * Subclasses may want to override this behavior to remain consistent
     * with the {@link Comparator#equals(Object) equals} contract.
     * 
     * @param object  the object to compare to
     * @return true if equal
     * @since Commons Collections 3.0
     */
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        } else if(null == object) {
            return false;
        } else if(object.getClass().equals(this.getClass())) {
            ReverseComparator thatrc = (ReverseComparator)object;
            return comparator.equals(thatrc.comparator);
        } else {
            return false;
        }
    }

}
