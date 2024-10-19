/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.impl.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class ScannerUtils {

  private ScannerUtils() {
  }

  /**
   * Clean provided string to remove chars that are not valid as file name.
   *
   * @param projectKey e.g. my:file
   */
  public static String cleanKeyForFilename(String projectKey) {
    String cleanKey = StringUtils.deleteWhitespace(projectKey);
    return StringUtils.replace(cleanKey, ":", "_");
  }

  public static String encodeForUrl(@Nullable String url) {
    try {
      return URLEncoder.encode(url == null ? "" : url, StandardCharsets.UTF_8.name());

    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Encoding not supported", e);
    }
  }

  public static String describe(Object o) {
    try {
      if (o.getClass().getMethod("toString").getDeclaringClass() != Object.class) {
        String str = o.toString();
        if (str != null) {
          return str;
        }
      }
    } catch (Exception e) {
      // fallback
    }

    return o.getClass().getName();
  }

  public static String pluralize(String str, int i) {
    if (i == 1) {
      return str;
    }
    return str + "s";
  }

}
