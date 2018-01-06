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
package org.sonar.api.rules;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;

public enum RuleType {
  CODE_SMELL(1), BUG(2), VULNERABILITY(3);

  private static final Set<String> ALL_NAMES = unmodifiableSet(new LinkedHashSet<>(stream(values())
    .map(Enum::name)
    .collect(toList())));

  private final int dbConstant;

  RuleType(int dbConstant) {
    this.dbConstant = dbConstant;
  }

  public int getDbConstant() {
    return dbConstant;
  }

  public static Set<String> names() {
    return ALL_NAMES;
  }

  /**
   * Returns the enum constant of the specified DB column value.
   */
  public static RuleType valueOf(int dbConstant) {
    // iterating the array is fast-enough as size is small. No need for a map.
    for (RuleType type : values()) {
      if (type.getDbConstant() == dbConstant) {
        return type;
      }
    }
    throw new IllegalArgumentException(format("Unsupported type value : %d", dbConstant));
  }

}
