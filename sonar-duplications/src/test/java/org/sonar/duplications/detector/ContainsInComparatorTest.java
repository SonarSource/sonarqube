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
package org.sonar.duplications.detector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.Comparator;

import org.junit.Test;
import org.sonar.duplications.index.ClonePart;

public class ContainsInComparatorTest {

  @Test
  public void shouldCompareByResourceId() {
    Comparator<ClonePart> comparator = ContainsInComparator.RESOURCE_ID_COMPARATOR;
    assertThat(comparator.compare(newClonePart("a", 0), newClonePart("b", 0)), is(-1));
    assertThat(comparator.compare(newClonePart("b", 0), newClonePart("a", 0)), is(1));
    assertThat(comparator.compare(newClonePart("a", 0), newClonePart("a", 0)), is(0));
  }

  @Test
  public void shouldCompareByResourceIdAndUnitStart() {
    Comparator<ClonePart> comparator = ContainsInComparator.CLONEPART_COMPARATOR;
    assertThat(comparator.compare(newClonePart("a", 0), newClonePart("b", 0)), is(-1));
    assertThat(comparator.compare(newClonePart("b", 0), newClonePart("a", 0)), is(1));

    assertThat(comparator.compare(newClonePart("a", 0), newClonePart("a", 0)), is(0));
    assertThat(comparator.compare(newClonePart("a", 0), newClonePart("a", 1)), is(-1));
    assertThat(comparator.compare(newClonePart("a", 1), newClonePart("a", 0)), is(1));
  }

  @Test
  public void shouldCompare() {
    assertThat(compare("a", 0, 0, "b", 0, 0), is(-1));
    assertThat(compare("b", 0, 0, "a", 0, 0), is(1));

    assertThat(compare("a", 0, 0, "a", 0, 0), is(0));
    assertThat(compare("a", 1, 0, "a", 0, 0), is(1));
    assertThat(compare("a", 0, 0, "a", 0, 1), is(-1));
  }

  private static int compare(String resourceId1, int start1, int end1, String resourceId2, int start2, int end2) {
    return new ContainsInComparator(end1 - start1, end2 - start2)
        .compare(newClonePart(resourceId1, start1), newClonePart(resourceId2, start2));
  }

  private static ClonePart newClonePart(String resourceId, int unitStart) {
    return new ClonePart(resourceId, unitStart, 0, 0);
  }

}
