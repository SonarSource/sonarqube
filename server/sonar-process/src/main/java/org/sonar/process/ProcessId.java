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
package org.sonar.process;

import static java.lang.String.format;

public enum ProcessId {

  APP("app", 0, "sonar"),
  ELASTICSEARCH("es", 1, "es"),
  WEB_SERVER("web", 2, "web"),
  COMPUTE_ENGINE("ce", 3, "ce");

  private final String key;
  private final int ipcIndex;
  private final String logFilenamePrefix;


  ProcessId(String key, int ipcIndex, String logFilenamePrefix) {
    this.key = key;
    this.ipcIndex = ipcIndex;
    this.logFilenamePrefix = logFilenamePrefix;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    sb.append("key='").append(key).append('\'');
    sb.append(", ipcIndex=").append(ipcIndex);
    sb.append(", logFilenamePrefix=").append(logFilenamePrefix);
    sb.append(']');
    return sb.toString();
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
