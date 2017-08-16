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

import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.tuple;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeTaskCharacteristicDto.INCREMENTAL_KEY;
import static org.sonar.server.ce.ws.ComponentAction.PARAM_COMPONENT_ID;
import static org.sonar.server.ce.ws.ComponentAction.PARAM_COMPONENT_KEY;

public class ComponentActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private TaskFormatter formatter = new TaskFormatter(db.getDbClient(), System2.INSTANCE);
  private ComponentAction underTest = new ComponentAction(userSession, db.getDbClient(), formatter, TestComponentFinder.from(db));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void empty_queue_and_empty_activity() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam("componentId", project.uuid())
      .executeProtobuf(WsCe.ProjectResponse.class);

    assertThat(response.getQueueCount()).isEqualTo(0);
    assertThat(response.hasCurrent()).isFalse();
  }

  @Test
  public void project_tasks() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    SnapshotDto analysisProject1 = db.components().insertSnapshot(project1);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    userSession.addProjectPermission(UserRole.USER, project1);
    insertActivity("T1", project1, CeActivityDto.Status.SUCCESS, analysisProject1);
    insertActivity("T2", project2, CeActivityDto.Status.FAILED, null);
    insertActivity("T3", project1, CeActivityDto.Status.FAILED, null);
    insertQueue("T4", project1, CeQueueDto.Status.IN_PROGRESS);
    insertQueue("T5", project1, CeQueueDto.Status.PENDING);

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam("componentId", project1.uuid())
      .executeProtobuf(WsCe.ProjectResponse.class);
    assertThat(response.getQueueCount()).isEqualTo(2);
    assertThat(response.getQueue(0).getId()).isEqualTo("T4");
    assertThat(response.getQueue(1).getId()).isEqualTo("T5");
    // T3 is the latest task executed on PROJECT_1
    assertThat(response.hasCurrent()).isTrue();
    assertThat(response.getCurrent().getId()).isEqualTo("T3");
    assertThat(response.getCurrent().hasAnalysisId()).isFalse();
    assertThat(response.getQueueList())
      .extracting(WsCe.Task::getOrganization)
      .containsOnly(organization.getKey());
    assertThat(response.getCurrent().getOrganization()).isEqualTo(organization.getKey());
    assertThat(response.getCurrent().getIncremental()).isFalse();
  }

  @Test
  public void search_tasks_by_component_key() {
    ComponentDto project = db.components().insertPrivateProject();
    logInWithBrowsePermission(project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    insertActivity("T1", project, CeActivityDto.Status.SUCCESS, analysis);

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEY, project.getDbKey())
      .executeProtobuf(WsCe.ProjectResponse.class);
    assertThat(response.hasCurrent()).isTrue();
    assertThat(response.getCurrent().getId()).isEqualTo("T1");
    assertThat(response.getCurrent().getAnalysisId()).isEqualTo(analysis.getUuid());
  }

  @Test
  public void canceled_tasks_must_not_be_picked_as_current_analysis() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    insertActivity("T1", project, CeActivityDto.Status.SUCCESS);
    insertActivity("T2", project, CeActivityDto.Status.FAILED);
    insertActivity("T3", project, CeActivityDto.Status.SUCCESS);
    insertActivity("T4", project, CeActivityDto.Status.CANCELED);
    insertActivity("T5", project, CeActivityDto.Status.CANCELED);

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam("componentId", project.uuid())
      .executeProtobuf(WsCe.ProjectResponse.class);
    assertThat(response.getQueueCount()).isEqualTo(0);
    // T3 is the latest task executed on PROJECT_1 ignoring Canceled ones
    assertThat(response.hasCurrent()).isTrue();
    assertThat(response.getCurrent().getId()).isEqualTo("T3");
  }

  @Test
  public void incremental_analysis_by_component_id() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    SnapshotDto incrementalAnalysis = db.components().insertSnapshot(project, s -> s.setIncremental(true));
    insertActivity("T1", project, SUCCESS, incrementalAnalysis);

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam("componentId", project.uuid())
      .executeProtobuf(WsCe.ProjectResponse.class);

    assertThat(response.getCurrent())
      .extracting(WsCe.Task::getId, WsCe.Task::getIncremental)
      .containsExactlyInAnyOrder("T1", true);
  }

  @Test
  public void incremental_analysis_by_component_key() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    SnapshotDto incrementalAnalysis = db.components().insertSnapshot(project, s -> s.setIncremental(true));
    insertActivity("T1", project, SUCCESS, incrementalAnalysis);

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam("componentKey", project.getKey())
      .executeProtobuf(WsCe.ProjectResponse.class);

    assertThat(response.getCurrent())
      .extracting(WsCe.Task::getId, WsCe.Task::getIncremental)
      .containsExactlyInAnyOrder("T1", true);
  }

  @Test
  public void incremental_on_in_queue_analysis() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.addProjectPermission(UserRole.USER, project);
    CeQueueDto queue1 = insertQueue("T1", project, CeQueueDto.Status.IN_PROGRESS);
    insertCharacteristic(queue1, INCREMENTAL_KEY, "true");
    CeQueueDto queue2 = insertQueue("T2", project, CeQueueDto.Status.PENDING);
    insertCharacteristic(queue2, INCREMENTAL_KEY, "true");

    WsCe.ProjectResponse response = ws.newRequest()
      .setParam("componentId", project.uuid())
      .executeProtobuf(WsCe.ProjectResponse.class);

    assertThat(response.getQueueList())
      .extracting(WsCe.Task::getId, WsCe.Task::getIncremental)
      .containsOnly(
        tuple("T1", true),
        tuple("T2", true)
      );
  }

  @Test
  public void fail_with_404_when_component_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("componentId", "UNKNOWN")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_user_cant_access_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, project.uuid())
      .execute();
  }

  @Test
  public void fail_when_no_component_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    logInWithBrowsePermission(db.components().insertPrivateProject());

    ws.newRequest().execute();
  }

  private void logInWithBrowsePermission(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.USER, project);
  }

  private CeQueueDto insertQueue(String taskUuid, ComponentDto component, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(component.uuid());
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    db.getDbClient().ceQueueDao().insert(db.getSession(), queueDto);
    db.getSession().commit();
    return queueDto;
  }

  private CeActivityDto insertActivity(String taskUuid, ComponentDto component, CeActivityDto.Status status) {
    return insertActivity(taskUuid, component, status, db.components().insertSnapshot(component));
  }

  private CeActivityDto insertActivity(String taskUuid, ComponentDto component, CeActivityDto.Status status, @Nullable SnapshotDto analysis) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(component.uuid());
    queueDto.setUuid(taskUuid);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(analysis == null ? null : analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.getSession().commit();
    return activityDto;
  }

  private CeTaskCharacteristicDto insertCharacteristic(CeQueueDto queueDto, String key, String value) {
    CeTaskCharacteristicDto dto = new CeTaskCharacteristicDto()
      .setUuid(Uuids.createFast())
      .setTaskUuid(queueDto.getUuid())
      .setKey(key)
      .setValue(value);
    db.getDbClient().ceTaskCharacteristicsDao().insert(db.getSession(), Collections.singletonList(dto));
    db.commit();
    return dto;
  }
}
