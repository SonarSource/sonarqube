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
package org.sonar.api.scan.filesystem;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.sonar.api.BatchSide;
import org.sonar.api.utils.PathUtils;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.5
 */
@BatchSide
public class PathResolver {

  public File relativeFile(File dir, String path) {
    Preconditions.checkArgument(dir.isDirectory(), "Not a directory: " + dir.getAbsolutePath());
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(dir, path).getAbsoluteFile();
      } catch (Exception e) {
        throw new IllegalStateException("Fail to resolve path '" + path + "' relative to: " + dir.getAbsolutePath(), e);
      }
    }
    return file;
  }

  public List<File> relativeFiles(File dir, List<String> paths) {
    List<File> result = new ArrayList<>();
    for (String path : paths) {
      result.add(relativeFile(dir, path));
    }
    return result;
  }

  @CheckForNull
  public RelativePath relativePath(Collection<File> dirs, File file) {
    List<String> stack = new ArrayList<>();
    File cursor = file;
    while (cursor != null) {
      File parentDir = parentDir(dirs, cursor);
      if (parentDir != null) {
        return new RelativePath(parentDir, Joiner.on("/").join(stack));
      }
      stack.add(0, cursor.getName());
      cursor = cursor.getParentFile();
    }
    return null;
  }

  @CheckForNull
  public String relativePath(File dir, File file) {
    List<String> stack = new ArrayList<>();
    String dirPath = PathUtils.canonicalPath(dir);
    File cursor = file;
    while (cursor != null) {
      if (dirPath.equals(PathUtils.canonicalPath(cursor))) {
        return Joiner.on("/").join(stack);
      }
      stack.add(0, cursor.getName());
      cursor = cursor.getParentFile();
    }
    return null;
  }

  @CheckForNull
  private File parentDir(Collection<File> dirs, File cursor) {
    for (File dir : dirs) {
      if (PathUtils.canonicalPath(dir).equals(PathUtils.canonicalPath(cursor))) {
        return dir;
      }
    }
    return null;
  }

  public static final class RelativePath {
    private File dir;
    private String path;

    public RelativePath(File dir, String path) {
      this.dir = dir;
      this.path = path;
    }

    public File dir() {
      return dir;
    }

    public String path() {
      return path;
    }
  }
}
