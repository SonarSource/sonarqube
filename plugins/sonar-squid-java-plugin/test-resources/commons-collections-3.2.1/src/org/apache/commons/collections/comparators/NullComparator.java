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
 * A Comparator that will compare nulls to be either lower or higher than
 * other objects.
 *
 * @since Commons Collections 2.0
 * @version $Revision: 647116 $ $Date: 2008-04-11 12:23:08 +0100 (Fri, 11 Apr 2008) $ 
 *
 * @author Michael A. Smith
 */
public class NullComparator implements Comparator, Serializable {

    /** Serialization version. */
    private static final long serialVersionUID = -5820772575483504339L;

    /**
     *  The comparator to use when comparing two non-<code>null</code> objects.
     **/
    private Comparator nonNullComparator;

    /**
     *  Specifies whether a <code>null</code> are compared as higher than
     *  non-<code>null</code> objects.
     **/
    private boolean nullsAreHigh;

    //-----------------------------------------------------------------------
    /**
     *  Construct an instance that sorts <code>null</code> higher than any
     *  non-<code>null</code> object it is compared with. When comparing two
     *  non-<code>null</code> objects, the {@link ComparableComparator} is
     *  used.
     **/
    public NullComparator() {
        this(ComparableComparator.getInstance(), true);
    }

    /**
     *  Construct an instance that sorts <code>null</code> higher than any
     *  non-<code>null</code> object it is compared with.  When comparing two
     *  non-<code>null</code> objects, the specified {@link Comparator} is
     *  used.
     *
     *  @param nonNullComparator the comparator to use when comparing two
     *  non-<code>null</code> objects.  This argument cannot be
     *  <code>null</code>
     *
     *  @exception NullPointerException if <code>nonNullComparator</code> is
     *  <code>null</code>
     **/
    public NullComparator(Comparator nonNullComparator) {
        this(nonNullComparator, true);
    }

    /**
     *  Construct an instance that sorts <code>null</code> higher or lower than
     *  any non-<code>null</code> object it is compared with.  When comparing
     *  two non-<code>null</code> objects, the {@link ComparableComparator} is
     *  used.
     *
     *  @param nullsAreHigh a <code>true</code> value indicates that
     *  <code>null</code> should be compared as higher than a
     *  non-<code>null</code> object.  A <code>false</code> value indicates
     *  that <code>null</code> should be compared as lower than a
     *  non-<code>null</code> object.
     **/
    public NullComparator(boolean nullsAreHigh) {
        this(ComparableComparator.getInstance(), nullsAreHigh);
    }
    
    /**
     *  Construct an instance that sorts <code>null</code> higher or lower than
     *  any non-<code>null</code> object it is compared with.  When comparing
     *  two non-<code>null</code> objects, the specified {@link Comparator} is
     *  used.
     *
     *  @param nonNullComparator the comparator to use when comparing two
     *  non-<code>null</code> objects. This argument cannot be
     *  <code>null</code>
     *
     *  @param nullsAreHigh a <code>true</code> value indicates that
     *  <code>null</code> should be compared as higher than a
     *  non-<code>null</code> object.  A <code>false</code> value indicates
     *  that <code>null</code> should be compared as lower than a
     *  non-<code>null</code> object.
     *
     *  @exception NullPointerException if <code>nonNullComparator</code> is
     *  <code>null</code>
     **/
    public NullComparator(Comparator nonNullComparator, boolean nullsAreHigh) {
        this.nonNullComparator = nonNullComparator;
        this.nullsAreHigh = nullsAreHigh;
        
        if(nonNullComparator == null) {
            throw new NullPointerException("null nonNullComparator");
        }
    }

    //-----------------------------------------------------------------------
    /**
     *  Perform a comparison between two objects.  If both objects are
     *  <code>null</code>, a <code>0</code> value is returned.  If one object
     *  is <code>null</code> and the other is not, the result is determined on
     *  whether the Comparator was constructed to have nulls as higher or lower
     *  than other objects.  If neither object is <code>null</code>, an
     *  underlying comparator specified in the constructor (or the default) is
     *  used to compare the non-<code>null</code> objects.
     *
     *  @param o1  the first object to compare
     *  @param o2  the object to compare it to.
     *  @return <code>-1</code> if <code>o1</code> is "lower" than (less than,
     *  before, etc.) <code>o2</code>; <code>1</code> if <code>o1</code> is
     *  "higher" than (greater than, after, etc.) <code>o2</code>; or
     *  <code>0</code> if <code>o1</code> and <code>o2</code> are equal.
     **/
    public int compare(Object o1, Object o2) {
        if(o1 == o2) { return 0; }
        if(o1 == null) { return (this.nullsAreHigh ? 1 : -1); }
        if(o2 == null) { return (this.nullsAreHigh ? -1 : 1); }
        return this.nonNullComparator.compare(o1, o2);
    }

    //-----------------------------------------------------------------------
    /**
     *  Implement a hash code for this comparator that is consistent with
     *  {@link #equals(Object)}.
     *
     *  @return a hash code for this comparator.
     **/
    public int hashCode() {
        return (nullsAreHigh ? -1 : 1) * nonNullComparator.hashCode();
    }

    /**
     *  Determines whether the specified object represents a comparator that is
     *  equal to this comparator.
     *
     *  @param obj  the object to compare this comparator with.
     *
     *  @return <code>true</code> if the specified object is a NullComparator
     *  with equivalent <code>null</code> comparison behavior
     *  (i.e. <code>null</code> high or low) and with equivalent underlying
     *  non-<code>null</code> object comparators.
     **/
    public boolean equals(Object obj) {
        if(obj == null) { return false; }
        if(obj == this) { return true; }
        if(!obj.getClass().equals(this.getClass())) { return false; }

        NullComparator other = (NullComparator)obj;
    
        return ((this.nullsAreHigh == other.nullsAreHigh) &&
                (this.nonNullComparator.equals(other.nonNullComparator)));
    }

}
