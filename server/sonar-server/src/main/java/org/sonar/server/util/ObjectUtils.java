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

package org.sonar.server.util;

public class ObjectUtils {
  private ObjectUtils() {
    // static utility methods only
  }

  /**
   * Taken from http://commons.apache.org/proper/commons-lang/javadocs/api-3.1/src-html/org/apache/commons/lang3/ObjectUtils.html#line.119
   * 
   * <p>Returns the first value in the array which is not {@code null}.
    * If all the values are {@code null} or the array is {@code null}
    * or empty then {@code null} is returned.</p>
    *
    * <pre>
    * ObjectUtils.firstNonNull(null, null)      = null
    * ObjectUtils.firstNonNull(null, "")        = ""
    * ObjectUtils.firstNonNull(null, null, "")  = ""
    * ObjectUtils.firstNonNull(null, "zz")      = "zz"
    * ObjectUtils.firstNonNull("abc", *)        = "abc"
    * ObjectUtils.firstNonNull(null, "xyz", *)  = "xyz"
    * ObjectUtils.firstNonNull(Boolean.TRUE, *) = Boolean.TRUE
    * ObjectUtils.firstNonNull()                = null
    * </pre>
    *
    * @param <T> the component type of the array
    * @param values  the values to test, may be {@code null} or empty
    * @return the first value from {@code values} which is not {@code null},
    *  or {@code null} if there are no non-null values
    * @since 3.0
    */
  public static <T> T firstNonNull(T... values) {
    if (values != null) {
      for (T val : values) {
        if (val != null) {
          return val;
        }
      }
    }
    return null;
  }
}
