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
package org.sonar.server.ce.ws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Ce;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeQueueTesting.makeInProgress;
import static org.sonar.db.dismissmessage.MessageType.INFO;
import static org.sonar.db.dismissmessage.MessageType.PROJECT_NCD_PAGE_90;

public class TaskFormatterTest {

  private static final String NODE_NAME = "nodeName1";
  private static final int WARNING_COUNT = 5;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final System2 system2 = mock(System2.class);
  private final TaskFormatter underTest = new TaskFormatter(db.getDbClient(), system2);

  @Test
  public void formatQueue_without_component() {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setCreatedAt(1_450_000_000_000L);

    Ce.Task wsTask = underTest.formatQueue(db.getSession(), dto);

    assertThat(wsTask.getType()).isEqualTo("TYPE");
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getStatus()).isEqualTo(Ce.TaskStatus.PENDING);
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
  public void formatQueue_with_component_and_other_fields() {
    String uuid = "COMPONENT_UUID";
    db.components().insertPrivateProject((t) -> t.setUuid(uuid).setKey("COMPONENT_KEY").setName("Component Name")).getMainBranchComponent();
    UserDto user = db.users().insertUser();

    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setCreatedAt(1_450_000_000_000L);
    dto.setComponentUuid(uuid);
    dto.setSubmitterUuid(user.getUuid());
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    makeInProgress(db.getSession(), "workerUuid", 1_958_000_000_000L, dto);
    CeQueueDto inProgress = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), dto.getUuid()).get();

    Ce.Task wsTask = underTest.formatQueue(db.getSession(), inProgress);

    assertThat(wsTask.getType()).isEqualTo("TYPE");
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getComponentId()).isEqualTo(uuid);
    assertThat(wsTask.getComponentKey()).isEqualTo("COMPONENT_KEY");
    assertThat(wsTask.getComponentName()).isEqualTo("Component Name");
    assertThat(wsTask.getComponentQualifier()).isEqualTo("TRK");
    assertThat(wsTask.getStatus()).isEqualTo(Ce.TaskStatus.IN_PROGRESS);
    assertThat(wsTask.getSubmitterLogin()).isEqualTo(user.getLogin());
    assertThat(wsTask.hasExecutionTimeMs()).isTrue();
    assertThat(wsTask.hasExecutedAt()).isFalse();
    assertThat(wsTask.hasScannerContext()).isFalse();
  }

  @Test
  public void formatQueue_do_not_fail_if_component_not_found() {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid("UUID");
    dto.setTaskType("TYPE");
    dto.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dto.setCreatedAt(1_450_000_000_000L);
    dto.setComponentUuid("DOES_NOT_EXIST");

    Ce.Task wsTask = underTest.formatQueue(db.getSession(), dto);

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
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setCreatedAt(1_450_000_000_000L);
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    makeInProgress(db.getSession(), "workerUuid", startedAt, dto);
    CeQueueDto inProgress = db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), dto.getUuid()).get();
    when(system2.now()).thenReturn(now);

    Ce.Task wsTask = underTest.formatQueue(db.getSession(), inProgress);

    assertThat(wsTask.getExecutionTimeMs()).isEqualTo(now - startedAt);
  }

  @Test
  public void formatQueues() {
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

    Iterable<Ce.Task> wsTasks = underTest.formatQueue(db.getSession(), asList(dto1, dto2));
    assertThat(wsTasks).extracting("id").containsExactly("UUID1", "UUID2");
  }

  @Test
  public void formatActivity() {
    UserDto user = db.users().insertUser();
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, user);

    Ce.Task wsTask = underTest.formatActivity(db.getSession(), dto, null);

    assertThat(wsTask.getType()).isEqualTo(CeTaskTypes.REPORT);
    assertThat(wsTask.getId()).isEqualTo("UUID");
    assertThat(wsTask.getNodeName()).isEqualTo(NODE_NAME);
    assertThat(wsTask.getStatus()).isEqualTo(Ce.TaskStatus.FAILED);
    assertThat(wsTask.getSubmittedAt()).isEqualTo(DateUtils.formatDateTime(new Date(1_450_000_000_000L)));
    assertThat(wsTask.getSubmitterLogin()).isEqualTo(user.getLogin());
    assertThat(wsTask.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(wsTask.getAnalysisId()).isEqualTo("U1");
    assertThat(wsTask.hasScannerContext()).isFalse();
    assertThat(wsTask.getWarningCount()).isZero();
    assertThat(wsTask.getWarningsList()).isEmpty();
    assertThat(wsTask.getInfoMessagesList()).isEmpty();
  }

  @Test
  public void formatActivity_set_scanner_context_if_argument_is_non_null() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, null);

    String expected = "scanner context baby!";
    Ce.Task wsTask = underTest.formatActivity(db.getSession(), dto, expected);

    assertThat(wsTask.hasScannerContext()).isTrue();
    assertThat(wsTask.getScannerContext()).isEqualTo(expected);
  }

  @Test
  public void formatActivity_filterWarnings_andSetWarningsAndCount() {
    TestActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, null);
    CeTaskMessageDto warning1 = createCeTaskMessageDto(1998, MessageType.GENERIC);
    CeTaskMessageDto warning2 = createCeTaskMessageDto(1999, MessageType.GENERIC);

    List<CeTaskMessageDto> ceTaskMessageDtos = new ArrayList<>(dto.getCeTaskMessageDtos());
    ceTaskMessageDtos.add(warning1);
    ceTaskMessageDtos.add(warning2);
    dto.setCeTaskMessageDtos(ceTaskMessageDtos);

    Ce.Task wsTask = underTest.formatActivity(db.getSession(), dto, null);

    assertThat(wsTask.getWarningCount()).isEqualTo(2);
    assertThat(wsTask.getWarningsList()).hasSameElementsAs(getMessagesText(List.of(warning1, warning2)));
  }

  @Test
  public void formatActivity_filterInformation_andSetInformationMessages() {
    TestActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, null);
    CeTaskMessageDto nonInfo1 = createCeTaskMessageDto(1998, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    CeTaskMessageDto nonInfo2 = createCeTaskMessageDto(1999, MessageType.GENERIC);
    CeTaskMessageDto nonInfo3 = createCeTaskMessageDto(2000, MessageType.BRANCH_NCD_90);
    CeTaskMessageDto info2 = createCeTaskMessageDto(2002, INFO);
    CeTaskMessageDto info1 = createCeTaskMessageDto(2001, INFO);

    List<CeTaskMessageDto> ceTaskMessageDtos = new ArrayList<>(dto.getCeTaskMessageDtos());
    ceTaskMessageDtos.addAll(Set.of(nonInfo1, nonInfo2, nonInfo3, info1, info2));
    dto.setCeTaskMessageDtos(ceTaskMessageDtos);

    Ce.Task wsTask = underTest.formatActivity(db.getSession(), dto, null);

    assertThat(wsTask.getInfoMessagesList()).hasSameElementsAs(getMessagesText(List.of(info1, info2)));
  }

  private static List<String> getMessagesText(List<CeTaskMessageDto> ceTaskMessageDtos) {
    return ceTaskMessageDtos.stream().map(CeTaskMessageDto::getMessage).toList();
  }

  @Test
  public void formatActivities() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    CeActivityDto dto1 = newActivity("UUID1", "COMPONENT_UUID", CeActivityDto.Status.FAILED, user1);
    CeActivityDto dto2 = newActivity("UUID2", "COMPONENT_UUID", CeActivityDto.Status.SUCCESS, user2);

    Iterable<Ce.Task> wsTasks = underTest.formatActivity(db.getSession(), asList(dto1, dto2));

    assertThat(wsTasks)
      .extracting(Ce.Task::getId, Ce.Task::getSubmitterLogin)
      .containsExactlyInAnyOrder(
        tuple("UUID1", user1.getLogin()),
        tuple("UUID2", user2.getLogin()));
  }

  @Test
  public void formatActivity_with_both_error_message_and_stacktrace() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, null)
      .setErrorMessage("error msg")
      .setErrorStacktrace("error stacktrace")
      .setErrorType("anErrorType");

    Ce.Task task = underTest.formatActivity(db.getSession(), Collections.singletonList(dto)).iterator().next();

    assertThat(task.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(task.getErrorStacktrace()).isEqualTo(dto.getErrorStacktrace());
    assertThat(task.getErrorType()).isEqualTo(dto.getErrorType());
  }

  @Test
  public void formatActivity_with_both_error_message_only() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, null)
      .setErrorMessage("error msg");

    Ce.Task task = underTest.formatActivity(db.getSession(), Collections.singletonList(dto)).iterator().next();

    assertThat(task.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  @Test
  public void formatActivity_with_both_error_message_and_only_stacktrace_flag() {
    CeActivityDto dto = newActivity("UUID", "COMPONENT_UUID", CeActivityDto.Status.FAILED, null)
      .setErrorMessage("error msg");

    Ce.Task task = underTest.formatActivity(db.getSession(), Collections.singletonList(dto)).iterator().next();

    assertThat(task.getErrorMessage()).isEqualTo(dto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  private TestActivityDto newActivity(String taskUuid, String componentUuid, CeActivityDto.Status status, @Nullable UserDto user) {
    CeQueueDto queueDto = new CeQueueDto()
      .setCreatedAt(1_450_000_000_000L)
      .setTaskType(CeTaskTypes.REPORT)
      .setComponentUuid(componentUuid)
      .setSubmitterUuid(user == null ? null : user.getUuid())
      .setUuid(taskUuid);
    TestActivityDto testActivityDto = new TestActivityDto(queueDto);

    List<CeTaskMessageDto> ceTaskMessageDtos = IntStream.range(0, WARNING_COUNT)
      .mapToObj(i -> createCeTaskMessageDto(i, PROJECT_NCD_PAGE_90))
      .toList();
    testActivityDto.setCeTaskMessageDtos(ceTaskMessageDtos);
    return (TestActivityDto) testActivityDto
      .setStatus(status)
      .setNodeName(NODE_NAME)
      .setExecutionTimeMs(500L)
      .setAnalysisUuid("U1");
  }

  private CeTaskMessageDto createCeTaskMessageDto(int i, MessageType messageType) {
    CeTaskMessageDto ceTaskMessageDto = new CeTaskMessageDto();
    ceTaskMessageDto.setMessage("message_" + i);
    ceTaskMessageDto.setCreatedAt(system2.now());
    ceTaskMessageDto.setTaskUuid("uuid_" + i);
    ceTaskMessageDto.setType(messageType);
    return ceTaskMessageDto;
  }

  private static class TestActivityDto extends CeActivityDto {

    public TestActivityDto(CeQueueDto queueDto) {
      super(queueDto);
    }

    @Override
    public CeActivityDto setHasScannerContext(boolean hasScannerContext) {
      return super.setHasScannerContext(hasScannerContext);
    }

    @Override
    public CeActivityDto setCeTaskMessageDtos(List<CeTaskMessageDto> ceTaskMessageDtos) {
      return super.setCeTaskMessageDtos(ceTaskMessageDtos);
    }
  }
}
