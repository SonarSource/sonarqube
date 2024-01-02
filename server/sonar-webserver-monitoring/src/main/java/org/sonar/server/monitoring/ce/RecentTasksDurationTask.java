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
package org.sonar.server.monitoring.ce;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static java.util.Objects.requireNonNull;

public class RecentTasksDurationTask extends ComputeEngineMetricsTask {

  private static final Logger LOGGER = Loggers.get(RecentTasksDurationTask.class);
  private final System2 system;

  private long lastUpdatedTimestamp;

  public RecentTasksDurationTask(DbClient dbClient, ServerMonitoringMetrics metrics, Configuration config,
    System2 system) {
    super(dbClient, metrics, config);
    this.system = system;
    this.lastUpdatedTimestamp = system.now();
  }

  @Override
  public void run() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<CeActivityDto> recentSuccessfulTasks = getRecentSuccessfulTasks(dbSession);

      Collection<String> componentUuids = recentSuccessfulTasks.stream()
        .map(CeActivityDto::getMainComponentUuid)
        .toList();
      List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
      Map<String, String> componentUuidAndKeys = componentDtos.stream()
        .collect(Collectors.toMap(ComponentDto::uuid, ComponentDto::getKey));

      reportObservedDurationForTasks(recentSuccessfulTasks, componentUuidAndKeys);
    }
    lastUpdatedTimestamp = system.now();
  }

  private List<CeActivityDto> getRecentSuccessfulTasks(DbSession dbSession) {
    List<CeActivityDto> recentTasks = dbClient.ceActivityDao().selectNewerThan(dbSession, lastUpdatedTimestamp);
    return recentTasks.stream()
      .filter(c -> c.getStatus() == CeActivityDto.Status.SUCCESS)
      .toList();
  }

  private void reportObservedDurationForTasks(List<CeActivityDto> tasks, Map<String, String> componentUuidAndKeys) {
    for (CeActivityDto task : tasks) {
      String mainComponentUuid = task.getMainComponentUuid();
      Long executionTimeMs = task.getExecutionTimeMs();
      try {
        requireNonNull(mainComponentUuid);
        requireNonNull(executionTimeMs);

        String mainComponentKey = componentUuidAndKeys.get(mainComponentUuid);
        requireNonNull(mainComponentKey);

        metrics.observeComputeEngineTaskDuration(executionTimeMs, task.getTaskType(), mainComponentKey);
      } catch (RuntimeException e) {
        LOGGER.warn("Can't report metric data for a CE task with component uuid " + mainComponentUuid, e);
      }
    }

  }
}
