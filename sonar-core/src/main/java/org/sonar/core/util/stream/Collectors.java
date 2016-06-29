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
   * Delegates to {@link java.util.stream.Collectors#toList()}.
   */
  public static <T> Collector<T, ?, List<T>> toList() {
    return java.util.stream.Collectors.toList();
  }

  /**
   * Does {@code java.util.stream.Collectors.toCollection(() -> new ArrayList<>(size));} which is equivalent to
   * {@link #toList()} but avoiding array copies when the size of the resulting set is already known.
   *
   * @see java.util.stream.Collectors#toList()
   * @see java.util.stream.Collectors#toCollection(Supplier)
   */
  public static <T> Collector<T, ?, List<T>> toList(int size) {
    return java.util.stream.Collectors.toCollection(() -> new ArrayList<>(size));
  }

  /**
   * Delegates to {@link java.util.stream.Collectors#toSet()}.
   */
  public static <T> Collector<T, ?, Set<T>> toSet() {
    return java.util.stream.Collectors.toSet();
  }

  /**
   * Does {@code java.util.stream.Collectors.toCollection(() -> new HashSet<>(size));} which is equivalent to
   * {@link #toSet()} but avoiding array copies when the size of the resulting set is already known.
   *
   * @see java.util.stream.Collectors#toSet()
   * @see java.util.stream.Collectors#toCollection(Supplier)
   */
  public static <T> Collector<T, ?, Set<T>> toSet(int size) {
    return java.util.stream.Collectors.toCollection(() -> new HashSet<>(size));
  }
}
