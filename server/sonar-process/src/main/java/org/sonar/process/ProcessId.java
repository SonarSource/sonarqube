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
package org.sonar.process;

public enum ProcessId {

  APP("app", 0), ELASTICSEARCH("es", 1), WEB_SERVER("web", 2), COMPUTE_ENGINE("ce", 3);

  private final String key;
  private final int ipcIndex;

  ProcessId(String key, int ipcIndex) {
    this.key = key;
    this.ipcIndex = ipcIndex;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    sb.append("key='").append(key).append('\'');
    sb.append(", ipcIndex=").append(ipcIndex);
    sb.append(']');
    return sb.toString();
  }
}
