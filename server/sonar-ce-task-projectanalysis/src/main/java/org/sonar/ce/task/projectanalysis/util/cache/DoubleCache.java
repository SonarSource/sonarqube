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
package org.sonar.ce.task.projectanalysis.util.cache;

import javax.annotation.Nullable;

public class DoubleCache {
  private static final Double ONE = 1.0;
  private static final Double HUNDRED = 100.0;
  private static final Double ZERO = 0.0;

  private DoubleCache() {
    // static only
  }

  public static Double intern(@Nullable Double num) {
    if (ZERO.equals(num)) {
      return ZERO;
    }
    if (ONE.equals(num)) {
      return ONE;
    }
    if (HUNDRED.equals(num)) {
      return HUNDRED;
    }
    return num;
  }
}
