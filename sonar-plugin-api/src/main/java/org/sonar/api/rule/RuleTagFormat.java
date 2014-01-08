/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.rule;

import org.apache.commons.lang.StringUtils;

/**
 * @since 4.2
 */
public class RuleTagFormat {

  private RuleTagFormat() {
    // only static methods
  }

  public static boolean isValid(String tag) {
    return StringUtils.isNotBlank(tag) && StringUtils.indexOf(tag, " ") < 0;
  }

  public static void validate(String tag) {
    if (!isValid(tag)) {
      throw new IllegalArgumentException(String.format("Whitespaces are not allowed in rule tags: '%s'", tag));
    }
  }
}
