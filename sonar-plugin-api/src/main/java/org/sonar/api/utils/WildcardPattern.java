/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.utils;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @since 1.10
 */
public class WildcardPattern {

  private static final Map<String, WildcardPattern> patterns = new HashMap<String, WildcardPattern>();

  private Pattern pattern;

  protected WildcardPattern(String pattern, String directorySeparator) {
    this.pattern = Pattern.compile(toRegexp(pattern, directorySeparator));
  }

  public boolean match(String value) {
    return pattern.matcher(removeSlahesToIgnore(value)).matches();
  }

  private String toRegexp(String wildcardPattern, String directorySeparator) {
    String patternStr = removeSlahesToIgnore(wildcardPattern);
    patternStr = StringUtils.replace(patternStr, "**/**", "**");
    patternStr = StringUtils.replace(patternStr, "**/", "|");
    patternStr = StringUtils.replace(patternStr, "/**", "|");
    patternStr = StringUtils.replace(patternStr, "**", "|");
    StringBuilder sb = new StringBuilder();

    for (char c : patternStr.toCharArray()) {
      switch (c) {
        case '|':
          sb.append(".*");
          break;
        case '*':
          sb.append("[^\\").append(directorySeparator).append("]*");
          break;
        case '?':
          sb.append("[^\\").append(directorySeparator).append("]");
          break;
        case '.':
          sb.append("\\.");
          break;
        case '/':
          sb.append("\\").append(directorySeparator);
          break;
        default:
          sb.append(c);
      }
    }

    return sb.toString();
  }

  private String removeSlahesToIgnore(String wildcardPattern) {
    String patternStr = StringUtils.removeStart(wildcardPattern, "/");
    return StringUtils.removeEnd(patternStr, "/");
  }

  public static WildcardPattern create(String pattern) {
    return create(pattern, "/");
  }

  public static WildcardPattern[] create(String[] patterns) {
    if (patterns==null) {
      return new WildcardPattern[0];
    }
    WildcardPattern[] exclusionPAtterns = new WildcardPattern[patterns.length];
    for (int i = 0; i < patterns.length; i++) {
      exclusionPAtterns[i] = create(patterns[i]);
    }
    return exclusionPAtterns;
  }

  public static WildcardPattern create(String pattern, String directorySeparator) {
    String key = pattern + directorySeparator;
    WildcardPattern wildcardPattern = patterns.get(key);
    if (wildcardPattern == null) {
      wildcardPattern = new WildcardPattern(pattern, directorySeparator);
      patterns.put(key, wildcardPattern);
    }
    return wildcardPattern;
  }
}
