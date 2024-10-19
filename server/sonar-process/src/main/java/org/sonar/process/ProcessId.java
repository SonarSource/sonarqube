/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.process;

import static java.lang.String.format;

public enum ProcessId {

  APP("app", 0, "sonar", "SonarQube"),
  ELASTICSEARCH("es", 1, "es", "ElasticSearch"),
  WEB_SERVER("web", 2, "web", "Web Server"),
  COMPUTE_ENGINE("ce", 3, "ce", "Compute Engine");

  private final String key;
  private final int ipcIndex;
  private final String logFilenamePrefix;
  private final String humanReadableName;

  ProcessId(String key, int ipcIndex, String logFilenamePrefix, String humanReadableName) {
    this.key = key;
    this.ipcIndex = ipcIndex;
    this.logFilenamePrefix = logFilenamePrefix;
    this.humanReadableName = humanReadableName;
  }

  public String getKey() {
    return key;
  }

  /**
   * Index used for inter-process communication
   */
  public int getIpcIndex() {
    return ipcIndex;
  }

  /**
   * Prefix of log file, for example "web" for file "web.log"
   */
  public String getLogFilenamePrefix() {
    return logFilenamePrefix;
  }

  public String getHumanReadableName() {
    return humanReadableName;
  }

  public static ProcessId fromKey(String key) {
    for (ProcessId processId : values()) {
      if (processId.getKey().equals(key)) {
        return processId;
      }
    }
    throw new IllegalArgumentException(format("Process [%s] does not exist", key));
  }

}
