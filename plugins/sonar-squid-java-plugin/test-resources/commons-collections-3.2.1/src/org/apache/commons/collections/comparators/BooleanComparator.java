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
 * A {@link Comparator} for {@link Boolean} objects that can sort either
 * true or false first.
 * <p>
 * @see #getTrueFirstComparator()
 * @see #getFalseFirstComparator()
 * @see #getBooleanComparator(boolean)
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Rodney Waldhoff
 */
public final class BooleanComparator implements Comparator, Serializable {

    /** Serialization version. */
    private static final long serialVersionUID = 1830042991606340609L;

    /** Constant "true first" reference. */
    private static final BooleanComparator TRUE_FIRST = new BooleanComparator(true);

    /** Constant "false first" reference. */
    private static final BooleanComparator FALSE_FIRST = new BooleanComparator(false);

    /** <code>true</code> iff <code>true</code> values sort before <code>false</code> values. */
    private boolean trueFirst = false;

    //-----------------------------------------------------------------------
    /**
     * Returns a BooleanComparator instance that sorts 
     * <code>true</code> values before <code>false</code> values.
     * <p />
     * Clients are encouraged to use the value returned from 
     * this method instead of constructing a new instance 
     * to reduce allocation and garbage collection overhead when
     * multiple BooleanComparators may be used in the same 
     * virtual machine.
     * 
     * @return the true first singleton BooleanComparator
     */
    public static BooleanComparator getTrueFirstComparator() {
        return TRUE_FIRST;
    }
    
    /**
     * Returns a BooleanComparator instance that sorts 
     * <code>false</code> values before <code>true</code> values.
     * <p />
     * Clients are encouraged to use the value returned from 
     * this method instead of constructing a new instance 
     * to reduce allocation and garbage collection overhead when
     * multiple BooleanComparators may be used in the same 
     * virtual machine.
     * 
     * @return the false first singleton BooleanComparator
     */
    public static BooleanComparator getFalseFirstComparator() {
        return FALSE_FIRST;
    }
        
    /**
     * Returns a BooleanComparator instance that sorts 
     * <code><i>trueFirst</i></code> values before 
     * <code>&#x21;<i>trueFirst</i></code> values.
     * <p />
     * Clients are encouraged to use the value returned from 
     * this method instead of constructing a new instance 
     * to reduce allocation and garbage collection overhead when
     * multiple BooleanComparators may be used in the same 
     * virtual machine.
     * 
     * @param trueFirst when <code>true</code>, sort 
     * <code>true</code> <code>Boolean</code>s before <code>false</code>
     * @return a singleton BooleanComparator instance
     */
    public static BooleanComparator getBooleanComparator(boolean trueFirst) {
        return trueFirst ? TRUE_FIRST : FALSE_FIRST;
    }

    //-----------------------------------------------------------------------
    /**
     * Creates a <code>BooleanComparator</code> that sorts
     * <code>false</code> values before <code>true</code> values.
     * <p>
     * Equivalent to {@link #BooleanComparator(boolean) BooleanComparator(false)}.
     * <p>
     * Please use the static factory instead whenever possible.
     */
    public BooleanComparator() {
        this(false);
    }

    /**
     * Creates a <code>BooleanComparator</code> that sorts
     * <code><i>trueFirst</i></code> values before 
     * <code>&#x21;<i>trueFirst</i></code> values.
     * <p>
     * Please use the static factories instead whenever possible.
     * 
     * @param trueFirst when <code>true</code>, sort 
     *  <code>true</code> boolean values before <code>false</code>
     */
    public BooleanComparator(boolean trueFirst) {
        this.trueFirst = trueFirst;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares two arbitrary Objects.
     * When both arguments are <code>Boolean</code>, this method is equivalent to 
     * {@link #compare(Boolean,Boolean) compare((Boolean)<i>obj1</i>,(Boolean)<i>obj2</i>)}.
     * When either argument is not a <code>Boolean</code>, this methods throws
     * a {@link ClassCastException}.
     * 
     * @param obj1  the first object to compare
     * @param obj2  the second object to compare
     * @return negative if obj1 is less, positive if greater, zero if equal
     * @throws ClassCastException when either argument is not <code>Boolean</code>
     */
    public int compare(Object obj1, Object obj2) {
        return compare((Boolean)obj1, (Boolean)obj2);
    }
    
    /**
     * Compares two non-<code>null</code> <code>Boolean</code> objects
     * according to the value of {@link #sortsTrueFirst()}.
     * 
     * @param b1  the first boolean to compare
     * @param b2  the second boolean to compare
     * @return negative if obj1 is less, positive if greater, zero if equal
     * @throws NullPointerException when either argument <code>null</code>
     */
    public int compare(Boolean b1, Boolean b2) {
        boolean v1 = b1.booleanValue();
        boolean v2 = b2.booleanValue();

        return (v1 ^ v2) ? ( (v1 ^ trueFirst) ? 1 : -1 ) : 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Implement a hash code for this comparator that is consistent with
     * {@link #equals(Object) equals}.
     *
     * @return a hash code for this comparator.
     */
    public int hashCode() {
        int hash = "BooleanComparator".hashCode();
        return trueFirst ? -1 * hash : hash;
    }

    /**
     * Returns <code>true</code> iff <i>that</i> Object is 
     * is a {@link Comparator} whose ordering is known to be 
     * equivalent to mine.
     * <p>
     * This implementation returns <code>true</code>
     * iff <code><i>that</i></code> is a {@link BooleanComparator} 
     * whose value of {@link #sortsTrueFirst()} is equal to mine.
     * 
     * @param object  the object to compare to
     * @return true if equal
     */
    public boolean equals(Object object) {
        return (this == object) || 
               ((object instanceof BooleanComparator) && 
                (this.trueFirst == ((BooleanComparator)object).trueFirst));
    }

    //-----------------------------------------------------------------------
    /**
     * Returns <code>true</code> iff
     * I sort <code>true</code> values before 
     * <code>false</code> values.  In other words,
     * returns <code>true</code> iff
     * {@link #compare(Boolean,Boolean) compare(Boolean.FALSE,Boolean.TRUE)}
     * returns a positive value.
     * 
     * @return the trueFirst flag
     */
    public boolean sortsTrueFirst() {
        return trueFirst;
    }

}
