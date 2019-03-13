/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.es;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.sonar.application.command.EsJvmOptions;
import org.sonar.process.Props;

import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

/**
 * Holds {@link File} to the various directories of ElasticSearch distribution embedded in SonarQube and provides
 * {@link File} objects to the various files of it SonarQube cares about.
 *
 * <p>
 * This class does not ensure files nor directories actually exist.
 * </p>
 */
public class EsInstallation {
  private final File homeDirectory;
  private final List<File> outdatedSearchDirectories;
  private final File dataDirectory;
  private final File confDirectory;
  private final File logDirectory;
  private EsJvmOptions esJvmOptions;
  private EsYmlSettings esYmlSettings;
  private Properties log4j2Properties;
  private String clusterName;
  private String host;
  private int port;

  public EsInstallation(Props props) {
    File sqHomeDir = props.nonNullValueAsFile(PATH_HOME.getKey());

    this.homeDirectory = new File(sqHomeDir, "elasticsearch");
    this.outdatedSearchDirectories = buildOutdatedSearchDirs(props);
    this.dataDirectory = buildDataDir(props);
    this.confDirectory = buildConfDir(props);
    this.logDirectory = buildLogPath(props);
  }

  private static List<File> buildOutdatedSearchDirs(Props props) {
    String dataPath = props.nonNullValue(PATH_DATA.getKey());
    return Arrays.asList(new File(dataPath, "es"), new File(dataPath, "es5"));
  }

  private static File buildDataDir(Props props) {
    String dataPath = props.nonNullValue(PATH_DATA.getKey());
    return new File(dataPath, "es6");
  }

  private static File buildLogPath(Props props) {
    return props.nonNullValueAsFile(PATH_LOGS.getKey());
  }

  private static File buildConfDir(Props props) {
    File tempPath = props.nonNullValueAsFile(PATH_TEMP.getKey());
    return new File(new File(tempPath, "conf"), "es");
  }

  public File getHomeDirectory() {
    return homeDirectory;
  }

  public List<File> getOutdatedSearchDirectories() {
    return Collections.unmodifiableList(outdatedSearchDirectories);
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
    return new File(homeDirectory, "bin/elasticsearch");
  }

  public File getLog4j2PropertiesLocation() {
    return new File(confDirectory, "log4j2.properties");
  }

  public File getElasticsearchYml() {
    return new File(confDirectory, "elasticsearch.yml");
  }

  public File getJvmOptions() {
    return new File(confDirectory, "jvm.options");
  }

  public File getLibDirectory() {
    return new File(homeDirectory, "lib");
  }

  public EsJvmOptions getEsJvmOptions() {
    return esJvmOptions;
  }

  public EsInstallation setEsJvmOptions(EsJvmOptions esJvmOptions) {
    this.esJvmOptions = esJvmOptions;
    return this;
  }

  public EsYmlSettings getEsYmlSettings() {
    return esYmlSettings;
  }

  public EsInstallation setEsYmlSettings(EsYmlSettings esYmlSettings) {
    this.esYmlSettings = esYmlSettings;
    return this;
  }

  public Properties getLog4j2Properties() {
    return log4j2Properties;
  }

  public EsInstallation setLog4j2Properties(Properties log4j2Properties) {
    this.log4j2Properties = log4j2Properties;
    return this;
  }

  public String getClusterName() {
    return clusterName;
  }

  public EsInstallation setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  public String getHost() {
    return host;
  }

  public EsInstallation setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public EsInstallation setPort(int port) {
    this.port = port;
    return this;
  }
}
