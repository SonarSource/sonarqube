/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.sonar.api.utils.DateUtils;

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

  public static Integer toInteger(@Nullable Object o) {
    if (o instanceof Integer) {
      return (Integer) o;
    }
    if (o instanceof Long) {
      return Ints.checkedCast((Long) o);
    }

    if (o instanceof String) {
      return Integer.parseInt((String) o);
    }
    return null;
  }

  public static Double toDouble(@Nullable Object o) {
    if (o instanceof Double) {
      return (Double) o;
    }
    if (o instanceof String) {
      return Double.parseDouble((String) o);
    }
    return null;
  }


  public static Date toDate(@Nullable Object o) {
    if (o instanceof Date) {
      return (Date) o;
    }
    if (o instanceof String) {
      return DateUtils.parseDateTime((String) o);
    }
    return null;
  }

  public static Boolean toBoolean(@Nullable Object o) {
    if (o instanceof Boolean) {
      return (Boolean) o;
    }
    if (o instanceof String) {
      return Boolean.parseBoolean((String) o);
    }
    return null;
  }
}
