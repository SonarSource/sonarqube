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
 * A {@link Comparator Comparator} that compares 
 * {@link Comparable Comparable} objects.
 * <p />
 * This Comparator is useful, for example,
 * for enforcing the natural order in custom implementations
 * of SortedSet and SortedMap.
 * <p />
 * Note: In the 2.0 and 2.1 releases of Commons Collections, 
 * this class would throw a {@link ClassCastException} if
 * either of the arguments to {@link #compare(Object, Object) compare}
 * were <code>null</code>, not {@link Comparable Comparable},
 * or for which {@link Comparable#compareTo(Object) compareTo} gave
 * inconsistent results.  This is no longer the case.  See
 * {@link #compare(Object, Object) compare} for details.
 *
 * @since Commons Collections 2.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 *
 * @author Henri Yandell
 *
 * @see java.util.Collections#reverseOrder()
 */
public class ComparableComparator implements Comparator, Serializable {

    /** Serialization version. */
    private static final long serialVersionUID=-291439688585137865L;

    /** The singleton instance. */
    private static final ComparableComparator instance = new ComparableComparator();

    //-----------------------------------------------------------------------
    /**
     * Gets the singleton instance of a ComparableComparator.
     * <p>
     * Developers are encouraged to use the comparator returned from this method
     * instead of constructing a new instance to reduce allocation and GC overhead
     * when multiple comparable comparators may be used in the same VM.
     * 
     * @return the singleton ComparableComparator
     */
    public static ComparableComparator getInstance() {
        return instance;
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor whose use should be avoided.
     * <p>
     * Please use the {@link #getInstance()} method whenever possible.
     */
    public ComparableComparator() {
        super();
    }

    //-----------------------------------------------------------------------
    /**
     * Compare the two {@link Comparable Comparable} arguments.
     * This method is equivalent to:
     * <pre>((Comparable)obj1).compareTo(obj2)</pre>
     * 
     * @param obj1  the first object to compare
     * @param obj2  the second object to compare
     * @return negative if obj1 is less, positive if greater, zero if equal
     * @throws NullPointerException when <i>obj1</i> is <code>null</code>, 
     *         or when <code>((Comparable)obj1).compareTo(obj2)</code> does
     * @throws ClassCastException when <i>obj1</i> is not a <code>Comparable</code>,
     *         or when <code>((Comparable)obj1).compareTo(obj2)</code> does
     */
    public int compare(Object obj1, Object obj2) {
        return ((Comparable)obj1).compareTo(obj2);
    }

    //-----------------------------------------------------------------------
    /**
     * Implement a hash code for this comparator that is consistent with
     * {@link #equals(Object) equals}.
     *
     * @return a hash code for this comparator.
     * @since Commons Collections 3.0
     */
    public int hashCode() {
        return "ComparableComparator".hashCode();
    }

    /**
     * Returns <code>true</code> iff <i>that</i> Object is 
     * is a {@link Comparator Comparator} whose ordering is 
     * known to be equivalent to mine.
     * <p>
     * This implementation returns <code>true</code>
     * iff <code><i>object</i>.{@link Object#getClass() getClass()}</code>
     * equals <code>this.getClass()</code>.
     * Subclasses may want to override this behavior to remain consistent
     * with the {@link Comparator#equals(Object)} contract.
     * 
     * @param object  the object to compare with
     * @return true if equal
     * @since Commons Collections 3.0
     */
    public boolean equals(Object object) {
        return (this == object) || 
               ((null != object) && (object.getClass().equals(this.getClass())));
    }

}
