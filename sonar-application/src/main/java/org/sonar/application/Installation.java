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
import org.apache.commons.lang.StringUtils;
import org.sonar.process.ConfigurationUtils;
import org.sonar.process.MinimumViableEnvironment;
import org.sonar.process.Props;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

public class Installation {
  private File homeDir, tempDir, logsDir;
  private Props props;

  public Installation(Properties rawProperties) throws Exception {
    init(new MinimumViableEnvironment(), detectHomeDir(), rawProperties);
  }

  Installation(Properties rawProperties, MinimumViableEnvironment mve, File homeDir) throws Exception {
    init(mve, homeDir, rawProperties);
  }

  void init(MinimumViableEnvironment minViableEnv, File homeDir, Properties rawProperties) throws URISyntaxException, IOException {
    minViableEnv.check();

    this.homeDir = homeDir;
    props = initProps(homeDir, rawProperties);
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
   * Load optional conf/sonar.properties and interpolates environment variables
   */
  private static Props initProps(File homeDir, Properties rawProperties) throws IOException {
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
    p.putAll(rawProperties);
    p.setProperty("sonar.path.home", homeDir.getAbsolutePath());
    p = ConfigurationUtils.interpolateVariables(p, System.getenv());
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

  static Installation parseArguments(String[] args) throws Exception {
    Properties props = argumentsToProperties(args);

    // complete with only the system properties that start with "sonar."
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith("sonar.")) {
        props.setProperty(key, entry.getValue().toString());
      }
    }

    return new Installation(props);
  }

  static Properties argumentsToProperties(String[] args) {
    Properties props = new Properties();
    for (String arg : args) {
      if (!arg.startsWith("-D") || !arg.contains("=")) {
        throw new IllegalArgumentException(String.format(
          "Command-line argument must start with -D, for example -Dsonar.jdbc.username=sonar. Got: %s", arg));
      }
      String key = StringUtils.substringBefore(arg, "=").substring(2);
      String value = StringUtils.substringAfter(arg, "=");
      props.setProperty(key, value);
    }
    return props;
  }
}
