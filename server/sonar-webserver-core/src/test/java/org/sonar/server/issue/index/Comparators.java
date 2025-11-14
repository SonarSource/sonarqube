/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.issue.index;

import java.util.Collection;
import java.util.Comparator;

public final class Comparators {

  /**
   * Prevent construction.
   */
  private Comparators() {
  }

  /**
   * Verify that a comparator is transitive.
   *
   * @param <T>        the type being compared
   * @param comparator the comparator to test
   * @param elements   the elements to test against
   * @throws AssertionError if the comparator is not transitive
   */
  public static <T> void verifyTransitivity(Comparator<T> comparator, Collection<T> elements) {
    for (T first : elements) {
      for (T second : elements) {
        int result1 = comparator.compare(first, second);
        int result2 = comparator.compare(second, first);
        if (result1 != -result2) {
          // Uncomment the following line to step through the failed case
          // comparator.compare(first, second);
          throw new AssertionError("compare(" + first + ", " + second + ") == " + result1 +
            " but swapping the parameters returns " + result2);
        }
      }
    }
    for (T first : elements) {
      for (T second : elements) {
        int firstGreaterThanSecond = comparator.compare(first, second);
        if (firstGreaterThanSecond <= 0)
          continue;
        for (T third : elements) {
          int secondGreaterThanThird = comparator.compare(second, third);
          if (secondGreaterThanThird <= 0)
            continue;
          int firstGreaterThanThird = comparator.compare(first, third);
          if (firstGreaterThanThird <= 0) {
            throw new AssertionError("compare(" + first + ", " + second + ") > 0, " +
              "compare(" + second + ", " + third + ") > 0, but compare(" + first + ", " + third + ") == " +
              firstGreaterThanThird);
          }
        }
      }
    }
  }

}
