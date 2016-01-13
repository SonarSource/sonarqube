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
package org.sonar.db.version;

import com.google.common.base.CharMatcher;
import javax.annotation.Nullable;

import static com.google.common.base.CharMatcher.JAVA_LOWER_CASE;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class ColumnDefValidation {

  private ColumnDefValidation() {
    // Only static stuff here
  }

  public static String validateColumnName(@Nullable String columnName) {
    String name = requireNonNull(columnName, "Column name cannot be null");
    checkArgument(JAVA_LOWER_CASE.or(CharMatcher.anyOf("_")).or(CharMatcher.DIGIT).matchesAllOf(name),
      String.format("Column name should only contains lowercase and _ characters, got '%s'", columnName));
    return name;
  }
}
