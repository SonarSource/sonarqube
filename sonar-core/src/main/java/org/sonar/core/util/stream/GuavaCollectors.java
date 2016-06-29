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
import java.util.stream.Collector;

public final class GuavaCollectors {
  private GuavaCollectors() {
    // prevents instantiation
  }

  /**
   * A Collector into an {@link ImmutableList}.
   */
  public static <T> Collector<T, List<T>, ImmutableList<T>> toList() {
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
  public static <T> Collector<T, List<T>, ImmutableList<T>> toList(int expectedSize) {
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
  public static <T> Collector<T, Set<T>, ImmutableSet<T>> toSet() {
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
  public static <T> Collector<T, Set<T>, ImmutableSet<T>> toSet(int expectedSize) {
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

}
