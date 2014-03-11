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
package org.sonar.api.utils;

import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * @since 4.0
 */
public class PathUtils {

  PathUtils() {
    // only static methods
  }

  /**
   * Normalize path and replace file separators by forward slash
   */
  public static String sanitize(@Nullable String path) {
    return FilenameUtils.normalize(path, true);
  }

  /**
   * Get canonical path and replace file separators by forward slash. This
   * method does not throw boring checked exception.
   */
  public static String canonicalPath(@Nullable File file) {
    try {
      return file != null ? FilenameUtils.separatorsToUnix(file.getCanonicalPath()) : null;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to get the canonical path of " + file.getAbsolutePath(), e);
    }
  }
}
