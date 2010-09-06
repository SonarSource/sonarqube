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
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class FormatUtils {
  public static final String DATE_PATTERN = "yyyy-MM-dd";
  public static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

  private FormatUtils() {
    // only static methods
  }

  public static Date toDate(String s, boolean includeTime) {
    String pattern = includeTime ? DATETIME_PATTERN : DATE_PATTERN;
    try {
      if (StringUtils.isNotBlank(s)) {
        return new SimpleDateFormat(pattern).parse(s);
      }
      return null;

    } catch (ParseException e) {
      throw new RuntimeException("The following value does not respect the date pattern " + pattern + ": " + s, e);
    }
  }

  public static String toString(Date d, boolean includeTime) {
    if (d != null) {
      return new SimpleDateFormat(includeTime ? DATETIME_PATTERN : DATE_PATTERN).format(d);
    }
    return null;
  }
}
