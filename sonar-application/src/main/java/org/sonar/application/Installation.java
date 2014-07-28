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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.process.Props;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public class Installation {
  private File homeDir, tempDir, logsDir;
  private Props props;

  public Installation() throws URISyntaxException, IOException {
    Properties systemProperties = new Properties();
    systemProperties.putAll(System.getenv());
    systemProperties.putAll(System.getProperties());

    init(new SystemChecks(), detectHomeDir(), systemProperties);
  }

  Installation(SystemChecks systemChecks, File homeDir, Properties systemProperties) throws URISyntaxException, IOException {
    init(systemChecks, homeDir, systemProperties);
  }

  void init(SystemChecks systemChecks, File homeDir, Properties systemProperties) throws URISyntaxException, IOException {
    systemChecks.checkJavaVersion();

    this.homeDir = homeDir;
    props = initProps(homeDir, systemProperties);
    DefaultSettings.initDefaults(props);

    // init file system
    initExistingDir("sonar.path.data", "data");
    initExistingDir("sonar.path.web", "web");
    this.tempDir = initTempDir("sonar.path.temp", "temp");
    this.logsDir = initExistingDir("sonar.path.logs", "logs");
  }

  private File detectHomeDir() throws URISyntaxException {
    File appJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    return appJar.getParentFile().getParentFile();
  }

  /**
   * Load conf/sonar.properties
   */
  private static Props initProps(File homeDir, Properties systemProperties) throws IOException {
    Properties p = new Properties();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    if (propsFile.exists()) {
      FileReader reader = new FileReader(propsFile);
      try {
        p.load(reader);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
    p.putAll(systemProperties);
    p.setProperty("sonar.path.home", homeDir.getAbsolutePath());
    return new Props(p);
  }

  File homeDir() {
    return homeDir;
  }

  File logsDir() {
    return logsDir;
  }

  File tempDir() {
    return tempDir;
  }

  String starPath(String relativePath) {
    File dir = new File(homeDir, relativePath);
    return FilenameUtils.concat(dir.getAbsolutePath(), "*");
  }

  private File initTempDir(String propKey, String defaultRelativePath) throws IOException {
    File dir = configuredDir(propKey, defaultRelativePath);
    FileUtils.deleteQuietly(dir);
    FileUtils.forceMkdir(dir);

    // verify that temp directory is writable
    File tempFile = File.createTempFile("check", "tmp", dir);
    FileUtils.deleteQuietly(tempFile);

    return dir;
  }

  private File initExistingDir(String propKey, String defaultRelativePath) throws IOException {
    File dir = configuredDir(propKey, defaultRelativePath);
    if (!dir.exists()) {
      throw new IllegalStateException(String.format("Property '%s' is not valid, directory does not exist: %s",
        propKey, dir.getAbsolutePath()));
    }
    if (!dir.isDirectory()) {
      throw new IllegalStateException(String.format("Property '%s' is not valid, not a directory: %s",
        propKey, dir.getAbsolutePath()));
    }
    return dir;
  }

  private File configuredDir(String propKey, String defaultRelativePath) {
    String path = prop(propKey, defaultRelativePath);
    File d = new File(path);
    if (!d.isAbsolute()) {
      d = new File(homeDir, path);
    }
    props.set(propKey, d.getAbsolutePath());
    return d;
  }

  @CheckForNull
  String prop(String key, @Nullable String defaultValue) {
    return props.of(key, defaultValue);
  }

  @CheckForNull
  String prop(String key) {
    return props.of(key);
  }

  public Props props() {
    return props;
  }
}
