/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;
import org.sonar.process.monitor.FileSystem;

import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.sonar.process.FileUtils.cleanDirectory;
import static org.sonar.process.ProcessProperties.PATH_DATA;
import static org.sonar.process.ProcessProperties.PATH_HOME;
import static org.sonar.process.ProcessProperties.PATH_LOGS;
import static org.sonar.process.ProcessProperties.PATH_TEMP;
import static org.sonar.process.ProcessProperties.PATH_WEB;

public class AppFileSystem implements FileSystem {
  private static final Logger LOG = LoggerFactory.getLogger(AppFileSystem.class);

  private static final String DEFAULT_DATA_DIRECTORY_NAME = "data";
  private static final String DEFAULT_WEB_DIRECTORY_NAME = "web";
  private static final String DEFAULT_LOGS_DIRECTORY_NAME = "logs";
  private static final String DEFAULT_TEMP_DIRECTORY_NAME = "temp";

  private final Props props;
  private final File homeDir;
  private boolean initialized = false;

  public AppFileSystem(Props props) {
    this.props = props;
    this.homeDir = props.nonNullValueAsFile(PATH_HOME);
  }

  public void verifyProps() {
    ensurePropertyIsAbsolutePath(props, PATH_DATA, DEFAULT_DATA_DIRECTORY_NAME);
    ensurePropertyIsAbsolutePath(props, PATH_WEB, DEFAULT_WEB_DIRECTORY_NAME);
    ensurePropertyIsAbsolutePath(props, PATH_LOGS, DEFAULT_LOGS_DIRECTORY_NAME);
    ensurePropertyIsAbsolutePath(props, PATH_TEMP, DEFAULT_TEMP_DIRECTORY_NAME);
    this.initialized = true;
  }

  /**
   * Must be called after {@link #verifyProps()}
   */
  @Override
  public void reset() throws IOException {
    if (!initialized) {
      throw new IllegalStateException("method verifyProps must be called first");
    }
    createDirectory(props, PATH_DATA);
    createDirectory(props, PATH_WEB);
    createDirectory(props, PATH_LOGS);
    createOrCleanDirectory(props, PATH_TEMP);
  }

  @Override
  public File getTempDir() {
    return props.nonNullValueAsFile(PATH_TEMP);
  }

  private File ensurePropertyIsAbsolutePath(Props props, String propKey, String defaultRelativePath) {
    String path = props.value(propKey, defaultRelativePath);
    File d = new File(path);
    if (!d.isAbsolute()) {
      d = new File(homeDir, path);
      LOG.trace("Overriding property {} from relative path '{}' to absolute path '{}'", path, d.getAbsolutePath());
      props.set(propKey, d.getAbsolutePath());
    }
    return d;
  }

  private static boolean createDirectory(Props props, String propKey) throws IOException {
    File dir = props.nonNullValueAsFile(propKey);
    if (dir.exists()) {
      ensureIsNotAFile(propKey, dir);
      return false;
    } else {
      LOG.trace("forceMkdir {}", dir.getAbsolutePath());
      forceMkdir(dir);
      ensureIsNotAFile(propKey, dir);
      return true;
    }
  }

  private static void ensureIsNotAFile(String propKey, File dir) {
    if (!dir.isDirectory()) {
      throw new IllegalStateException(String.format("Property '%s' is not valid, not a directory: %s",
        propKey, dir.getAbsolutePath()));
    }
  }

  private static void createOrCleanDirectory(Props props, String propKey) throws IOException {
    File dir = props.nonNullValueAsFile(propKey);
    LOG.info("Cleaning or creating temp directory {}", dir.getAbsolutePath());
    if (!createDirectory(props, propKey)) {
      cleanDirectory(dir);
    }
  }

}
