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
package org.sonar.process;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class SharedStatus {

  private final File file;

  public SharedStatus(File file) {
    this.file = file;
  }

  /**
   * Executed by monitor - remove existing shared file before starting child process
   */
  public void prepare() {
    if (file.exists()) {
      if (!file.delete()) {
        throw new MessageException(String.format(
          "Fail to delete file %s. Please check that no SonarQube process is alive", file));
      }
    }
  }

  public boolean wasStartedAfter(long launchedAt) {
    // File#lastModified() can have second precision on some OS
    return file.exists() && file.lastModified() / 1000 >= launchedAt / 1000;
  }

  public void setReady() {
    try {
      FileUtils.touch(file);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create file " + file, e);
    }
  }

  public void setStopped() {
    FileUtils.deleteQuietly(file);
  }
}
