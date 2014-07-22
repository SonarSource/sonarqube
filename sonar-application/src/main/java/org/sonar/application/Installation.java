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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Installation {
  private static final Logger LOG = LoggerFactory.getLogger(Installation.class);

  // guessed from location of sonar-application.jar
  private final File homeDir;
  private final File tempDir, dataDir, logsDir, webDir;
  private final Map<String, String> props;

  Installation() throws URISyntaxException, IOException {
    // lib/sonar-application.jar
    File appJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    homeDir = appJar.getParentFile().getParentFile();

    props = initProps(homeDir);

    // init file system
    props.put("sonar.path.home", homeDir.getAbsolutePath());
    this.dataDir = existingDir("sonar.path.data", "data");
    this.tempDir = freshDir("sonar.path.temp", "temp");
    this.logsDir = existingDir("sonar.path.logs", "logs");
    this.webDir = existingDir("sonar.path.web", "web");

    initElasticsearch();
  }

  /**
   * Load conf/sonar.properties
   */
  private static Map<String, String> initProps(File homeDir) throws IOException {
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
    p = ConfigurationUtils.interpolateEnvVariables(p);
    Map<String, String> result = new HashMap<String, String>();
    for (Map.Entry<Object, Object> entry : p.entrySet()) {
      Object val = entry.getValue();
      if (val != null) {
        result.put(entry.getKey().toString(), val.toString());
      }
    }
    return result;
  }

  private void initElasticsearch() {
    // init Elasticsearch
    if (props.get("sonar.es.port") == null) {
      props.put("sonar.es.port", String.valueOf(NetworkUtils.freePort()));
    }
    if (props.get("sonar.es.cluster.name") == null) {
      props.put("sonar.es.cluster.name", "sonarqube");
    }
    props.put("sonar.es.type", "TRANSPORT");
  }

  File homeDir() {
    return homeDir;
  }

  File esDir() {
    return new File(homeDir, "data/es");
  }

  File webDir() {
    return webDir;
  }

  File tempDir() {
    return tempDir;
  }

  String starPath(String relativePath) {
    File dir = new File(homeDir, relativePath);
    return FilenameUtils.concat(dir.getAbsolutePath(), "*");
  }

  private File freshDir(String propKey, String defaultRelativePath) throws IOException {
    File dir = configuredDir(propKey, defaultRelativePath);
    FileUtils.deleteQuietly(dir);
    FileUtils.forceMkdir(dir);
    return dir;
  }

  private File existingDir(String propKey, String defaultRelativePath) throws IOException {
    File dir = configuredDir(propKey, defaultRelativePath);
    if (!dir.exists()) {
      // TODO replace by MessageException
      throw new IllegalStateException(String.format("Directory does not exist: %s. Please check property %s", dir.getAbsolutePath(), propKey));
    }
    if (!dir.isDirectory()) {
      // TODO replace by MessageException
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
    props.put(propKey, d.getAbsolutePath());
    return d;
  }

  Map<String, String> props() {
    return props;
  }

  String prop(String key) {
    return props.get(key);
  }

  @CheckForNull
  String prop(String key, @Nullable String defaultValue) {
    String s = props.get(key);
    return s != null ? s : defaultValue;
  }

  @CheckForNull
  Integer propAsInt(String key) {
    String s = prop(key, null);
    if (s != null && !"".equals(s)) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        throw new IllegalStateException(String.format("Value of property %s is not an integer: %s", key, s), e);
      }
    }
    return null;
  }

  void setProp(String key, String value) {
    props.put(key, value);
  }

  void logInfo(String message) {
    System.out.println(message);
  }

  void logError(String message) {
    System.err.println(message);
  }
}
