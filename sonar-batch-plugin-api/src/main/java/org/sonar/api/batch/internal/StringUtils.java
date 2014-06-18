/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.api.internal;

/**
 * Copied from commons lang
 */
public class StringUtils {

  public static final String EMPTY = "";

  public static String trim(String str) {
    return str == null ? null : str.trim();
  }

  public static String removeStart(String str, String remove) {
    if (isEmpty(str) || isEmpty(remove)) {
      return str;
    }
    if (str.startsWith(remove)) {
      return str.substring(remove.length());
    }
    return str;
  }

  public static String removeEnd(String str, String remove) {
    if (isEmpty(str) || isEmpty(remove)) {
      return str;
    }
    if (str.endsWith(remove)) {
      return str.substring(0, str.length() - remove.length());
    }
    return str;
  }

  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  public static boolean startsWithIgnoreCase(String str, String prefix) {
    return startsWith(str, prefix, true);
  }

  private static boolean startsWith(String str, String prefix, boolean ignoreCase) {
    if (str == null || prefix == null) {
      return (str == null && prefix == null);
    }
    if (prefix.length() > str.length()) {
      return false;
    }
    return str.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
  }

  public static String substring(String str, int start) {
    if (str == null) {
      return null;
    }

    // handle negatives, which means last n characters
    if (start < 0) {
      start = str.length() + start; // remember start is negative
    }

    if (start < 0) {
      start = 0;
    }
    if (start > str.length()) {
      return EMPTY;
    }

    return str.substring(start);
  }

  public static boolean isBlank(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if ((Character.isWhitespace(str.charAt(i)) == false)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotBlank(String str) {
    return !StringUtils.isBlank(str);
  }

  public static String removeEndIgnoreCase(String str, String remove) {
    if (isEmpty(str) || isEmpty(remove)) {
      return str;
    }
    if (endsWithIgnoreCase(str, remove)) {
      return str.substring(0, str.length() - remove.length());
    }
    return str;
  }

  public static boolean endsWithIgnoreCase(String str, String suffix) {
    return endsWith(str, suffix, true);
  }

  private static boolean endsWith(String str, String suffix, boolean ignoreCase) {
    if (str == null || suffix == null) {
      return (str == null && suffix == null);
    }
    if (suffix.length() > str.length()) {
      return false;
    }
    int strOffset = str.length() - suffix.length();
    return str.regionMatches(ignoreCase, strOffset, suffix, 0, suffix.length());
  }

  public static String lowerCase(String str) {
    if (str == null) {
      return null;
    }
    return str.toLowerCase();
  }

}
