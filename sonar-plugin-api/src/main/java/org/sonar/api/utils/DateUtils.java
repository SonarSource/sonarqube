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

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
  private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATETIME_FORMAT);

  /**
   * This method is not thread-safe.
   */
  public static String formatDate(Date d) {
    return dateFormat.format(d);
  }

  /**
   * This method is not thread-safe.
   */
  public static String formatDateTime(Date d) {
    return dateTimeFormat.format(d);
  }

  /**
   * This method is not thread-safe.
   */
  public static Date parseDate(String s) {
    try {
      return dateFormat.parse(s);

    } catch (ParseException e) {
      throw new SonarException("The date '" + s + "' does not respect format '" + DATE_FORMAT + "'");
    }
  }

  /**
   * This method is not thread-safe.
   */
  public static Date parseDateTime(String s) {
    try {
      return dateTimeFormat.parse(s);

    } catch (ParseException e) {
      throw new SonarException("The date '" + s + "' does not respect format '" + DATETIME_FORMAT + "'");
    }
  }
}
