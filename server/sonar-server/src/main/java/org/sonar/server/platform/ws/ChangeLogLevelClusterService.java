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
package org.sonar.server.platform.ws;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.Database;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberSelectors;
import org.sonar.server.app.ServerProcessLogging;
import org.sonar.core.platform.HazelcastDistributedCallComponentContainer;
import org.sonar.server.platform.ServerLogging;

public class ChangeLogLevelClusterService implements ChangeLogLevelService {

  private static final int CLUSTER_TIMEOUT = 5000;
  private static final Logger LOGGER = Loggers.get(ChangeLogLevelClusterService.class);

  private final HazelcastMember member;

  public ChangeLogLevelClusterService(HazelcastMember member) {
    this.member = member;
  }

  public void changeLogLevel(LoggerLevel level) {
    try {
      member.call(setLogLevelForNode(level), HazelcastMemberSelectors.selectorForProcessIds(ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE), CLUSTER_TIMEOUT)
        .propagateExceptions();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static DistributedCall<Object> setLogLevelForNode(LoggerLevel level) {
    return () -> {
      try {
        ComponentContainer componentContainer = HazelcastDistributedCallComponentContainer.get();

        // set SQL log level
        Database db = componentContainer.getComponentByType(Database.class);
        db.enableSqlLogging(level.equals(LoggerLevel.TRACE));

        // set log level of this process
        ServerLogging logging = componentContainer.getComponentByType(ServerLogging.class);
        ServerProcessLogging serverProcessLogging = componentContainer.getComponentByType(ServerProcessLogging.class);
        logging.changeLevel(serverProcessLogging, level);
      } catch (Exception e) {
        LOGGER.error("Setting log level to '" + level.name() + "' in this cluster node failed", e);
        throw new IllegalStateException("Setting log level to '" + level.name() + "' in this cluster node failed", e);
      }
      return null;
    };
  }
}
