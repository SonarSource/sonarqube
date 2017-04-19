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
package org.sonar.server.ce.ws;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.organization.OrganizationDto;
import org.sonarqube.ws.WsCe;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskFormatterTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private System2 system2 = mock(System2.class);
  private TaskFormatter underTest = new TaskFormatter(db.getDbClient(), system2);

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
    assertThat(wsTask.hasScannerContext()).isFalse();

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
    String uuid = "COMPONENT_UUID";
    OrganizationDto organizationDto = db.organizations().insert();
    db.components().insertPrivateProject(organizationDto, (t) -> t.setUuid(uuid).setKey("COMPONENT_KEY").setName("Component Name"));

    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dto.setCreatedAt(1_450_000_000_000L);
    dto.setStartedAt(1_451_000_000_000L);
    dto.setComponentUuid(uuid);
    dto.setSubmitterLogin("rob");

    WsCe.Task wsTask = underTest.formatQueue(db.getSession(), dto);

    assertThat(wsTask.getType()).isEqualTo("TYPE");
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getComponentId()).isEqualTo(uuid);
    assertThat(wsTask.getComponentKey()).isEqualTo("COMPONENT_KEY");
    assertThat(wsTask.getComponentName()).isEqualTo("Component Name");
    assertThat(wsTask.getComponentQualifier()).isEqualTo("TRK");
    assertThat(wsTask.getStatus()).isEqualTo(WsCe.TaskStatus.IN_PROGRESS);
    assertThat(wsTask.getLogs()).isFalse();
    assertThat(wsTask.getSubmitterLogin()).isEqualTo("rob");
    assertThat(wsTask.hasExecutionTimeMs()).isTrue();
    assertThat(wsTask.hasExecutedAt()).isFalse();
    assertThat(wsTask.hasScannerContext()).isFalse();
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

    Iterable<WsCe.Task> wsTasks = underTest.formatQueue(db.getSession(), asList(dto1, dto2));
    assertThat(wsTasks).extracting("id").containsExactly("UUID1", "UUID2");
  }

  @Test
  public void formatActivity() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED);

    WsCe.Task wsTask = underTest.formatActivity(db.getSession(), dto);

    assertThat(wsTask.getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(wsTask.getSubmittedAt()).isEqualTo(DateUtils.formatDateTime(new Date(1_450_000_000_000L)));
    assertThat(wsTask.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(wsTask.getAnalysisId()).isEqualTo("U1");
    assertThat(wsTask.getLogs()).isFalse();
    assertThat(wsTask.hasScannerContext()).isFalse();
  }

  @Test
  public void formatActivity_set_scanner_context_if_argument_is_non_null() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED);

    String expected = "scanner context baby!";
    WsCe.Task wsTask = underTest.formatActivity(db.getSession(), dto, Optional.absent(), expected);

    assertThat(wsTask.hasScannerContext()).isTrue();
    assertThat(wsTask.getScannerContext()).isEqualTo(expected);
  }

  @Test
  public void formatActivities() {
    CeActivityDto dto1 = newActivity("UUID1", "COMPONENT_UUID", CeActivityDto.Status.FAILED);
    CeActivityDto dto2 = newActivity("UUID2", "COMPONENT_UUID", CeActivityDto.Status.SUCCESS);

    Iterable<WsCe.Task> wsTasks = underTest.formatActivity(db.getSession(), asList(dto1, dto2));

    assertThat(wsTasks).extracting("id").containsExactly("UUID1", "UUID2");
  }

  @Test
  public void formatActivity_with_both_error_message_and_stacktrace() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED)
        .setErrorMessage("error msg")
        .setErrorStacktrace("error stacktrace");

    WsCe.Task task = underTest.formatActivity(db.getSession(), Collections.singletonList(dto)).iterator().next();

    assertThat(task.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(task.getErrorStacktrace()).isEqualTo(dto.getErrorStacktrace());
  }

  @Test
  public void formatActivity_with_both_error_message_only() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED)
        .setErrorMessage("error msg");

    WsCe.Task task = underTest.formatActivity(db.getSession(), Collections.singletonList(dto)).iterator().next();

    assertThat(task.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  @Test
  public void formatActivity_with_both_error_message_and_only_stacktrace_flag() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED)
        .setErrorMessage("error msg");

    WsCe.Task task = underTest.formatActivity(db.getSession(), Collections.singletonList(dto)).iterator().next();

    assertThat(task.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
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
    activityDto.setAnalysisUuid("U1");
    return activityDto;
  }
}
