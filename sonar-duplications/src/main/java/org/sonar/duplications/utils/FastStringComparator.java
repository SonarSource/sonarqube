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
package org.sonar.duplications.utils;

import java.util.Comparator;

/**
 * More efficient (in terms of performance) implementation of a String comparator.
 * Speed is gained by using hash code as a primary comparison attribute, which is cached for String.
 * Be aware that this ordering is not lexicographic, however stable.
 */
public final class FastStringComparator implements Comparator<String> {

  public static final FastStringComparator INSTANCE = new FastStringComparator();

  /**
   * Compares two strings (not lexicographically).
   */
  @Override
  public int compare(String s1, String s2) {
    if (s1 == s2) { // NOSONAR false-positive: Compare Objects With Equals
      return 0;
    }
    int h1 = s1.hashCode();
    int h2 = s2.hashCode();
    if (h1 < h2) {
      return -1;
    } else if (h1 > h2) {
      return 1;
    } else {
      return s1.compareTo(s2);
    }
  }

  /**
   * Enforce use of a singleton instance.
   */
  private FastStringComparator() {
  }

}
