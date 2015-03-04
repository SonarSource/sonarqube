/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.util;

import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import java.util.List;

/**
 * Utility class heavily inspired by Guava
 */
public class MultiSets {

  private static final Ordering<Multiset.Entry<?>> DECREASING_COUNT_ORDERING = new NonNullOrdering<Multiset.Entry<?>>() {
    @Override
    public int doCompare(Multiset.Entry<?> entry1, Multiset.Entry<?> entry2) {
      return Ints.compare(entry2.getCount(), entry1.getCount());
    }
  };

  private MultiSets() {

  }

  /**
   * Returns a copy of {@code multiset} as a {@link List} whose iteration order is
   * highest count first, with ties broken by the iteration order of the original multiset.
   */
  public static <E> List<Multiset.Entry<E>> listOrderedByHighestCounts(Multiset<E> multiset) {
    return DECREASING_COUNT_ORDERING.sortedCopy(multiset.entrySet());
  }

  private abstract static class NonNullOrdering<T> extends Ordering<T> {

    @Override
    public int compare(T left, T right) {
      return doCompare(left, right);
    }

    public abstract int doCompare(T left, T right);
  }
}
