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

import org.sonar.api.utils.ProjectTempFolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.TempFolder;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public class DefaultTempFolder implements TempFolder, ProjectTempFolder {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private final File tempDir;
  private final boolean cleanUp;

  public DefaultTempFolder(File tempDir) {
    this(tempDir, false);
  }

  public DefaultTempFolder(File tempDir, boolean cleanUp) {
    this.tempDir = tempDir;
    this.cleanUp = cleanUp;
  }

  @Override
  public File newDir() {
    return createTempDir(tempDir, "");
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
    throw new IllegalStateException(MessageFormat.format("Failed to create directory within {0} attempts (tried {1} to {2})", TEMP_DIR_ATTEMPTS, baseName + 0, baseName
      + (TEMP_DIR_ATTEMPTS - 1)));
  }

  @Override
  public File newDir(String name) {
    File dir = new File(tempDir, name);
    try {
      FileUtils.forceMkdir(dir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp directory in " + dir, e);
    }
    return dir;
  }

  @Override
  public File newFile() {
    return newFile(null, null);
  }

  @Override
  public File newFile(@Nullable String prefix, @Nullable String suffix) {
    return createTempFile(tempDir, prefix, suffix);
  }

  /**
   * Inspired by guava waiting for JDK 7 Files#createTempFile
   */
  private static File createTempFile(File baseDir, String prefix, String suffix) {
    String baseName = StringUtils.defaultIfEmpty(prefix, "") + System.currentTimeMillis() + "-";

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
    throw new IllegalStateException(MessageFormat.format("Failed to create temp file within {0} attempts (tried {1} to {2})", TEMP_DIR_ATTEMPTS, baseName + 0 + suffix, baseName
      + (TEMP_DIR_ATTEMPTS - 1) + suffix));
  }

  public void clean() {
    FileUtils.deleteQuietly(tempDir);
  }

  public void stop() {
    if(cleanUp) {
      clean();
    }
  }

}
