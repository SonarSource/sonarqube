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

package org.sonar.application.cluster;

import java.io.Serializable;
import javax.annotation.Nonnull;
import org.sonar.process.ProcessId;

public class ClusterProcess implements Serializable {
  private final ProcessId processId;
  private final String nodeUuid;

  public ClusterProcess(@Nonnull String nodeUuid, @Nonnull ProcessId processId) {
    this.processId = processId;
    this.nodeUuid = nodeUuid;
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
    if (!(o instanceof ClusterProcess)) {
      return false;
    }

    ClusterProcess that = (ClusterProcess) o;

    if (processId != that.processId) {
      return false;
    }
    return nodeUuid != null ? nodeUuid.equals(that.nodeUuid) : that.nodeUuid == null;
  }

  @Override
  public int hashCode() {
    int result = processId != null ? processId.hashCode() : 0;
    result = 31 * result + (nodeUuid != null ? nodeUuid.hashCode() : 0);
    return result;
  }
}
