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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.NetworkUtils;
import org.sonar.process.Props;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public class Installation {
  private static final Logger LOG = LoggerFactory.getLogger(Installation.class);

  private final File homeDir;
  private final File tempDir, dataDir, logsDir, webDir;
  private final Props props;

  Installation() throws URISyntaxException, IOException {
    // home dir guessed with location of lib/sonar-application.jar
    File appJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    homeDir = appJar.getParentFile().getParentFile();

    props = initProps(homeDir);

    // init file system
    this.tempDir = initTempDir("sonar.path.temp", "temp");
    this.dataDir = existingDir("sonar.path.data", "data");
    this.logsDir = existingDir("sonar.path.logs", "logs");
    this.webDir = existingDir("sonar.path.web", "web");

    initElasticsearch();
  }

  /**
   * Load conf/sonar.properties
   */
  private static Props initProps(File homeDir) throws IOException {
    Properties p = new Properties();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    if (propsFile.exists()) {
      FileReader reader = new FileReader(propsFile);
      try {
        p.load(reader);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    } else {
      LOG.info("Configuration file not found: " + propsFile.getAbsolutePath());
    }
    p.putAll(System.getenv());
    p.putAll(System.getProperties());
    p.setProperty("sonar.path.home", homeDir.getAbsolutePath());
    return new Props(p);
  }

  private void initElasticsearch() {
    int port = props.intOf("sonar.es.port", 0);
    if (port <= 0) {
      props.set("sonar.es.port", String.valueOf(NetworkUtils.freePort()));
    }
    if (props.of("sonar.es.cluster.name") == null) {
      props.set("sonar.es.cluster.name", "sonarqube");
    }
    props.set("sonar.es.type", "TRANSPORT");
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

  private File existingDir(String propKey, String defaultRelativePath) throws IOException {
    File dir = configuredDir(propKey, defaultRelativePath);
    if (!dir.exists()) {
      throw new IllegalStateException(String.format("Directory does not exist: %s. Please check property %s", dir.getAbsolutePath(), propKey));
    }
    if (!dir.isDirectory()) {
      throw new IllegalStateException(String.format("Not a directory: %s. Please check property %s", dir.getAbsolutePath(), propKey));
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

  public Props props() {
    return props;
  }
}
