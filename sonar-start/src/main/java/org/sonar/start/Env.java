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
package org.sonar.start;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

class Env {

  private static final String CONF_DIRECTORY = "/conf";

  private final File confFile;
  private final File homeDir;

  public Env(String homeDir) throws URISyntaxException {
    this.homeDir = new File(homeDir);
    this.confFile = new File(CONF_DIRECTORY+"/sonar.properties");
  }

  File rootDir() {
    return homeDir;
  }

  File file(String relativePath) {
    return new File(rootDir(), relativePath);
  }

  File freshDir(String relativePath) {
    File dir = new File(rootDir(), relativePath);
    FileUtils.deleteQuietly(dir);
    dir.mkdirs();
    return dir;
  }

  /**
   * This check is required in order to provide more meaningful message than JRuby - see SONAR-2715
   */
  void verifyWritableTempDir() {
    File file = null;
    try {
      file = File.createTempFile("sonarqube-check", "tmp");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create file in temporary directory, please check existence " +
        "and permissions of: " + FileUtils.getTempDirectory(), e);
    } finally {
      FileUtils.deleteQuietly(file);
    }
  }
}
