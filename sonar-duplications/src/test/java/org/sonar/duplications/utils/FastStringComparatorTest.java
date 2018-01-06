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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FastStringComparatorTest {
  static int compare(String left, String right) {
    return FastStringComparator.INSTANCE.compare(left, right);
  }

  @Test
  public void sameHashCode() {
    // Next two Strings have same hash code in Java - see http://www.drmaciver.com/2008/07/javalangstringhashcode/
    String s1 = "Od";
    String s2 = "PE";

    assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    assertThat(compare(s1, s2)).isLessThan(0);
    assertThat(compare(s2, s1)).isGreaterThan(0);
  }

  @Test
  public void differentHashCode() {
    String s1 = "a";
    String s2 = "c";

    assertThat(s1.hashCode()).isNotEqualTo(s2.hashCode());
    assertThat(compare(s1, s2)).isEqualTo(-1);
    assertThat(compare(s2, s1)).isEqualTo(1);
  }

  @Test
  public void sameObject() {
    String s1 = "a";
    String s2 = s1;

    assertThat(s1).isSameAs(s2);
    assertThat(compare(s1, s2)).isZero();
    assertThat(compare(s1, s2)).isZero();
  }

  @Test
  public void sameString() {
    String s1 = new String("a");
    String s2 = new String("a");

    assertThat(s1).isNotSameAs(s2);
    assertThat(compare(s1, s2)).isZero();
    assertThat(compare(s1, s2)).isZero();
  }
}
