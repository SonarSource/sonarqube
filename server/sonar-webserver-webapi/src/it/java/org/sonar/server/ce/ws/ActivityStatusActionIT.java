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
package org.sonar.server.ce.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeQueueTesting.newCeQueueDto;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.test.JsonAssert.assertJson;

public class ActivityStatusActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final System2 system2 = mock(System2.class);
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final WsActionTester ws = new WsActionTester(new ActivityStatusAction(userSession, dbClient, TestComponentFinder.from(db), system2));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("activity_status");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
    assertThat(def.params()).extracting(WebService.Param::key).containsOnly("component");
  }

  @Test
  public void json_example() {
    when(system2.now()).thenReturn(200123L);
    dbClient.ceQueueDao().insert(dbSession, newCeQueueDto("ce-queue-uuid-1").setStatus(CeQueueDto.Status.PENDING).setCreatedAt(100000));
    dbClient.ceQueueDao().insert(dbSession, newCeQueueDto("ce-queue-uuid-2").setStatus(CeQueueDto.Status.PENDING));
    dbClient.ceQueueDao().insert(dbSession, newCeQueueDto("ce-queue-uuid-3").setStatus(CeQueueDto.Status.IN_PROGRESS));
    for (int i = 0; i < 5; i++) {
      dbClient.ceActivityDao().insert(dbSession, new CeActivityDto(newCeQueueDto("ce-activity-uuid-" + i))
        .setStatus(CeActivityDto.Status.FAILED));
    }
    db.commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("activity_status-example.json"));
  }

  @Test
  public void status_for_a_project_as_project_admin() {
    String projectKey = "project-key";
    String anotherProjectKey = "another-project-key";
    ProjectData project = db.components().insertPrivateProject(c -> c.setKey(projectKey));
    ProjectData anotherProject = db.components().insertPrivateProject(c -> c.setKey(anotherProjectKey));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project.getProjectDto());
    // pending tasks returned
    insertInQueue(CeQueueDto.Status.PENDING, project);
    insertInQueue(CeQueueDto.Status.PENDING, project);
    // other tasks not returned
    insertInQueue(CeQueueDto.Status.IN_PROGRESS, project);
    insertInQueue(CeQueueDto.Status.PENDING, anotherProject);
    insertInQueue(CeQueueDto.Status.PENDING, null);
    // only one last activity for a given project
    insertActivity(CeActivityDto.Status.SUCCESS, project);
    insertActivity(CeActivityDto.Status.CANCELED, project);
    insertActivity(CeActivityDto.Status.FAILED, project);
    insertActivity(CeActivityDto.Status.FAILED, project);
    insertActivity(CeActivityDto.Status.FAILED, anotherProject);

    Ce.ActivityStatusWsResponse result = callByComponentKey(projectKey);

    assertThat(result.getPending()).isEqualTo(2);
    assertThat(result.getFailing()).isOne();
  }

  @Test
  public void add_pending_time() {
    String projectKey = "project-key";
    ProjectData project = db.components().insertPrivateProject(c -> c.setKey(projectKey));

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project.getProjectDto());
    when(system2.now()).thenReturn(2000L);
    insertInQueue(CeQueueDto.Status.PENDING, project, 1000L);
    Ce.ActivityStatusWsResponse result = callByComponentKey(projectKey);

    assertThat(result).extracting(Ce.ActivityStatusWsResponse::getPending, Ce.ActivityStatusWsResponse::getFailing,
        Ce.ActivityStatusWsResponse::getInProgress, Ce.ActivityStatusWsResponse::getPendingTime)
      .containsOnly(1, 0, 0, 1000L);
  }

  @Test
  public void empty_status() {
    Ce.ActivityStatusWsResponse result = call();

    assertThat(result.getPending()).isZero();
    assertThat(result.getFailing()).isZero();
  }

  @Test
  public void fail_if_component_key_is_unknown() {
    assertThatThrownBy(() -> callByComponentKey("unknown-key"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void throw_ForbiddenException_if_not_root() {
    userSession.logIn();

    assertThatThrownBy(this::call)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_not_administrator_of_requested_project() {
    userSession.logIn();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    String dbKey = project.getKey();
    assertThatThrownBy(() -> callByComponentKey(dbKey))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  private void insertInQueue(CeQueueDto.Status status, @Nullable ProjectData projectData) {
    insertInQueue(status, projectData, null);
  }

  private void insertInQueue(CeQueueDto.Status status, @Nullable ProjectData projectData, @Nullable Long createdAt) {
    CeQueueDto ceQueueDto = newCeQueueDto(Uuids.createFast())
      .setStatus(status);
    if(projectData != null) {
      ceQueueDto.setComponentUuid(projectData.getMainBranchComponent().uuid())
        .setEntityUuid(projectData.projectUuid());
    }
    if (createdAt != null) {
      ceQueueDto.setCreatedAt(createdAt);
    }
    dbClient.ceQueueDao().insert(dbSession, ceQueueDto);
    db.commit();
  }

  private void insertActivity(CeActivityDto.Status status, ProjectData dto) {
    CeQueueDto ceQueueDto = newCeQueueDto(Uuids.createFast());
    ceQueueDto.setComponentUuid(dto.getMainBranchComponent().uuid());
    ceQueueDto.setEntityUuid(dto.projectUuid());
    dbClient.ceActivityDao().insert(dbSession, new CeActivityDto(ceQueueDto)
      .setStatus(status));
    db.commit();
  }

  private Ce.ActivityStatusWsResponse call() {
    return callByComponentKey(null);
  }

  private Ce.ActivityStatusWsResponse callByComponentKey(@Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    if (componentKey != null) {
      request.setParam(PARAM_COMPONENT, componentKey);
    }
    return request.executeProtobuf(Ce.ActivityStatusWsResponse.class);
  }
}
