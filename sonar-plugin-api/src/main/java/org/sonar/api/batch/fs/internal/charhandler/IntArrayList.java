/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.fs.internal.charhandler;

import java.util.Arrays;
import java.util.Collection;

/**
 * Specialization of {@link java.util.ArrayList} to create a list of int (only append elements) and then produce an int[].
 */
class IntArrayList {

  /**
   * Default initial capacity.
   */
  private static final int DEFAULT_CAPACITY = 10;

  /**
   * Shared empty array instance used for default sized empty instances. We
   * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
   * first element is added.
   */
  private static final int[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

  /**
   * The array buffer into which the elements of the ArrayList are stored.
   * The capacity of the IntArrayList is the length of this array buffer. Any
   * empty IntArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
   * will be expanded to DEFAULT_CAPACITY when the first element is added.
   */
  private int[] elementData;

  /**
   * The size of the IntArrayList (the number of elements it contains).
   */
  private int size;

  /**
   * Constructs an empty list with an initial capacity of ten.
   */
  public IntArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
  }

  /**
   * Trims the capacity of this <tt>IntArrayList</tt> instance to be the
   * list's current size and return the internal array. An application can use this operation to minimize
   * the storage of an <tt>IntArrayList</tt> instance.
   */
  public int[] trimAndGet() {
    if (size < elementData.length) {
      elementData = Arrays.copyOf(elementData, size);
    }
    return elementData;
  }

  private void ensureCapacityInternal(int minCapacity) {
    int capacity = minCapacity;
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
      capacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }

    ensureExplicitCapacity(capacity);
  }

  private void ensureExplicitCapacity(int minCapacity) {
    if (minCapacity - elementData.length > 0) {
      grow(minCapacity);
    }
  }

  /**
   * Increases the capacity to ensure that it can hold at least the
   * number of elements specified by the minimum capacity argument.
   *
   * @param minCapacity the desired minimum capacity
   */
  private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity;
    }
    elementData = Arrays.copyOf(elementData, newCapacity);
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * @param e element to be appended to this list
   * @return <tt>true</tt> (as specified by {@link Collection#add})
   */
  public boolean add(int e) {
    ensureCapacityInternal(size + 1);
    elementData[size] = e;
    size++;
    return true;
  }

}
