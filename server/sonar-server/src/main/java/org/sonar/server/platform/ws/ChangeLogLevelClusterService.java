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
package org.sonar.server.platform.ws;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberSelectors;
import org.sonar.server.log.ServerLogging;

public class ChangeLogLevelClusterService implements ChangeLogLevelService {

  private static final long CLUSTER_TIMEOUT_MILLIS = 5000;
  private static final Logger LOGGER = Loggers.get(ChangeLogLevelClusterService.class);

  private final HazelcastMember member;

  public ChangeLogLevelClusterService(HazelcastMember member) {
    this.member = member;
  }

  public ChangeLogLevelClusterService() {
    this(null);
  }

  public void changeLogLevel(LoggerLevel level) throws InterruptedException {
    member.call(setLogLevelForNode(level), HazelcastMemberSelectors.selectorForProcessIds(ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE), CLUSTER_TIMEOUT_MILLIS)
      .propagateExceptions();
  }

  private static DistributedCall<Object> setLogLevelForNode(LoggerLevel level) {
    return () -> {
      try {
        ServerLogging.changeLevelFromHazelcastDistributedQuery(level);
      } catch (Exception e) {
        LOGGER.error("Setting log level to '" + level.name() + "' in this cluster node failed", e);
        throw new IllegalStateException("Setting log level to '" + level.name() + "' in this cluster node failed", e);
      }
      return null;
    };
  }
}
