/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import org.sonar.api.utils.DateUtils;

public class UtcDateUtils {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT).withZone(ZoneOffset.UTC);

  private UtcDateUtils() {
    // only static stuff
  }

  public static String formatDateTime(Date date) {
    return formatter.format(date.toInstant());
  }

  public static Date parseDateTime(String s) {
    try {
      return Date.from(OffsetDateTime.parse(s, formatter).toInstant());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Fail to parse date: " + s, e);
    }
  }
}
