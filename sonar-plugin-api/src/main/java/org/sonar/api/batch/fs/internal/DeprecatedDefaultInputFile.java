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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.utils.PathUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @since 4.2
 */
public class DeprecatedDefaultInputFile extends DefaultInputFile implements org.sonar.api.resources.InputFile {

  private String basedir;
  private String deprecatedKey;
  private String sourceDirAbsolutePath;
  private String pathRelativeToSourceDir;

  public DeprecatedDefaultInputFile(String moduleKey, String relativePath) {
    super(moduleKey, relativePath);
  }

  /**
   * @deprecated in 4.2. Replaced by {@link org.sonar.api.batch.fs.FileSystem#baseDir()}
   */
  @Deprecated
  @Override
  public File getFileBaseDir() {
    return new File(basedir);
  }

  public DeprecatedDefaultInputFile setBasedir(File basedir) {
    this.basedir = PathUtils.sanitize(basedir.getAbsolutePath());
    return this;
  }

  /**
   * @deprecated in 4.2. Use {@link #file()}
   */
  @Deprecated
  @Override
  public File getFile() {
    return file();
  }

  /**
   * @deprecated in 4.2. Use {@link #relativePath()}
   */
  @Deprecated
  @Override
  public String getRelativePath() {
    return pathRelativeToSourceDir;
  }

  /**
   * Key used before version 4.2. It can be different than {@link #key} on Java files.
   */
  public String deprecatedKey() {
    return deprecatedKey;
  }

  public DeprecatedDefaultInputFile setDeprecatedKey(String s) {
    this.deprecatedKey = s;
    return this;
  }

  /**
   * Used only for backward-compatibility. Meaningless since version 4.2.
   */
  public String sourceDirAbsolutePath() {
    return sourceDirAbsolutePath;
  }

  public DeprecatedDefaultInputFile setSourceDirAbsolutePath(String s) {
    this.sourceDirAbsolutePath = PathUtils.sanitize(s);
    return this;
  }

  /**
   * Used only for backward-compatibility. Meaningless since version 4.2.
   */

  public String pathRelativeToSourceDir() {
    return pathRelativeToSourceDir;
  }

  public DeprecatedDefaultInputFile setPathRelativeToSourceDir(String s) {
    this.pathRelativeToSourceDir = PathUtils.sanitize(s);
    return this;
  }

  @Override
  public InputStream getInputStream() throws FileNotFoundException {
    return new BufferedInputStream(new FileInputStream(file()));
  }
}
