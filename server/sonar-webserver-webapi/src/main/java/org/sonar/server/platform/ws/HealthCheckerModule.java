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
package org.sonar.server.platform.ws;

import org.sonar.core.platform.Module;
import org.sonar.server.common.health.AppNodeClusterCheck;
import org.sonar.server.common.health.CeStatusNodeCheck;
import org.sonar.server.common.health.DbConnectionNodeCheck;
import org.sonar.server.common.health.EsStatusClusterCheck;
import org.sonar.server.common.health.EsStatusNodeCheck;
import org.sonar.server.health.HealthCheckerImpl;
import org.sonar.server.common.health.WebServerStatusNodeCheck;
import org.sonar.server.platform.NodeInformation;

public class HealthCheckerModule extends Module {
  private final NodeInformation nodeInformation;

  public HealthCheckerModule(NodeInformation nodeInformation) {
    this.nodeInformation = nodeInformation;
  }

  @Override
  protected void configureModule() {
    // NodeHealthCheck implementations
    add(WebServerStatusNodeCheck.class,
      DbConnectionNodeCheck.class,
      CeStatusNodeCheck.class);
    if (nodeInformation.isStandalone()) {
      add(EsStatusNodeCheck.class);
    } else {
      // ClusterHealthCheck implementations
      add(EsStatusClusterCheck.class,
        AppNodeClusterCheck.class);
    }

    add(HealthCheckerImpl.class);
  }
}
