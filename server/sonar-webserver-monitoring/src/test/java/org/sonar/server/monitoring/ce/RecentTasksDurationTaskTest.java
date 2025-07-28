/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.ce.CeActivityDao;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDao;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecentTasksDurationTaskTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final CeActivityDao ceActivityDao = mock(CeActivityDao.class);
  private final EntityDao entityDao = mock(EntityDao.class);
  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final Configuration config = mock(Configuration.class);
  private final System2 system = mock(System2.class);

  @Before
  public void before() {
    when(dbClient.ceActivityDao()).thenReturn(ceActivityDao);
    when(dbClient.entityDao()).thenReturn(entityDao);
    ComponentDto componentDto = new ComponentDto();
    componentDto.setKey("key");
  }

  @Test
  public void run_given5SuccessfulTasks_observeDurationFor5Tasks() {
    RecentTasksDurationTask task = new RecentTasksDurationTask(dbClient, metrics, config, system);
    List<CeActivityDto> recentTasks = createTasks(5, 0);

    when(entityDao.selectByUuids(any(), any())).thenReturn(createEntityDtos(5));
    when(ceActivityDao.selectNewerThan(any(), anyLong())).thenReturn(recentTasks);

    task.run();

    verify(metrics, times(5)).observeComputeEngineTaskDuration(anyLong(), any(), any());
  }

  @Test
  public void run_given1SuccessfulTasksAnd1Failing_observeDurationFor1Tasks() {
    RecentTasksDurationTask task = new RecentTasksDurationTask(dbClient, metrics, config, system);
    List<CeActivityDto> recentTasks = createTasks(1, 1);

    when(entityDao.selectByUuids(any(), any())).thenReturn(createEntityDtos(1));
    when(ceActivityDao.selectNewerThan(any(), anyLong())).thenReturn(recentTasks);

    task.run();

    verify(metrics, times(1)).observeComputeEngineTaskDuration(anyLong(), any(), any());
  }

  @Test
  public void run_given1TaskWithEntityUuidAnd1Without_observeDurationFor2Tasks() {
    RecentTasksDurationTask task = new RecentTasksDurationTask(dbClient, metrics, config, system);
    List<CeActivityDto> recentTasks = createTasks(2, 0);

    recentTasks.get(0).setEntityUuid(null);

    when(entityDao.selectByUuids(any(), any())).thenReturn(createEntityDtos(1));
    when(ceActivityDao.selectNewerThan(any(), anyLong())).thenReturn(recentTasks);

    task.run();

    verify(metrics, times(1)).observeComputeEngineTaskDuration(anyLong(), any(), any());
    verify(metrics, times(1)).observeComputeEngineSystemTaskDuration(anyLong(), any());
  }

  @Test
  public void run_givenNullExecutionTime_dontReportMetricData() {
    RecentTasksDurationTask task = new RecentTasksDurationTask(dbClient, metrics, config, system);
    List<CeActivityDto> recentTasks = createTasks(1, 0);
    recentTasks.get(0).setExecutionTimeMs(null);

    when(entityDao.selectByUuids(any(), any())).thenReturn(createEntityDtos(1));
    when(ceActivityDao.selectNewerThan(any(), anyLong())).thenReturn(recentTasks);

    task.run();

    verify(metrics, times(0)).observeComputeEngineTaskDuration(anyLong(), any(), any());
  }

  private List<CeActivityDto> createTasks(int numberOfSuccededTasks, int numberOfFailedTasks) {
    List<CeActivityDto> dtos = new ArrayList<>();

    for (int i=0; i<numberOfSuccededTasks; i++) {
      dtos.add(newCeActivityTask(CeActivityDto.Status.SUCCESS));
    }

    for (int i=0; i<numberOfFailedTasks; i++) {
      dtos.add(newCeActivityTask(CeActivityDto.Status.FAILED));
    }

    return dtos;
  }

  private List<EntityDto> createEntityDtos(int number) {
    List<EntityDto> entityDtos = new ArrayList<>();
    for(int i=0; i<5; i++) {
      ProjectDto entity = new ProjectDto();
      entity.setUuid(i + "");
      entity.setKey(i + "");
      entityDtos.add(entity);
    }
    return entityDtos;
  }

  private CeActivityDto newCeActivityTask(CeActivityDto.Status status) {
    CeActivityDto dto = new CeActivityDto(new CeQueueDto());
    dto.setStatus(status);
    dto.setEntityUuid("0");
    dto.setExecutionTimeMs(1000L);
    return dto;
  }
}


