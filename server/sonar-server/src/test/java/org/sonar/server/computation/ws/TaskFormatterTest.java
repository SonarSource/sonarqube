/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.ws;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonarqube.ws.WsCe;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskFormatterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  System2 system2 = mock(System2.class);
  CeLogging ceLogging = mock(CeLogging.class, Mockito.RETURNS_DEEP_STUBS);
  TaskFormatter underTest = new TaskFormatter(db.getDbClient(), ceLogging, system2);

  @Test
  public void formatQueue_without_component() {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setCreatedAt(1_450_000_000_000L);

    WsCe.Task wsTask = underTest.formatQueue(db.getSession(), dto);

    assertThat(wsTask.getType()).isEqualTo("TYPE");
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getStatus()).isEqualTo(WsCe.TaskStatus.PENDING);
    assertThat(wsTask.getLogs()).isFalse();
    assertThat(wsTask.getSubmittedAt()).isEqualTo(DateUtils.formatDateTime(new Date(1_450_000_000_000L)));

    assertThat(wsTask.hasExecutionTimeMs()).isFalse();
    assertThat(wsTask.hasSubmitterLogin()).isFalse();
    assertThat(wsTask.hasComponentId()).isFalse();
    assertThat(wsTask.hasComponentKey()).isFalse();
    assertThat(wsTask.hasComponentName()).isFalse();
    assertThat(wsTask.hasExecutedAt()).isFalse();
    assertThat(wsTask.hasStartedAt()).isFalse();
    assertThat(wsTask.hasExecutionTimeMs()).isFalse();
  }

  @Test
  public void formatQueue_with_component_and_other_fields() throws IOException {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.of(temp.newFile()));
    db.getDbClient().componentDao().insert(db.getSession(), new ComponentDto()
      .setUuid("COMPONENT_UUID").setKey("COMPONENT_KEY").setName("Component Name").setQualifier(Qualifiers.PROJECT));

    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dto.setCreatedAt(1_450_000_000_000L);
    dto.setStartedAt(1_451_000_000_000L);
    dto.setComponentUuid("COMPONENT_UUID");
    dto.setSubmitterLogin("rob");

    WsCe.Task wsTask = underTest.formatQueue(db.getSession(), dto);

    assertThat(wsTask.getType()).isEqualTo("TYPE");
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getComponentId()).isEqualTo("COMPONENT_UUID");
    assertThat(wsTask.getComponentKey()).isEqualTo("COMPONENT_KEY");
    assertThat(wsTask.getComponentName()).isEqualTo("Component Name");
    assertThat(wsTask.getComponentQualifier()).isEqualTo("TRK");
    assertThat(wsTask.getStatus()).isEqualTo(WsCe.TaskStatus.IN_PROGRESS);
    assertThat(wsTask.getLogs()).isTrue();
    assertThat(wsTask.getSubmitterLogin()).isEqualTo("rob");
    assertThat(wsTask.hasExecutionTimeMs()).isTrue();
    assertThat(wsTask.hasExecutedAt()).isFalse();
  }

  @Test
  public void formatQueue_do_not_fail_if_component_not_found() throws Exception {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dto.setCreatedAt(1_450_000_000_000L);
    dto.setComponentUuid("DOES_NOT_EXIST");

    WsCe.Task wsTask = underTest.formatQueue(db.getSession(), dto);

    assertThat(wsTask.getComponentId()).isEqualTo("DOES_NOT_EXIST");
    assertThat(wsTask.hasComponentKey()).isFalse();
    assertThat(wsTask.hasComponentName()).isFalse();
  }

  @Test
  public void formatQueue_compute_execute_time_if_in_progress() {
    long startedAt = 1_450_000_001_000L;
    long now = 1_450_000_003_000L;
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dto.setCreatedAt(1_450_000_000_000L);
    dto.setStartedAt(startedAt);
    when(system2.now()).thenReturn(now);

    WsCe.Task wsTask = underTest.formatQueue(db.getSession(), dto);

    assertThat(wsTask.getExecutionTimeMs()).isEqualTo(now-startedAt);
  }

  @Test
  public void formatQueues() throws Exception {
    CeQueueDto dto1 = new CeQueueDto();
    dto1.setUuid("UUID1");
    dto1.setTaskType("TYPE1");
    dto1.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dto1.setCreatedAt(1_450_000_000_000L);

    CeQueueDto dto2 = new CeQueueDto();
    dto2.setUuid("UUID2");
    dto2.setTaskType("TYPE2");
    dto2.setStatus(CeQueueDto.Status.PENDING);
    dto2.setCreatedAt(1_451_000_000_000L);

    List<WsCe.Task> wsTasks = underTest.formatQueue(db.getSession(), asList(dto1, dto2));
    assertThat(wsTasks).extracting("id").containsExactly("UUID1", "UUID2");
  }

  @Test
  public void formatActivity() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED);

    WsCe.Task wsTask = underTest.formatActivity(db.getSession(), dto);

    assertThat(wsTask.getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(wsTask.getLogs()).isFalse();
    assertThat(wsTask.getSubmittedAt()).isEqualTo(DateUtils.formatDateTime(new Date(1_450_000_000_000L)));
    assertThat(wsTask.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(wsTask.getAnalysisId()).isEqualTo("123456");
    assertThat(wsTask.getLogs()).isFalse();
  }

  @Test
  public void formatActivity_has_logs() throws IOException {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.of(temp.newFile()));
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED);

    WsCe.Task wsTask = underTest.formatActivity(db.getSession(), dto);

    assertThat(wsTask.getLogs()).isTrue();
  }

  @Test
  public void formatActivities() {
    CeActivityDto dto1 = newActivity("UUID1", "COMPONENT_UUID", CeActivityDto.Status.FAILED);
    CeActivityDto dto2 = newActivity("UUID2", "COMPONENT_UUID", CeActivityDto.Status.SUCCESS);

    List<WsCe.Task> wsTasks = underTest.formatActivity(db.getSession(), asList(dto1, dto2));

    assertThat(wsTasks).extracting("id").containsExactly("UUID1", "UUID2");
  }

  private CeActivityDto newActivity(String taskUuid, String componentUuid, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setCreatedAt(1_450_000_000_000L);
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setSnapshotId(123_456L);
    return activityDto;
  }
}
