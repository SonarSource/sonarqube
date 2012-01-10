/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Java;

public final class FindbugsAntConverter {

  private FindbugsAntConverter() {
  }

  /**
   * Convert the exclusion ant pattern to a java regexp accepted by findbugs
   * exclusion file
   *
   * @param exclusion ant pattern to convert
   * @return Exclusion pattern for findbugs
   */
  public static String antToJavaRegexpConvertor(String exclusion) {
    StringBuilder builder = new StringBuilder("~");
    int offset = 0;
    // First **/ or */ is optional
    if (exclusion.startsWith("**/")) {
      builder.append("(.*\\.)?");
      offset += 3;
    } else if (exclusion.startsWith("*/")) {
      builder.append("([^\\\\^\\s]*\\.)?");
      offset += 2;
    }
    for (String suffix : Java.SUFFIXES) {
      exclusion = StringUtils.removeEndIgnoreCase(exclusion, "." + suffix);
    }

    char[] array = exclusion.toCharArray();
    for (int i = offset; i < array.length; i++) {
      char c = array[i];
      if (c == '?') {
        builder.append('.');
      } else if (c == '*') {
        if (i + 1 < array.length && array[i + 1] == '*') {
          builder.append(".*");
          i++;
        } else {
          builder.append("[^\\\\^\\s]*");
        }
      } else if (c == '/') {
        builder.append("\\.");
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }
}
