/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.utils;

import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.TempUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public abstract class AbstractTempUtils implements TempUtils, Startable {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private File tempDir;

  protected void setTempDir(File tempDir) {
    this.tempDir = tempDir;
  }

  @Override
  public File createTempDirectory() {
    return createTempDirectory(null);
  }

  @Override
  public File createTempDirectory(@Nullable String prefix) {
    return createTempDir(tempDir, prefix);
  }

  /**
   * Copied from guava waiting for JDK 7 Files#createTempDirectory
   */
  private static File createTempDir(File baseDir, String prefix) {
    String baseName = prefix + System.currentTimeMillis() + "-";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory within "
      + TEMP_DIR_ATTEMPTS + " attempts (tried "
      + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
  }

  @Override
  public File createDirectory(String name) {
    File dir = new File(tempDir, name);
    try {
      FileUtils.forceMkdir(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create temp directory in " + dir, e);
    }
    return dir;
  }

  @Override
  public File createTempFile() {
    return createTempFile(null, null);
  }

  @Override
  public File createTempFile(@Nullable String prefix, @Nullable String suffix) {
    return createTempFile(tempDir, prefix, suffix);
  }

  /**
   * Inspired by guava waiting for JDK 7 Files#createTempFile
   */
  private static File createTempFile(File baseDir, String prefix, String suffix) {
    String baseName = prefix + System.currentTimeMillis() + "-";

    try {
      for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
        File tempFile = new File(baseDir, baseName + counter + suffix);
        if (tempFile.createNewFile()) {
          return tempFile;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp file", e);
    }
    throw new IllegalStateException("Failed to create temp file within "
      + TEMP_DIR_ATTEMPTS + " attempts (tried "
      + baseName + "0" + suffix + " to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + suffix + ")");
  }

  @Override
  public void start() {
    // Nothing to do
  }

  @Override
  public void stop() {
    FileUtils.deleteQuietly(tempDir);
  }

}
