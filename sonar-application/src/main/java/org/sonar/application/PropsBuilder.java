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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.process.ConfigurationUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
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

  PropsBuilder(Properties rawProperties, JdbcSettings jdbcSettings) throws URISyntaxException {
    this(rawProperties, jdbcSettings, detectHomeDir());
  }

  /**
   * Load optional conf/sonar.properties, interpolates environment variables and
   * initializes file system
   */
  Props build() throws IOException {
    Properties p = loadPropertiesFile(homeDir);
    p.putAll(rawProperties);
    p.setProperty(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    p = ConfigurationUtils.interpolateVariables(p, System.getenv());

    // the difference between Properties and Props is that the latter
    // supports decryption of values, so it must be used when values
    // are accessed
    Props props = new Props(p);
    ProcessProperties.completeDefaults(props);

    // init file system
    initExistingDir(props, ProcessProperties.PATH_DATA, "data");
    initExistingDir(props, ProcessProperties.PATH_WEB, "web");
    initExistingDir(props, ProcessProperties.PATH_LOGS, "logs");
    initTempDir(props);

    // check JDBC properties and set path to driver
    jdbcSettings.checkAndComplete(homeDir, props);

    return props;
  }

  static File detectHomeDir() throws URISyntaxException {
    File appJar = new File(PropsBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    return appJar.getParentFile().getParentFile();
  }

  private Properties loadPropertiesFile(File homeDir) throws IOException {
    Properties p = new Properties();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    if (propsFile.exists()) {
      Reader reader = new InputStreamReader(new FileInputStream(propsFile), Charsets.UTF_8);
      try {
        p.load(reader);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
    return p;
  }

  private void initTempDir(Props props) throws IOException {
    File dir = configureDir(props, ProcessProperties.PATH_TEMP, "temp");
    FileUtils.deleteQuietly(dir);
    FileUtils.forceMkdir(dir);
  }

  private void initExistingDir(Props props, String propKey, String defaultRelativePath) throws IOException {
    File dir = configureDir(props, propKey, defaultRelativePath);
    if (!dir.exists()) {
      FileUtils.forceMkdir(dir);
    }
    if (!dir.isDirectory()) {
      throw new IllegalStateException(String.format("Property '%s' is not valid, not a directory: %s",
        propKey, dir.getAbsolutePath()));
    }
  }

  private File configureDir(Props props, String propKey, String defaultRelativePath) {
    String path = props.value(propKey, defaultRelativePath);
    File d = new File(path);
    if (!d.isAbsolute()) {
      d = new File(homeDir, path);
    }
    props.set(propKey, d.getAbsolutePath());
    return d;
  }
}
