/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.core.util.stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class Collectors {
  private Collectors() {
    // prevents instantiation
  }

  /**
   * A Collector into an {@link ImmutableList}.
   */
  public static <T> Collector<T, List<T>, List<T>> toList() {
    return Collector.of(
      ArrayList::new,
      List::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableList::copyOf);
  }

  /**
   * A Collector into an {@link ImmutableList} of the specified expected size.
   */
  public static <T> Collector<T, List<T>, List<T>> toList(int expectedSize) {
    // use ArrayList rather than ImmutableList.Builder because initial capacity of builder can not be specified
    return Collector.of(
      () -> new ArrayList<>(expectedSize),
      List::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableList::copyOf);
  }

  /**
   * A Collector into an {@link ImmutableSet}.
   */
  public static <T> Collector<T, Set<T>, Set<T>> toSet() {
    return Collector.of(
      HashSet::new,
      Set::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableSet::copyOf);
  }

  /**
   * A Collector into an {@link ImmutableSet} of the specified expected size.
   */
  public static <T> Collector<T, Set<T>, Set<T>> toSet(int expectedSize) {
    // use HashSet rather than ImmutableSet.Builder because initial capacity of builder can not be specified
    return Collector.of(
      () -> new HashSet<>(expectedSize),
      Set::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableSet::copyOf);
  }

  /**
   * Delegates to {@link java.util.stream.Collectors#toCollection(Supplier)}.
   */
  public static <T> Collector<T, ?, ArrayList<T>> toArrayList() {
    return java.util.stream.Collectors.toCollection(ArrayList::new);
  }

  /**
   * Does {@code java.util.stream.Collectors.toCollection(() -> new ArrayList<>(size));} which is equivalent to
   * {@link #toArrayList()} but avoiding array copies when the size of the resulting list is already known.
   *
   * @see java.util.stream.Collectors#toList()
   * @see java.util.stream.Collectors#toCollection(Supplier)
   */
  public static <T> Collector<T, ?, ArrayList<T>> toArrayList(int size) {
    return java.util.stream.Collectors.toCollection(() -> new ArrayList<>(size));
  }

  /**
   * Delegates to {@link java.util.stream.Collectors#toCollection(Supplier)}.
   */
  public static <T> Collector<T, ?, HashSet<T>> toHashSet() {
    return java.util.stream.Collectors.toCollection(HashSet::new);
  }

  /**
   * Does {@code java.util.stream.Collectors.toCollection(() -> new HashSet<>(size));} which is equivalent to
   * {@link #toHashSet()} but avoiding array copies when the size of the resulting set is already known.
   *
   * @see java.util.stream.Collectors#toSet()
   * @see java.util.stream.Collectors#toCollection(Supplier)
   */
  public static <T> Collector<T, ?, HashSet<T>> toHashSet(int size) {
    return java.util.stream.Collectors.toCollection(() -> new HashSet<>(size));
  }
}
