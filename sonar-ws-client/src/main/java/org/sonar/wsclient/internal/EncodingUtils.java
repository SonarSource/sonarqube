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
package org.sonar.wsclient.internal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Not an API. Please do not use this class, except maybe for unit tests.
 */
public class EncodingUtils {

  private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
  private static final String DATE_FORMAT = "yyyy-MM-dd";

  private EncodingUtils() {
    // only static methods
  }

  public static Map<String, Object> toMap(String... array) {
    if (array.length%2==1) {
      throw new IllegalArgumentException("Not an even number of arguments");
    }
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < array.length; i += 2) {
      Object value = array[i + 1];
      if (value != null) {
        map.put(array[i], array[i + 1]);
      }
    }
    return map;
  }

  public static String toQueryParam(String[] strings) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String string : strings) {
      if (!first) {
        sb.append(',');
      }
      sb.append(string);
      first = false;
    }
    return sb.toString();
  }

  public static String toQueryParam(Date d, boolean includeTime) {
    String format = includeTime ? DATETIME_FORMAT : DATE_FORMAT;
    SimpleDateFormat dateFormat = new SimpleDateFormat(format);
    return dateFormat.format(d);
  }
}
