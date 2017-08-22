/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.process.es;

import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

/**
 * Holds {@link File} to the various directories of ElasticSearch distribution embedded in SonarQube and provides
 * {@link File} objects to the various files of it SonarQube cares about.
 *
 * <p>
 * This class does not ensure files nor directories actually exist.
 * </p>
 */
public class EsFileSystem {
  private final File homeDirectory;
  private final File dataDirectory;
  private final File confDirectory;
  private final File logDirectory;

  public EsFileSystem(Props props) {
    File sqHomeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);

    this.homeDirectory = new File(sqHomeDir, "elasticsearch");
    this.dataDirectory = buildDataDir(props, sqHomeDir);
    this.confDirectory = buildConfDir(props);
    this.logDirectory = buildLogPath(props, sqHomeDir);
  }

  private static File buildDataDir(Props props, File sqHomeDir) {
    String dataPath = props.value(ProcessProperties.PATH_DATA);
    if (StringUtils.isNotEmpty(dataPath)) {
      return new File(dataPath, "es");
    }
    return new File(sqHomeDir, "data/es");
  }

  private static File buildLogPath(Props props, File sqHomeDir) {
    String logPath = props.value(ProcessProperties.PATH_LOGS);
    if (StringUtils.isNotEmpty(logPath)) {
      return new File(logPath);
    }
    return new File(sqHomeDir, "log");
  }

  private static File buildConfDir(Props props) {
    File tempPath = props.nonNullValueAsFile(ProcessProperties.PATH_TEMP);
    return new File(new File(tempPath, "conf"), "es");
  }

  public File getHomeDirectory() {
    return homeDirectory;
  }

  public File getDataDirectory() {
    return dataDirectory;
  }

  public File getConfDirectory() {
    return confDirectory;
  }

  public File getLogDirectory() {
    return logDirectory;
  }

  public File getExecutable() {
    return new File(homeDirectory, "bin/" + getExecutableName());
  }

  private static String getExecutableName() {
    if (System.getProperty("os.name").startsWith("Windows")) {
      return "elasticsearch.bat";
    }
    return "elasticsearch";
  }

  public File getLog4j2Properties() {
    return new File(confDirectory, "log4j2.properties");
  }

  public File getElasticsearchYml() {
    return new File(confDirectory, "elasticsearch.yml");
  }

  public File getJvmOptions() {
    return new File(confDirectory, "jvm.options");
  }
}
