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
package org.sonar.duplications.index;

public final class DataUtils {

  public interface Sortable {

    /**
     * @return the number of elements.
     */
    int size();

    /**
     * Swaps elements in positions i and j.
     */
    void swap(int i, int j);

    /**
     * @return true if element in position i less than in position j.
     */
    boolean isLess(int i, int j);

  }

  /**
   * Value for search must be stored in position {@link Sortable#size() size}.
   */
  public static int binarySearch(Sortable data) {
    int value = data.size();
    int lower = 0;
    int upper = data.size();
    while (lower < upper) {
      int mid = (lower + upper) >> 1;
      if (data.isLess(mid, value)) {
        lower = mid + 1;
      } else {
        upper = mid;
      }
    }
    return lower;
  }

  public static void sort(Sortable data) {
    quickSort(data, 0, data.size() - 1);
  }

  private static void bubbleSort(Sortable data, int left, int right) {
    for (int i = right; i > left; i--) {
      for (int j = left; j < i; j++) {
        if (data.isLess(j + 1, j)) {
          data.swap(j, j + 1);
        }
      }
    }
  }

  private static int partition(Sortable data, int i, int j) {
    // can be selected randomly
    int pivot = i + (j - i) / 2;
    while (i <= j) {
      while (data.isLess(i, pivot)) {
        i++;
      }
      while (data.isLess(pivot, j)) {
        j--;
      }
      if (i <= j) {
        data.swap(i, j);
        if (i == pivot) {
          pivot = j;
        } else if (j == pivot) {
          pivot = i;
        }
        i++;
        j--;
      }
    }
    return i;
  }

  private static void quickSort(Sortable data, int low, int high) {
    if (high - low < 5) {
      bubbleSort(data, low, high);
      return;
    }
    int i = partition(data, low, high);
    if (low < i - 1) {
      quickSort(data, low, i - 1);
    }
    if (i < high) {
      quickSort(data, i, high);
    }
  }

  private DataUtils() {
  }

}
