/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.utils;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Parses and formats <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html#rfc822timezone">RFC 822</a> dates.
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

  public static String formatDateTime(long ms) {
    return THREAD_SAFE_DATETIME_FORMAT.format(new Date(ms));
  }

  public static String formatDateTimeNullSafe(@Nullable Date date) {
    return date == null ? "" : THREAD_SAFE_DATETIME_FORMAT.format(date);
  }

  @CheckForNull
  public static Date longToDate(@Nullable Long time) {
    return time == null ? null : new Date(time);
  }

  @CheckForNull
  public static Long dateToLong(@Nullable Date date) {
    return date == null ? null : date.getTime();
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
   * Parse format {@link #DATE_FORMAT}. This method never throws exception.
   *
   * @param s any string
   * @return the date, {@code null} if parsing error or if parameter is {@code null}
   * @since 3.0
   */
  @CheckForNull
  public static Date parseDateQuietly(@Nullable String s) {
    Date date = null;
    if (s != null) {
      try {
        date = parseDate(s);
      } catch (RuntimeException e) {
        // ignore
      }

    }
    return date;
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

  /**
   * Parse format {@link #DATETIME_FORMAT}. This method never throws exception.
   *
   * @param s any string
   * @return the datetime, {@code null} if parsing error or if parameter is {@code null}
   */
  @CheckForNull
  public static Date parseDateTimeQuietly(@Nullable String s) {
    Date datetime = null;
    if (s != null) {
      try {
        datetime = parseDateTime(s);
      } catch (RuntimeException e) {
        // ignore
      }

    }
    return datetime;
  }

  /**
   * @throws IllegalArgumentException if stringDate is not a correctly formed date or datetime
   * @return the datetime, {@code null} if stringDate is null
   * @since 6.1
   */
  @CheckForNull
  public static Date parseDateOrDateTime(@Nullable String stringDate) {
    if (stringDate == null) {
      return null;
    }

    Date date = parseDateTimeQuietly(stringDate);
    if (date != null) {
      return date;
    }

    date = parseDateQuietly(stringDate);
    checkArgument(date != null, "Date '%s' cannot be parsed as either a date or date+time", stringDate);

    return date;
  }

  /**
   * @see #parseDateOrDateTime(String) 
   */
  @CheckForNull
  public static Date parseStartingDateOrDateTime(@Nullable String stringDate) {
    return parseDateOrDateTime(stringDate);
  }

  /**
   * Return the datetime if @param stringDate is a datetime, date + 1 day if stringDate is a date.
   * So '2016-09-01' would return a date equivalent to '2016-09-02T00:00:00+0000' in GMT
   * @see #parseDateOrDateTime(String)
   * @throws IllegalArgumentException if stringDate is not a correctly formed date or datetime
   * @return the datetime, {@code null} if stringDate is null
   * @since 6.1
   */
  @CheckForNull
  public static Date parseEndingDateOrDateTime(@Nullable String stringDate) {
    if (stringDate == null) {
      return null;
    }

    Date date = parseDateTimeQuietly(stringDate);
    if (date != null) {
      return date;
    }

    date = parseDateQuietly(stringDate);
    checkArgument(date != null, "Date '%s' cannot be parsed as either a date or date+time", stringDate);

    return addDays(date, 1);
  }

  /**
   * Adds a number of days to a date returning a new object.
   * The original date object is unchanged.
   *
   * @param date  the date, not null
   * @param numberOfDays  the amount to add, may be negative
   * @return the new date object with the amount added
   */
  public static Date addDays(Date date, int numberOfDays) {
    return org.apache.commons.lang.time.DateUtils.addDays(date, numberOfDays);
  }

  static class ThreadSafeDateFormat extends DateFormat {
    private final String format;
    private final ThreadLocal<Reference<DateFormat>> cache = new ThreadLocal<Reference<DateFormat>>() {
      @Override
      public Reference<DateFormat> get() {
        Reference<DateFormat> softRef = super.get();
        if (softRef == null || softRef.get() == null) {
          SimpleDateFormat sdf = new SimpleDateFormat(format);
          sdf.setLenient(false);
          softRef = new SoftReference<>(sdf);
          super.set(softRef);
        }
        return softRef;
      }
    };

    ThreadSafeDateFormat(String format) {
      this.format = format;
    }

    private DateFormat getDateFormat() {
      return cache.get().get();
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
      return getDateFormat().format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
      return getDateFormat().parse(source, pos);
    }

    private void readObject(ObjectInputStream ois) throws NotSerializableException {
      throw new NotSerializableException();
    }

    private void writeObject(ObjectOutputStream ois) throws NotSerializableException {
      throw new NotSerializableException();
    }
  }
}
