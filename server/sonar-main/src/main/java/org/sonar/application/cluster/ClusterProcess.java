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
package org.sonar.application.cluster;

import java.io.Serializable;
import org.sonar.process.ProcessId;

import static java.util.Objects.requireNonNull;

public class ClusterProcess implements Serializable {
  private final ProcessId processId;
  private final String nodeUuid;

  public ClusterProcess(String nodeUuid, ProcessId processId) {
    this.processId = requireNonNull(processId);
    this.nodeUuid = requireNonNull(nodeUuid);
  }

  public ProcessId getProcessId() {
    return processId;
  }

  public String getNodeUuid() {
    return nodeUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterProcess that = (ClusterProcess) o;
    if (processId != that.processId) {
      return false;
    }
    return nodeUuid.equals(that.nodeUuid);
  }

  @Override
  public int hashCode() {
    int result = processId.hashCode();
    result = 31 * result + nodeUuid.hashCode();
    return result;
  }
}
