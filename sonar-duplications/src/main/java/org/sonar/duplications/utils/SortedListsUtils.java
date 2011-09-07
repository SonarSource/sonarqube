/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.utils;

import java.util.Comparator;
import java.util.List;

/**
 * Provides utility methods for sorted lists.
 */
public final class SortedListsUtils {

  /**
   * Returns true if all elements from second list also presented in first list.
   * Both lists must be sorted.
   * And both must implement {@link java.util.RandomAccess}, otherwise this method is inefficient in terms of performance.
   * Running time - O(|container| + |list|).
   */
  public static <T> boolean contains(List<T> container, List<T> list, Comparator<T> comparator) {
    int j = 0;
    for (int i = 0; i < list.size(); i++) {
      T e1 = list.get(i);
      boolean found = false;
      for (; j < container.size(); j++) {
        T e2 = container.get(j);
        int c = comparator.compare(e1, e2);
        if (c == 0) {
          found = true;
          break;
        } else if (c < 0) {
          // e1 is less than e2 - stop search, because all next elements e2 would be greater than e1
          return false;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }

  private SortedListsUtils() {
  }

}
