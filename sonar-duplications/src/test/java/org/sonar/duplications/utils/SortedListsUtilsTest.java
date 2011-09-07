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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

public class SortedListsUtilsTest {

  @Test
  public void testContains() {
    List<Integer> c1 = Arrays.asList(1, 2, 3);
    List<Integer> c2 = Arrays.asList(1, 2);
    List<Integer> c3 = Arrays.asList(1, 3);

    assertThat(SortedListsUtils.contains(c1, c1, IntegerComparator.INSTANCE), is(true));
    assertThat(SortedListsUtils.contains(c1, c2, IntegerComparator.INSTANCE), is(true));
    assertThat(SortedListsUtils.contains(c1, c3, IntegerComparator.INSTANCE), is(true));

    assertThat(SortedListsUtils.contains(c2, c1, IntegerComparator.INSTANCE), is(false));
    assertThat(SortedListsUtils.contains(c2, c2, IntegerComparator.INSTANCE), is(true));
    assertThat(SortedListsUtils.contains(c2, c3, IntegerComparator.INSTANCE), is(false));

    assertThat(SortedListsUtils.contains(c3, c1, IntegerComparator.INSTANCE), is(false));
    assertThat(SortedListsUtils.contains(c3, c2, IntegerComparator.INSTANCE), is(false));
    assertThat(SortedListsUtils.contains(c3, c3, IntegerComparator.INSTANCE), is(true));
  }

  private static class IntegerComparator implements Comparator<Integer> {
    public static final IntegerComparator INSTANCE = new IntegerComparator();

    public int compare(Integer o1, Integer o2) {
      return o1 - o2;
    }
  }

}
