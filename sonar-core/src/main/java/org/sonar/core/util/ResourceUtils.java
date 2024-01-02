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
package org.sonar.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public final class ResourceUtils {
  private ResourceUtils() {
  }

  /**
   * Read a text resource from classpath.
   * The resolution of the resource is equivalent to {@link Class#getResource(String)}.
   *
   * @param relativeTo   class to base the resource path on
   * @param resourcePath classpath name to the resource
   * @return content of the resource
   * @throws IllegalStateException if the resource cannot be read
   */
  public static String readClasspathResource(Class<?> relativeTo, String resourcePath) {
    try {
      InputStream resourceStream = relativeTo.getResourceAsStream(resourcePath);
      if (resourceStream == null) {
        throw new IllegalStateException(getErrorMessage(relativeTo, resourcePath));
      }
      return IOUtils.toString(resourceStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(getErrorMessage(relativeTo, resourcePath), e);
    }
  }

  private static String getErrorMessage(Class<?> relativeTo, String resourcePath) {
    return String.format("Fail to read classpath resource: %s of class: %s", resourcePath, relativeTo.getPackageName());
  }
}
