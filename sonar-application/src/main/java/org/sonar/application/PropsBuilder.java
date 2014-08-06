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
import org.apache.commons.io.IOUtils;
import org.sonar.process.ConfigurationUtils;
import org.sonar.process.Props;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

class PropsBuilder {

  private final File homeDir;
  private final JdbcSettings jdbcSettings;
  private final Properties rawProperties;

  PropsBuilder(Properties rawProperties, JdbcSettings jdbcSettings, File homeDir) {
    this.rawProperties = rawProperties;
    this.jdbcSettings = jdbcSettings;
    this.homeDir = homeDir;
  }

  PropsBuilder(Properties rawProperties, JdbcSettings jdbcSettings) throws Exception {
    this(rawProperties, jdbcSettings, detectHomeDir());
  }

  /**
   * Load optional conf/sonar.properties, interpolates environment variables and
   * initializes file system
   */
  Props build() throws Exception {
    Properties p = loadPropertiesFile(homeDir);
    p.putAll(rawProperties);
    p.setProperty("sonar.path.home", homeDir.getAbsolutePath());
    p = ConfigurationUtils.interpolateVariables(p, System.getenv());

    // the difference between Properties and Props is that the latter
    // supports decryption of values, so it must be used when values
    // are accessed
    Props props = new Props(p);
    DefaultSettings.init(props);

    // init file system
    initExistingDir(props, "sonar.path.data", "data");
    initExistingDir(props, "sonar.path.web", "web");
    initExistingDir(props, "sonar.path.logs", "logs");
    initTempDir(props);

    // check JDBC properties and set path to driver
    jdbcSettings.checkAndComplete(homeDir, props);

    return props;
  }

  static File detectHomeDir() throws Exception {
    File appJar = new File(PropsBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    return appJar.getParentFile().getParentFile();
  }

  private Properties loadPropertiesFile(File homeDir) throws IOException {
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
    return p;
  }

  private void initTempDir(Props props) throws IOException {
    File dir = configureDir(props, "sonar.path.temp", "temp");
    FileUtils.deleteQuietly(dir);
    FileUtils.forceMkdir(dir);
  }

  private void initExistingDir(Props props, String propKey, String defaultRelativePath) throws IOException {
    File dir = configureDir(props, propKey, defaultRelativePath);
    if (!dir.exists()) {
      throw new IllegalStateException(String.format("Property '%s' is not valid, directory does not exist: %s",
        propKey, dir.getAbsolutePath()));
    }
    if (!dir.isDirectory()) {
      throw new IllegalStateException(String.format("Property '%s' is not valid, not a directory: %s",
        propKey, dir.getAbsolutePath()));
    }
  }

  private File configureDir(Props props, String propKey, String defaultRelativePath) {
    String path = props.of(propKey, defaultRelativePath);
    File d = new File(path);
    if (!d.isAbsolute()) {
      d = new File(homeDir, path);
    }
    props.set(propKey, d.getAbsolutePath());
    return d;
  }
}
