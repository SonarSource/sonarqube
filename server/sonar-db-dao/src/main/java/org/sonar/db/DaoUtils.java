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
package org.sonar.db;

import java.util.regex.Pattern;

public class DaoUtils {
  private static final Pattern ESCAPE_PERCENT_AND_UNDERSCORE = Pattern.compile("[/%_]");

  private DaoUtils() {
    // prevent new instances
  }

  /**
   * Returns an escaped value in parameter, with the desired wildcards. Suitable to be used in a like sql query<br />
   * Escapes the "/", "%" and "_" characters.<br/>
   *
   * You <strong>must</strong> add "ESCAPE '/'" after your like query. It defines '/' as the escape character.
   */
  public static String buildLikeValue(String value, WildcardPosition wildcardPosition) {
    String escapedValue = escapePercentAndUnderscore(value);
    String wildcard = "%";
    switch (wildcardPosition) {
      case BEFORE:
        escapedValue = wildcard + escapedValue;
        break;
      case AFTER:
        escapedValue += wildcard;
        break;
      case BEFORE_AND_AFTER:
        escapedValue = wildcard + escapedValue + wildcard;
        break;
      default:
        throw new UnsupportedOperationException("Unhandled WildcardPosition: " + wildcardPosition);
    }

    return escapedValue;
  }

  /**
   * Replace escape percent and underscore by adding a slash just before
   */
  private static String escapePercentAndUnderscore(String value) {
    return ESCAPE_PERCENT_AND_UNDERSCORE.matcher(value)
      // $0 : Group zero, stands for the entire expression
      .replaceAll("/$0");
  }
}
