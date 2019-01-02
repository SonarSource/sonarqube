/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

public class DataUtilsTest {

  @Test
  public void testSort() {
    int[] expected = new int[200];
    int[] actual = new int[expected.length];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (int) (Math.random() * 900);
      actual[i] = expected[i];
    }
    Arrays.sort(expected);
    DataUtils.sort(new SimpleSortable(actual, actual.length));
    assertThat(actual, equalTo(expected));
  }

  @Test
  public void testSearch() {
    int[] a = new int[] { 1, 2, 4, 4, 4, 5, 0 };
    SimpleSortable sortable = new SimpleSortable(a, a.length - 1);
    // search 4
    a[a.length - 1] = 4;
    assertThat(DataUtils.binarySearch(sortable), is(2));
    // search 5
    a[a.length - 1] = 5;
    assertThat(DataUtils.binarySearch(sortable), is(5));
    // search -5
    a[a.length - 1] = -5;
    assertThat(DataUtils.binarySearch(sortable), is(0));
    // search 10
    a[a.length - 1] = 10;
    assertThat(DataUtils.binarySearch(sortable), is(6));
    // search 3
    a[a.length - 1] = 3;
    assertThat(DataUtils.binarySearch(sortable), is(2));
  }

  class SimpleSortable implements DataUtils.Sortable {
    private final int[] a;
    private final int size;

    public SimpleSortable(int[] a, int size) {
      this.a = a;
      this.size = size;
    }

    public int size() {
      return size;
    }

    public void swap(int i, int j) {
      int tmp = a[i];
      a[i] = a[j];
      a[j] = tmp;
    }

    public boolean isLess(int i, int j) {
      return a[i] < a[j];
    }
  }

}
