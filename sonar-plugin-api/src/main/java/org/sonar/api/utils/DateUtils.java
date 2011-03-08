/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @since 2.7
 */
public final class DateUtils {
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  /**
   * This method is not optimized. The DateFormat instance is created each time. Please use it sporadically.
   */
  public static String formatDate(Date d) {
    return new SimpleDateFormat(DATE_FORMAT).format(d);
  }

  /**
   * This method is not optimized. The DateFormat instance is created each time. Please use it sporadically.
   */
  public static String formatDateTime(Date d) {
    return new SimpleDateFormat(DATETIME_FORMAT).format(d);
  }

  /**
   * This method is not optimized. The DateFormat instance is created each time. Please use it sporadically.
   */
  public static Date parseDate(String s) {
    return parse(s, DATE_FORMAT);
  }

  /**
   * This method is not optimized. The DateFormat instance is created each time. Please use it sporadically.
   */
  public static Date parseDateTime(String s) {
    return parse(s, DATETIME_FORMAT);
  }

  private static Date parse(String s, String format) {
    try {
      return new SimpleDateFormat(format).parse(s);

    } catch (ParseException e) {
      throw new SonarException("The date '" + s + "' does not respect format '" + format + "'");
    }
  }
}
