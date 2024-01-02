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
package org.sonar.ce.notification;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.notifications.Notification;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotification;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotificationBuilder;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotificationSerializer;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.notification.NotificationService;

import static java.util.Collections.singleton;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class ReportAnalysisFailureNotificationExecutionListenerTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final Random random = new Random();
  private final DbClient dbClient = dbTester.getDbClient();
  private final NotificationService notificationService = mock(NotificationService.class);
  private final ReportAnalysisFailureNotificationSerializer serializer = mock(ReportAnalysisFailureNotificationSerializer.class);
  private final System2 system2 = mock(System2.class);
  private final DbClient dbClientMock = mock(DbClient.class);
  private final CeTask ceTaskMock = mock(CeTask.class);
  private final Throwable throwableMock = mock(Throwable.class);
  private final CeTaskResult ceTaskResultMock = mock(CeTaskResult.class);
  private final ReportAnalysisFailureNotificationExecutionListener fullMockedUnderTest = new ReportAnalysisFailureNotificationExecutionListener(
    notificationService, dbClientMock, serializer, system2);
  private final ReportAnalysisFailureNotificationExecutionListener underTest = new ReportAnalysisFailureNotificationExecutionListener(
    notificationService, dbClient, serializer, system2);

  @Test
  public void onStart_has_no_effect() {
    CeTask mockedCeTask = mock(CeTask.class);

    fullMockedUnderTest.onStart(mockedCeTask);

    verifyNoInteractions(mockedCeTask, notificationService, dbClientMock, serializer, system2);
  }

  @Test
  public void onEnd_has_no_effect_if_status_is_SUCCESS() {
    fullMockedUnderTest.onEnd(ceTaskMock, CeActivityDto.Status.SUCCESS, randomDuration(), ceTaskResultMock, throwableMock);

    verifyNoInteractions(ceTaskMock, ceTaskResultMock, throwableMock, notificationService, dbClientMock, serializer, system2);
  }

  @Test
  public void onEnd_has_no_effect_if_CeTask_type_is_not_report() {
    when(ceTaskMock.getType()).thenReturn(randomAlphanumeric(12));

    fullMockedUnderTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, throwableMock);

    verifyNoInteractions(ceTaskResultMock, throwableMock, notificationService, dbClientMock, serializer, system2);
  }

  @Test
  public void onEnd_has_no_effect_if_CeTask_has_no_component_uuid() {
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);

    fullMockedUnderTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, throwableMock);

    verifyNoInteractions(ceTaskResultMock, throwableMock, notificationService, dbClientMock, serializer, system2);
  }

  @Test
  public void onEnd_has_no_effect_if_there_is_no_subscriber_for_ReportAnalysisFailureNotification_type() {
    String componentUuid = randomAlphanumeric(6);
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);
    when(ceTaskMock.getComponent()).thenReturn(Optional.of(new CeTask.Component(componentUuid, null, null)));
    when(notificationService.hasProjectSubscribersForTypes(componentUuid, singleton(ReportAnalysisFailureNotification.class)))
      .thenReturn(false);

    fullMockedUnderTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, throwableMock);

    verifyNoInteractions(ceTaskResultMock, throwableMock, dbClientMock, serializer, system2);
  }

  @Test
  public void onEnd_fails_with_ISE_if_project_does_not_exist_in_DB() {
    String componentUuid = randomAlphanumeric(6);
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);
    when(ceTaskMock.getComponent()).thenReturn(Optional.of(new CeTask.Component(componentUuid, null, null)));
    when(notificationService.hasProjectSubscribersForTypes(componentUuid, singleton(ReportAnalysisFailureNotification.class)))
      .thenReturn(true);

    Duration randomDuration = randomDuration();
    assertThatThrownBy(() -> underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration, ceTaskResultMock, throwableMock))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Could not find project uuid " + componentUuid);
  }

  @Test
  public void onEnd_fails_with_ISE_if_branch_does_not_exist_in_DB() {
    String componentUuid = randomAlphanumeric(6);
    ProjectDto project = new ProjectDto().setUuid(componentUuid).setKey(randomAlphanumeric(5)).setQualifier(Qualifiers.PROJECT);
    dbTester.getDbClient().projectDao().insert(dbTester.getSession(), project);
    dbTester.getSession().commit();
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);
    when(ceTaskMock.getComponent()).thenReturn(Optional.of(new CeTask.Component(componentUuid, null, null)));
    when(notificationService.hasProjectSubscribersForTypes(componentUuid, singleton(ReportAnalysisFailureNotification.class)))
      .thenReturn(true);

    Duration randomDuration = randomDuration();
    assertThatThrownBy(() -> underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration, ceTaskResultMock, throwableMock))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Could not find a branch for project uuid " + componentUuid);
  }

  @Test
  public void onEnd_fails_with_IAE_if_component_is_not_a_project() {
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);
    ComponentDto project = dbTester.components().insertPrivateProject();
    ComponentDto module = dbTester.components().insertComponent(newModuleDto(project));
    ComponentDto directory = dbTester.components().insertComponent(newDirectory(module, randomAlphanumeric(12)));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    ComponentDto view = dbTester.components().insertComponent(ComponentTesting.newPortfolio());
    ComponentDto subView = dbTester.components().insertComponent(ComponentTesting.newSubPortfolio(view));
    ComponentDto projectCopy = dbTester.components().insertComponent(ComponentTesting.newProjectCopy(project, subView));
    ComponentDto application = dbTester.components().insertComponent(ComponentTesting.newApplication());

    Arrays.asList(module, directory, file, view, subView, projectCopy, application)
      .forEach(component -> {

      when(ceTaskMock.getComponent()).thenReturn(Optional.of(new CeTask.Component(component.uuid(), null, null)));
      when(notificationService.hasProjectSubscribersForTypes(component.uuid(), singleton(ReportAnalysisFailureNotification.class)))
        .thenReturn(true);

        Duration randomDuration = randomDuration();
        try {
          underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration, ceTaskResultMock, throwableMock);

          fail("An IllegalArgumentException should have been thrown for component " + component);
        } catch (IllegalArgumentException e) {
          assertThat(e.getMessage()).isEqualTo(String.format("Component %s must be a project (qualifier=%s)", component.uuid(), component.qualifier()));
        } catch (IllegalStateException e) {
          assertThat(e.getMessage()).isEqualTo("Could not find project uuid " + component.uuid());
        }
      });
  }

  @Test
  public void onEnd_fails_with_RowNotFoundException_if_activity_for_task_does_not_exist_in_DB() {
    String componentUuid = randomAlphanumeric(6);
    String taskUuid = randomAlphanumeric(6);
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);
    when(ceTaskMock.getUuid()).thenReturn(taskUuid);
    when(ceTaskMock.getComponent()).thenReturn(Optional.of(new CeTask.Component(componentUuid, null, null)));
    when(notificationService.hasProjectSubscribersForTypes(componentUuid, singleton(ReportAnalysisFailureNotification.class)))
      .thenReturn(true);
    dbTester.components().insertPrivateProject(s -> s.setUuid(componentUuid));

    Duration randomDuration = randomDuration();
    assertThatThrownBy(() -> underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration, ceTaskResultMock, throwableMock))
      .isInstanceOf(RowNotFoundException.class)
      .hasMessage("CeActivity with uuid '" + taskUuid + "' not found");
  }

  @Test
  public void onEnd_creates_notification_with_data_from_activity_and_project_and_deliver_it() {
    String taskUuid = randomAlphanumeric(12);
    int createdAt = random.nextInt(999_999);
    long executedAt = random.nextInt(999_999);
    ComponentDto project = initMocksToPassConditions(taskUuid, createdAt, executedAt);
    Notification notificationMock = mockSerializer();

    underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, throwableMock);

    ArgumentCaptor<ReportAnalysisFailureNotificationBuilder> notificationCaptor = verifyAndCaptureSerializedNotification();
    verify(notificationService).deliver(same(notificationMock));

    ReportAnalysisFailureNotificationBuilder reportAnalysisFailureNotificationBuilder = notificationCaptor.getValue();

    ReportAnalysisFailureNotificationBuilder.Project notificationProject = reportAnalysisFailureNotificationBuilder.project();
    assertThat(notificationProject.name()).isEqualTo(project.name());
    assertThat(notificationProject.key()).isEqualTo(project.getKey());
    assertThat(notificationProject.uuid()).isEqualTo(project.uuid());
    assertThat(notificationProject.branchName()).isNull();
    ReportAnalysisFailureNotificationBuilder.Task notificationTask = reportAnalysisFailureNotificationBuilder.task();
    assertThat(notificationTask.uuid()).isEqualTo(taskUuid);
    assertThat(notificationTask.createdAt()).isEqualTo(createdAt);
    assertThat(notificationTask.failedAt()).isEqualTo(executedAt);
  }

  @Test
  public void onEnd_creates_notification_with_error_message_from_Throwable_argument_message() {
    initMocksToPassConditions(randomAlphanumeric(12), random.nextInt(999_999), (long) random.nextInt(999_999));
    String message = randomAlphanumeric(66);
    when(throwableMock.getMessage()).thenReturn(message);

    underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, throwableMock);

    ArgumentCaptor<ReportAnalysisFailureNotificationBuilder> notificationCaptor = verifyAndCaptureSerializedNotification();

    ReportAnalysisFailureNotificationBuilder reportAnalysisFailureNotificationBuilder = notificationCaptor.getValue();
    assertThat(reportAnalysisFailureNotificationBuilder.errorMessage()).isEqualTo(message);
  }

  @Test
  public void onEnd_creates_notification_with_null_error_message_if_Throwable_is_null() {
    String taskUuid = randomAlphanumeric(12);
    initMocksToPassConditions(taskUuid, random.nextInt(999_999), (long) random.nextInt(999_999));
    Notification notificationMock = mockSerializer();

    underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, null);

    verify(notificationService).deliver(same(notificationMock));
    ArgumentCaptor<ReportAnalysisFailureNotificationBuilder> notificationCaptor = verifyAndCaptureSerializedNotification();

    ReportAnalysisFailureNotificationBuilder reportAnalysisFailureNotificationBuilder = notificationCaptor.getValue();
    assertThat(reportAnalysisFailureNotificationBuilder.errorMessage()).isNull();
  }

  @Test
  public void onEnd_ignores_null_CeTaskResult_argument() {
    String taskUuid = randomAlphanumeric(12);
    initMocksToPassConditions(taskUuid, random.nextInt(999_999), (long) random.nextInt(999_999));
    Notification notificationMock = mockSerializer();

    underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), null, null);

    verify(notificationService).deliver(same(notificationMock));
  }

  @Test
  public void onEnd_ignores_CeTaskResult_argument() {
    String taskUuid = randomAlphanumeric(12);
    initMocksToPassConditions(taskUuid, random.nextInt(999_999), (long) random.nextInt(999_999));
    Notification notificationMock = mockSerializer();

    underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, null);

    verify(notificationService).deliver(same(notificationMock));
    verifyNoInteractions(ceTaskResultMock);
  }

  @Test
  public void onEnd_uses_system_data_as_failedAt_if_task_has_no_executedAt() {
    String taskUuid = randomAlphanumeric(12);
    initMocksToPassConditions(taskUuid, random.nextInt(999_999), null);
    long now = random.nextInt(999_999);
    when(system2.now()).thenReturn(now);
    Notification notificationMock = mockSerializer();

    underTest.onEnd(ceTaskMock, CeActivityDto.Status.FAILED, randomDuration(), ceTaskResultMock, null);

    verify(notificationService).deliver(same(notificationMock));
    ArgumentCaptor<ReportAnalysisFailureNotificationBuilder> notificationCaptor = verifyAndCaptureSerializedNotification();
    assertThat(notificationCaptor.getValue().task().failedAt()).isEqualTo(now);
  }

  private ReportAnalysisFailureNotification mockSerializer() {
    ReportAnalysisFailureNotification notificationMock = mock(ReportAnalysisFailureNotification.class);
    when(serializer.toNotification(any(ReportAnalysisFailureNotificationBuilder.class))).thenReturn(notificationMock);
    return notificationMock;
  }

  private ComponentDto initMocksToPassConditions(String taskUuid, int createdAt, @Nullable Long executedAt) {
    ComponentDto project = random.nextBoolean() ? dbTester.components().insertPrivateProject() : dbTester.components().insertPublicProject();
    when(ceTaskMock.getType()).thenReturn(CeTaskTypes.REPORT);
    when(ceTaskMock.getComponent()).thenReturn(Optional.of(new CeTask.Component(project.uuid(), null, null)));
    when(ceTaskMock.getUuid()).thenReturn(taskUuid);
    when(notificationService.hasProjectSubscribersForTypes(project.uuid(), singleton(ReportAnalysisFailureNotification.class)))
      .thenReturn(true);
    insertActivityDto(taskUuid, createdAt, executedAt, project);
    return project;
  }

  private void insertActivityDto(String taskUuid, int createdAt, @Nullable Long executedAt, ComponentDto project) {
    dbClient.ceActivityDao().insert(dbTester.getSession(), new CeActivityDto(new CeQueueDto()
      .setUuid(taskUuid)
      .setTaskType(CeTaskTypes.REPORT)
      .setComponentUuid(project.uuid())
      .setCreatedAt(createdAt))
      .setExecutedAt(executedAt)
      .setStatus(CeActivityDto.Status.FAILED));
    dbTester.getSession().commit();
  }

  private ArgumentCaptor<ReportAnalysisFailureNotificationBuilder> verifyAndCaptureSerializedNotification() {
    ArgumentCaptor<ReportAnalysisFailureNotificationBuilder> notificationCaptor = ArgumentCaptor.forClass(ReportAnalysisFailureNotificationBuilder.class);
    verify(serializer).toNotification(notificationCaptor.capture());
    return notificationCaptor;
  }

  private Duration randomDuration() {
    return Duration.of(random.nextLong(), ChronoUnit.MILLIS);
  }
}
