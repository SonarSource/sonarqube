/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;

/**
 * @since 4.0
 */
public class PathUtils {

  private PathUtils() {
    // only static methods
  }

  /**
   * Normalize path and replace file separators by forward slash
   */
  @CheckForNull
  public static String sanitize(@Nullable String path) {
    return FilenameUtils.normalize(path, true);
  }

  /**
   * Get canonical path and replace file separators by forward slash. This
   * method does not throw boring checked exception.
   */
  @CheckForNull
  public static String canonicalPath(@Nullable File file) {
    if (file == null) {
      return null;
    }
    try {
      return FilenameUtils.separatorsToUnix(file.getCanonicalPath());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to get the canonical path of " + file.getAbsolutePath(), e);
    }
  }
}
