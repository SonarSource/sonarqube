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
package org.sonar.application;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

class Env {

  static final String ERROR_MESSAGE = "Do not copy-paste the configuration files (conf directory) from the old version. Update the content of the new files instead.";
  private final File confFile;

  // visible for testing
  Env(URL confUrl) throws URISyntaxException {
    if (confUrl == null) {
      throw new IllegalStateException(ERROR_MESSAGE);
    }
    this.confFile = new File(confUrl.toURI());
  }

  Env() throws URISyntaxException {
    this(Env.class.getResource("/sonar.properties"));
  }

  File rootDir() {
    return confFile.getParentFile().getParentFile();
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
