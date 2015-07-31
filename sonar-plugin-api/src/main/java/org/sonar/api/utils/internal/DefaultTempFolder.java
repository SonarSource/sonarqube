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
package org.sonar.api.utils.internal;

import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.TempFolder;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultTempFolder implements TempFolder {

  private final File tempDir;
  private final boolean deleteOnExit;

  public DefaultTempFolder(File tempDir) {
    this(tempDir, false);
  }

  public DefaultTempFolder(File tempDir, boolean deleteOnExit) {
    this.tempDir = tempDir;
    this.deleteOnExit = deleteOnExit;
  }

  @Override
  public File newDir() {
    return createTempDir(tempDir.toPath()).toFile();
  }

  private static Path createTempDir(Path baseDir) {
    try {
      return Files.createTempDirectory(baseDir, null);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp directory", e);
    }
  }

  @Override
  public File newDir(String name) {
    File dir = new File(tempDir, name);
    try {
      FileUtils.forceMkdir(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp directory - " + dir, e);
    }
    return dir;
  }

  @Override
  public File newFile() {
    return newFile(null, null);
  }

  @Override
  public File newFile(@Nullable String prefix, @Nullable String suffix) {
    return createTempFile(tempDir.toPath(), prefix, suffix).toFile();
  }

  private static Path createTempFile(Path baseDir, String prefix, String suffix) {
    try {
      return Files.createTempFile(baseDir, prefix, suffix);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp file", e);
    }
  }

  public void clean() {
    FileUtils.deleteQuietly(tempDir);
  }

  public void stop() {
    if (deleteOnExit) {
      clean();
    }
  }

}
