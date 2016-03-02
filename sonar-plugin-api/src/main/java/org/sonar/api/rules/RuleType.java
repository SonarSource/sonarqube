/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Enums;
import com.google.common.collect.Lists;
import java.util.List;

import static java.lang.String.format;

public enum RuleType {
  CODE_SMELL(1), BUG(2), VULNERABILITY(3);

  public static final List<String> ALL_NAMES = Lists.transform(Lists.newArrayList(values()), Enums.stringConverter(RuleType.class).reverse());

  private final int dbConstant;

  RuleType(int dbConstant) {
    this.dbConstant = dbConstant;
  }

  public int getDbConstant() {
    return dbConstant;
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
