/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @deprecated replaced by {@link org.sonar.api.scan.filesystem.ModuleFileSystem} and {@link org.sonar.api.scan.filesystem.PathResolver} in 3.5
 */
@Deprecated
public class DefaultProjectFileSystem {
  /**
   * getRelativePath("c:/foo/src/my/package/Hello.java", "c:/foo/src") is "my/package/Hello.java"
   *
   * @return null if file is not in dir (including recursive subdirectories)
   */
  public static String getRelativePath(java.io.File file, java.io.File dir) {
    return getRelativePath(file, Arrays.asList(dir));
  }

  /**
   * getRelativePath("c:/foo/src/my/package/Hello.java", ["c:/bar", "c:/foo/src"]) is "my/package/Hello.java".
   * <p>
   * Relative path is composed of slashes. Windows backslaches are replaced by /
   * </p>
   *
   * @return null if file is not in dir (including recursive subdirectories)
   */
  public static String getRelativePath(java.io.File file, List<java.io.File> dirs) {
    List<String> stack = new ArrayList<String>();
    String path = FilenameUtils.normalize(file.getAbsolutePath());
    java.io.File cursor = new java.io.File(path);
    while (cursor != null) {
      if (containsFile(dirs, cursor)) {
        return StringUtils.join(stack, "/");
      }
      stack.add(0, cursor.getName());
      cursor = cursor.getParentFile();
    }
    return null;
  }

  private static boolean containsFile(List<java.io.File> dirs, java.io.File cursor) {
    for (java.io.File dir : dirs) {
      if (FilenameUtils.equalsNormalizedOnSystem(dir.getAbsolutePath(), cursor.getAbsolutePath())) {
        return true;
      }
    }
    return false;
  }
}
