/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.updatecenter.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

  public static final String WORKING_DIR = "workingDir";
  public static final String OUTPUT_FILE = "outputFile";
  public static final String SOURCE_PATH = "path";
  public static final String SOURCE_LOGIN = "login";
  public static final String SOURCE_PASSWORD = "password";

  private static Logger LOG = LoggerFactory.getLogger(Configuration.class);
  private Properties props;
  private File workingDir = null;  

  public Configuration(Properties props) {
    this.props = props;
  }

  public void log() {
    LOG.info("-------------------------------");
    LOG.info(WORKING_DIR + ": " + getWorkingDir().getPath());
    LOG.info(OUTPUT_FILE + ": " + getOutputFile().getPath());
    LOG.info(SOURCE_PATH + ": " +  getSourcePath());
    LOG.info(SOURCE_LOGIN + ": " + getSourceLogin());
    LOG.info(SOURCE_PASSWORD + ": " + getSourcePassword());
    LOG.info("-------------------------------");
  }

  public File getWorkingDir() {
    if (workingDir == null) {
      String path = props.getProperty(WORKING_DIR);
      if (StringUtils.isBlank(path)) {
        workingDir = new File(System.getProperty("user.home"), ".sonar-update-center");
      } else {
        workingDir = new File(path);
      }
      try {
        FileUtils.forceMkdir(workingDir);

      } catch (IOException e) {
        throw new RuntimeException("Fail to create the working directory: " + workingDir.getAbsolutePath(), e);
      }
    }
    return workingDir;
  }

  public File getOutputFile() {
    String path = props.getProperty(OUTPUT_FILE);
    if (StringUtils.isNotBlank(path)) {
      return new File(path);
    }
    return new File(getWorkingDir(), "generated-sonar-updates.properties");
  }

  public String getSourcePath() {
    return props.getProperty(SOURCE_PATH);
  }

  public String getSourceLogin() {
    return props.getProperty(SOURCE_LOGIN);
  }

  public String getSourcePassword() {
    return props.getProperty(SOURCE_PASSWORD);
  }
}
