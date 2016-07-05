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
package org.sonar.scanner.issue.ignore.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.SonarException;

import java.util.List;

public class PatternDecoder {

  private static final int THREE_FIELDS_PER_LINE = 3;
  private static final String LINE_RANGE_REGEXP = "\\[((\\d+|\\d+-\\d+),?)*\\]";
  private static final String CONFIG_FORMAT_ERROR_PREFIX = "Exclusions > Issues : Invalid format. ";

  public List<IssuePattern> decode(String patternsList) {
    List<IssuePattern> patterns = Lists.newLinkedList();
    String[] patternsLines = StringUtils.split(patternsList, "\n");
    for (String patternLine : patternsLines) {
      IssuePattern pattern = decodeLine(patternLine.trim());
      if (pattern != null) {
        patterns.add(pattern);
      }
    }
    return patterns;
  }

  /**
   * Main method that decodes a line which defines a pattern
   */
  public IssuePattern decodeLine(String line) {
    if (isBlankOrComment(line)) {
      return null;
    }

    String[] fields = StringUtils.splitPreserveAllTokens(line, ';');
    if (fields.length > THREE_FIELDS_PER_LINE) {
      throw new SonarException(CONFIG_FORMAT_ERROR_PREFIX + "The following line has more than 3 fields separated by comma: " + line);
    }

    IssuePattern pattern;
    if (fields.length == THREE_FIELDS_PER_LINE) {
      checkRegularLineConstraints(line, fields);
      pattern = new IssuePattern(StringUtils.trim(fields[0]), StringUtils.trim(fields[1]));
      decodeRangeOfLines(pattern, fields[2]);
    } else if (fields.length == 2) {
      checkDoubleRegexpLineConstraints(line, fields);
      pattern = new IssuePattern().setBeginBlockRegexp(fields[0]).setEndBlockRegexp(fields[1]);
    } else {
      checkWholeFileRegexp(fields[0]);
      pattern = new IssuePattern().setAllFileRegexp(fields[0]);
    }

    return pattern;
  }

  static void checkRegularLineConstraints(String line, String[] fields) {
    if (!isResource(fields[0])) {
      throw new SonarException(CONFIG_FORMAT_ERROR_PREFIX + "The first field does not define a resource pattern: " + line);
    }
    if (!isRule(fields[1])) {
      throw new SonarException(CONFIG_FORMAT_ERROR_PREFIX + "The second field does not define a rule pattern: " + line);
    }
    if (!isLinesRange(fields[2])) {
      throw new SonarException(CONFIG_FORMAT_ERROR_PREFIX + "The third field does not define a range of lines: " + line);
    }
  }

  static void checkDoubleRegexpLineConstraints(String line, String[] fields) {
    if (!isRegexp(fields[0])) {
      throw new SonarException(CONFIG_FORMAT_ERROR_PREFIX + "The first field does not define a regular expression: " + line);
    }
    // As per configuration help, missing second field means: from start regexp to EOF
  }

  static void checkWholeFileRegexp(String regexp) {
    if (!isRegexp(regexp)) {
      throw new SonarException(CONFIG_FORMAT_ERROR_PREFIX + "The field does not define a regular expression: " + regexp);
    }
  }

  public static void decodeRangeOfLines(IssuePattern pattern, String field) {
    if (StringUtils.equals(field, "*")) {
      pattern.setCheckLines(false);
    } else {
      pattern.setCheckLines(true);
      String s = StringUtils.substringBetween(StringUtils.trim(field), "[", "]");
      String[] parts = StringUtils.split(s, ',');
      for (String part : parts) {
        if (StringUtils.contains(part, '-')) {
          String[] range = StringUtils.split(part, '-');
          pattern.addLineRange(Integer.valueOf(range[0]), Integer.valueOf(range[1]));
        } else {
          pattern.addLine(Integer.valueOf(part));
        }
      }
    }
  }

  @VisibleForTesting
  static boolean isLinesRange(String field) {
    return StringUtils.equals(field, "*") || java.util.regex.Pattern.matches(LINE_RANGE_REGEXP, field);
  }

  @VisibleForTesting
  static boolean isBlankOrComment(String line) {
    return StringUtils.isBlank(line) ^ StringUtils.startsWith(line, "#");
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
