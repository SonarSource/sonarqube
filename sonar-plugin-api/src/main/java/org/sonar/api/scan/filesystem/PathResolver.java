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
package org.sonar.api.scan.filesystem;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.sonar.api.BatchComponent;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.5
 */
public class PathResolver implements BatchComponent {

  public File relativeFile(File dir, String path) {
    Preconditions.checkArgument(dir.isDirectory(), "Not a directory: " + dir.getAbsolutePath());
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(dir, path).getCanonicalFile();
      } catch (Exception e) {
        throw new IllegalStateException("Fail to resolve path '" + path + "' relative to: " + dir.getAbsolutePath(), e);
      }
    }
    return file;
  }

  public List<File> relativeFiles(File dir, List<String> paths) {
    List<File> result = Lists.newArrayList();
    for (String path : paths) {
      result.add(relativeFile(dir, path));
    }
    return result;
  }

  public RelativePath relativePath(Collection<File> dirs, File file) {
    List<String> stack = Lists.newArrayList();
    String path = FilenameUtils.normalize(file.getAbsolutePath());
    File cursor = new File(path);
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

  public String relativePath(File dir, File file) {
    List<String> stack = Lists.newArrayList();
    String path = FilenameUtils.normalize(file.getAbsolutePath());
    File cursor = new File(path);
    while (cursor != null) {
      if (containsFile(dir, cursor)) {
        return Joiner.on("/").join(stack);
      }
      stack.add(0, cursor.getName());
      cursor = cursor.getParentFile();
    }
    return null;
  }

  private File parentDir(Collection<File> dirs, File cursor) {
    for (File dir : dirs) {
      if (FilenameUtils.equalsNormalizedOnSystem(dir.getAbsolutePath(), cursor.getAbsolutePath())) {
        return dir;
      }
    }
    return null;
  }

  private boolean containsFile(File dir, File cursor) {
    return FilenameUtils.equalsNormalizedOnSystem(dir.getAbsolutePath(), cursor.getAbsolutePath());
  }

  public static final class RelativePath {
    private File dir;
    private String path;

    private RelativePath(File dir, String path) {
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
