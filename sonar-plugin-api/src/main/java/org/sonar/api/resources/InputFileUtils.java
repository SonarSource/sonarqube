/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.resources;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.8
 * @deprecated since 5.0 as {@link InputFile} is deprecated
 */
@Deprecated
public final class InputFileUtils {

  private InputFileUtils() {
    // only static methods
  }

  /**
   * @param inputFiles not nullable
   * @return not null list
   */
  public static List<java.io.File> toFiles(Collection<InputFile> inputFiles) {
    List<java.io.File> files = new ArrayList<>();
    for (InputFile inputFile : inputFiles) {
      files.add(inputFile.getFile());
    }
    return files;
  }

  /**
   * Extract the directory from relative path. Examples :
   * - returns "org/foo" when relative path is "org/foo/Bar.java"
   * - returns "" when relative path is "Bar.java"
   */
  public static String getRelativeDirectory(InputFile inputFile) {
    String relativePath = inputFile.getRelativePath();
    if (StringUtils.contains(relativePath, "/")) {
      return StringUtils.substringBeforeLast(relativePath, "/");
    }
    return "";
  }

  /**
   * For internal and for testing purposes. Please use the FileSystem component to access files.
   */
  public static InputFile create(java.io.File basedir, java.io.File file) {
    String relativePath = getRelativePath(basedir, file);
    if (relativePath != null) {
      return create(basedir, relativePath);
    }
    return null;
  }

  /**
   * For internal and for testing purposes. Please use the FileSystem component to access files.
   */
  public static InputFile create(java.io.File basedir, String relativePath) {
    return new DefaultInputFile(basedir, relativePath);
  }

  /**
   * For internal and for testing purposes. Please use the FileSystem component to access files.
   */
  public static List<InputFile> create(java.io.File basedir, Collection<java.io.File> files) {
    List<InputFile> inputFiles = new ArrayList<>();
    for (File file : files) {
      InputFile inputFile = create(basedir, file);
      if (inputFile != null) {
        inputFiles.add(inputFile);
      }
    }
    return inputFiles;
  }

  static String getRelativePath(java.io.File basedir, java.io.File file) {
    List<String> stack = Lists.newArrayList(file.getName());
    java.io.File cursor = file.getParentFile();
    while (cursor != null) {
      if (basedir.equals(cursor)) {
        return StringUtils.join(stack, "/");
      }
      stack.add(0, cursor.getName());
      cursor = cursor.getParentFile();
    }
    return null;
  }

  static final class DefaultInputFile implements InputFile {
    private java.io.File basedir;
    private String relativePath;

    DefaultInputFile(java.io.File basedir, String relativePath) {
      this.basedir = basedir;
      this.relativePath = relativePath;
    }

    @Override
    public java.io.File getFileBaseDir() {
      return basedir;
    }

    @Override
    public java.io.File getFile() {
      return new java.io.File(basedir, relativePath);
    }

    /**
     * @since 3.1
     */
    @Override
    public InputStream getInputStream() throws FileNotFoundException {
      return new BufferedInputStream(new FileInputStream(getFile()));
    }

    @Override
    public String getRelativePath() {
      return relativePath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof DefaultInputFile) {
        DefaultInputFile that = (DefaultInputFile) o;
        return Objects.equal(basedir, that.basedir) && Objects.equal(relativePath, that.relativePath);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = basedir.hashCode();
      result = 31 * result + relativePath.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return String.format("%s -> %s", basedir.getAbsolutePath(), relativePath);
    }
  }
}
