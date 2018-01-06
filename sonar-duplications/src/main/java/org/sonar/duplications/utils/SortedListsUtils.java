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
package org.sonar.duplications.utils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Provides utility methods for sorted lists.
 */
public final class SortedListsUtils {

  /**
   * Returns true if {@code container} contains all elements from {@code list}.
   * Both lists must be sorted in consistency with {@code comparator},
   * that is for any two sequential elements x and y:
   * {@code (comparator.compare(x, y) <= 0) && (comparator.compare(y, x) >= 0)}.
   * Running time - O(|container| + |list|).
   */
  public static <T> boolean contains(List<T> container, List<T> list, Comparator<T> comparator) {
    Iterator<T> listIterator = list.iterator();
    Iterator<T> containerIterator = container.iterator();
    T listElement = listIterator.next();
    T containerElement = containerIterator.next();
    while (true) {
      int r = comparator.compare(containerElement, listElement);
      if (r == 0) {
        // current element from list is equal to current element from container
        if (!listIterator.hasNext()) {
          // no elements remaining in list - all were matched
          return true;
        }
        // next element from list also can be equal to current element from container
        listElement = listIterator.next();
      } else if (r < 0) {
        // current element from list is greater than current element from container
        // need to check next element from container
        if (!containerIterator.hasNext()) {
          return false;
        }
        containerElement = containerIterator.next();
      } else {
        // current element from list is less than current element from container
        // stop search, because current element from list would be less than any next element from container
        return false;
      }
    }
  }

  private SortedListsUtils() {
  }

}
