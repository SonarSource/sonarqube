/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.DateUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * @since 3.6
 */
public class RubyUtils {

  private RubyUtils() {
    // only static methods
  }

  @CheckForNull
  public static List<String> toStrings(@Nullable Object o) {
    List<String> result = null;
    if (o != null) {
      if (o instanceof List) {
        // assume that it contains only strings
        result = (List) o;
      } else if (o instanceof CharSequence) {
        result = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().split((CharSequence) o));
      }
    }
    return result;
  }

  @CheckForNull
  public static <E extends Enum<E>> List<E> toEnums(@Nullable Object o, Class<E> enumClass) {
    if (o == null) {
      return null;
    }
    List<E> result = Lists.newArrayList();
    if (o instanceof List) {
      for (String s : (List<String>) o) {
        result.add(Enum.valueOf(enumClass, s));
      }
    } else if (o instanceof CharSequence) {
      for (String s : Splitter.on(',').omitEmptyStrings().split((CharSequence) o)) {
        result.add(Enum.valueOf(enumClass, s));
      }
    } else {
      throw new IllegalArgumentException("Unsupported type: " + o.getClass());
    }
    return result;
  }

  @CheckForNull
  public static Integer toInteger(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Integer) {
      return (Integer) o;
    }
    if (o instanceof Long) {
      return Ints.checkedCast((Long) o);
    }
    if (o instanceof String) {
      if (StringUtils.isBlank((String) o)) {
        return null;
      }
      return Integer.parseInt((String) o);
    }
    throw new IllegalArgumentException("Unsupported type for integer: " + o.getClass());
  }

  @CheckForNull
  public static Double toDouble(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Double) {
      return (Double) o;
    }
    if (o instanceof Integer) {
      return ((Integer) o).doubleValue();
    }
    if (o instanceof Long) {
      return ((Long) o).doubleValue();
    }
    if (o instanceof String) {
      if (StringUtils.isBlank((String) o)) {
        return null;
      }
      return Double.parseDouble((String) o);
    }
    throw new IllegalArgumentException("Unsupported type for double: " + o.getClass());
  }

  @CheckForNull
  public static Date toDate(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Date) {
      return (Date) o;
    }
    if (o instanceof String) {
      if (StringUtils.isBlank((String) o)) {
        return null;
      }
      Date date = DateUtils.parseDateTimeQuietly((String) o);
      if (date != null) {
        return date;
      }
      return DateUtils.parseDate((String) o);
    }
    throw new IllegalArgumentException("Unsupported type for date: " + o.getClass());
  }

  @CheckForNull
  public static Boolean toBoolean(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Boolean) {
      return (Boolean) o;
    }
    if (o instanceof String) {
      if (StringUtils.isBlank((String) o)) {
        return null;
      }
      return Boolean.parseBoolean((String) o);
    }
    throw new IllegalArgumentException("Unsupported type for boolean: " + o.getClass());
  }

  @CheckForNull
  public static Long toLong(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Integer) {
      return ((Integer) o).longValue();
    }
    if (o instanceof Long) {
      return (Long) o;
    }
    if (o instanceof String) {
      if (StringUtils.isBlank((String) o)) {
        return null;
      }
      return Long.parseLong((String) o);
    }
    throw new IllegalArgumentException("Unsupported type for long: " + o.getClass());
  }
}
