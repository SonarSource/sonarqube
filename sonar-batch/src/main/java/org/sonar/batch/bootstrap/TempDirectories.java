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
package org.sonar.batch.bootstrap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.io.File;
import java.io.IOException;

public class TempDirectories {

  private File rootDir;

  public TempDirectories(ProjectReactor reactor) throws IOException {
    this.rootDir = new File(reactor.getRoot().getWorkDir(), "_tmp");
    if (rootDir.exists()) {
      FileUtils.deleteDirectory(rootDir);
    }
    FileUtils.forceMkdir(rootDir);
  }

  public File getRoot() {
    return rootDir;
  }

  public void stop() {
    FileUtils.deleteQuietly(rootDir);
  }

  /**
   * Get or create a working directory
   */
  public File getDir(String dirname) {
    if (StringUtils.isBlank(dirname)) {
      return rootDir;
    }

    File dir = new File(rootDir, dirname);
    try {
      FileUtils.forceMkdir(dir);
      return dir;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp directory: " + dir, e);
    }
  }

  public File getFile(String dirname, String filename) {
    File dir = getDir(dirname);
    return new File(dir, filename);
  }
}
