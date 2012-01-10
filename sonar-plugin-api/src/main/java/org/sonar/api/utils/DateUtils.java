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
package org.sonar.api.utils;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Parses and formats <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a> dates.
 * This class is thread-safe.
 *
 * @since 2.7
 */
public final class DateUtils {
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

  private static final ThreadSafeDateFormat THREAD_SAFE_DATE_FORMAT = new ThreadSafeDateFormat(DATE_FORMAT);
  private static final ThreadSafeDateFormat THREAD_SAFE_DATETIME_FORMAT = new ThreadSafeDateFormat(DATETIME_FORMAT);

  private DateUtils() {
  }

  public static String formatDate(Date d) {
    return THREAD_SAFE_DATE_FORMAT.format(d);
  }

  public static String formatDateTime(Date d) {
    return THREAD_SAFE_DATETIME_FORMAT.format(d);
  }

  /**
   * @param s string in format {@link #DATE_FORMAT}
   * @throws SonarException when string cannot be parsed
   */
  public static Date parseDate(String s) {
    ParsePosition pos = new ParsePosition(0);
    Date result = THREAD_SAFE_DATE_FORMAT.parse(s, pos);
    if (pos.getIndex() != s.length()) {
      throw new SonarException("The date '" + s + "' does not respect format '" + DATE_FORMAT + "'");
    }
    return result;
  }

  /**
   * @param s string in format {@link #DATETIME_FORMAT}
   * @throws SonarException when string cannot be parsed
   */
  public static Date parseDateTime(String s) {
    ParsePosition pos = new ParsePosition(0);
    Date result = THREAD_SAFE_DATETIME_FORMAT.parse(s, pos);
    if (pos.getIndex() != s.length()) {
      throw new SonarException("The date '" + s + "' does not respect format '" + DATETIME_FORMAT + "'");
    }
    return result;
  }

  static class ThreadSafeDateFormat extends DateFormat {
    private static final long serialVersionUID = -8856468429474634301L;
    private final String format;

    ThreadSafeDateFormat(String format) {
      this.format = format;
    }

    private final transient ThreadLocal<Reference<DateFormat>> cache = new ThreadLocal<Reference<DateFormat>>() {
      public Reference<DateFormat> get() {
        Reference<DateFormat> softRef = super.get();
        if (softRef == null || softRef.get() == null) {
          softRef = new SoftReference<DateFormat>(new SimpleDateFormat(format));
          super.set(softRef);
        }
        return softRef;
      }
    };

    private DateFormat getDateFormat() {
      return (DateFormat) ((Reference) cache.get()).get();
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
      return getDateFormat().format(date, toAppendTo, fieldPosition);
    }

    public Date parse(String source, ParsePosition pos) {
      return getDateFormat().parse(source, pos);
    }
  }
}
