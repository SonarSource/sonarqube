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
package org.sonar.application.process;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.sonar.process.ProcessId;

public class EsCommand extends AbstractCommand<EsCommand> {
  private File executable;
  private File confDir;
  private String clusterName;
  private String host;
  private int port;
  private Properties log4j2Properties;
  private List<String> esOptions = new ArrayList<>();
  private List<String> jvmOptions = new ArrayList<>();
  private Path jvmOptionsFile;

  public EsCommand(ProcessId id) {
    super(id);
  }

  public File getExecutable() {
    return executable;
  }

  public EsCommand setExecutable(File executable) {
    this.executable = executable;
    return this;
  }

  public File getConfDir() {
    return confDir;
  }

  public EsCommand setConfDir(File confDir) {
    this.confDir = confDir;
    return this;
  }

  public String getClusterName() {
    return clusterName;
  }

  public EsCommand setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  public String getHost() {
    return host;
  }

  public EsCommand setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public EsCommand setPort(int port) {
    this.port = port;
    return this;
  }

  public Properties getLog4j2Properties() {
    return log4j2Properties;
  }

  public EsCommand setLog4j2Properties(Properties log4j2Properties) {
    this.log4j2Properties = log4j2Properties;
    return this;
  }

  public List<String> getEsOptions() {
    return esOptions;
  }

  public EsCommand addEsOption(String s) {
    if (!s.isEmpty()) {
      esOptions.add(s);
    }
    return this;
  }

  public List<String> getJvmOptions() {
    return jvmOptions;
  }

  public EsCommand addJvmOption(String s) {
    if (!s.isEmpty()) {
      jvmOptions.add(s);
    }
    return this;
  }

  public Path getJvmOptionsFile() {
    return jvmOptionsFile;
  }

  public EsCommand setJvmOptionsFile(Path jvmOptionsFile) {
    this.jvmOptionsFile = jvmOptionsFile;
    return this;
  }
}
