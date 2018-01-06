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
package org.sonar.scanner.issue.ignore.pattern;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

public class PatternDecoder {
  private static final String LINE_RANGE_REGEXP = "\\[((\\d+|\\d+-\\d+),?)*\\]";
  private static final String CONFIG_FORMAT_ERROR_PREFIX = "Exclusions > Issues : Invalid format. ";

  private PatternDecoder() {
    // static only
  }
  
  static void checkRegularLineConstraints(String line, String[] fields) {
    if (!isResource(fields[0])) {
      throw new IllegalStateException(CONFIG_FORMAT_ERROR_PREFIX + "The first field does not define a resource pattern: " + line);
    }
    if (!isRule(fields[1])) {
      throw new IllegalStateException(CONFIG_FORMAT_ERROR_PREFIX + "The second field does not define a rule pattern: " + line);
    }
    if (!isLinesRange(fields[2])) {
      throw new IllegalStateException(CONFIG_FORMAT_ERROR_PREFIX + "The third field does not define a range of lines: " + line);
    }
  }

  static void checkDoubleRegexpLineConstraints(String line, String[] fields) {
    if (!isRegexp(fields[0])) {
      throw new IllegalStateException(CONFIG_FORMAT_ERROR_PREFIX + "The first field does not define a regular expression: " + line);
    }
    // As per configuration help, missing second field means: from start regexp to EOF
  }

  static void checkWholeFileRegexp(String regexp) {
    if (!isRegexp(regexp)) {
      throw new IllegalStateException(CONFIG_FORMAT_ERROR_PREFIX + "The field does not define a regular expression: " + regexp);
    }
  }

  public static Set<LineRange> decodeRangeOfLines(String field) {
    if (StringUtils.equals(field, "*")) {
      return Collections.emptySet();
    } else {
      Set<LineRange> lineRanges = new HashSet<>();
      
      String s = StringUtils.substringBetween(StringUtils.trim(field), "[", "]");
      String[] parts = StringUtils.split(s, ',');
      for (String part : parts) {
        if (StringUtils.contains(part, '-')) {
          String[] range = StringUtils.split(part, '-');
          lineRanges.add(new LineRange(Integer.valueOf(range[0]), Integer.valueOf(range[1])));
        } else {
          lineRanges.add(new LineRange(Integer.valueOf(part), Integer.valueOf(part)));
        }
      }
      return lineRanges;
    }
  }

  @VisibleForTesting
  static boolean isLinesRange(String field) {
    return StringUtils.equals(field, "*") || java.util.regex.Pattern.matches(LINE_RANGE_REGEXP, field);
  }

  @VisibleForTesting
  static boolean isResource(String field) {
    return StringUtils.isNotBlank(field);
  }

  @VisibleForTesting
  static boolean isRule(String field) {
    return StringUtils.isNotBlank(field);
  }

  @VisibleForTesting
  static boolean isRegexp(String field) {
    return StringUtils.isNotBlank(field);
  }
}
