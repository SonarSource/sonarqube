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
package org.sonar.server.ce.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeQueueTesting.newCeQueueDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.ce.ws.CeWsParameters.DEPRECATED_PARAM_COMPONENT_KEY;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonar.test.JsonAssert.assertJson;

public class ActivityStatusActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private WsActionTester ws = new WsActionTester(new ActivityStatusAction(userSession, dbClient, TestComponentFinder.from(db)));

  @Test
  public void json_example() {
    dbClient.ceQueueDao().insert(dbSession, newCeQueueDto("ce-queue-uuid-1").setStatus(CeQueueDto.Status.PENDING));
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
    String projectUuid = "project-uuid";
    String anotherProjectUuid = "another-project-uuid";
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project = newPrivateProjectDto(organizationDto, projectUuid);
    ComponentDto anotherProject = newPrivateProjectDto(organizationDto, anotherProjectUuid);
    db.components().insertComponent(project);
    db.components().insertComponent(newPrivateProjectDto(organizationDto, anotherProjectUuid));
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
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

    Ce.ActivityStatusWsResponse result = call(projectUuid);

    assertThat(result.getPending()).isEqualTo(2);
    assertThat(result.getFailing()).isEqualTo(1);
  }

  @Test
  public void empty_status() {
    Ce.ActivityStatusWsResponse result = call();

    assertThat(result.getPending()).isEqualTo(0);
    assertThat(result.getFailing()).isEqualTo(0);
  }

  @Test
  public void fail_if_component_uuid_and_key_are_provided() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    db.components().insertComponent(project);
    expectedException.expect(IllegalArgumentException.class);

    callByComponentUuidOrComponentKey(project.uuid(), project.getDbKey());
  }

  @Test
  public void fail_if_component_uuid_is_unknown() {
    expectedException.expect(NotFoundException.class);

    call("unknown-uuid");
  }

  @Test
  public void fail_if_component_key_is_unknown() {
    expectedException.expect(NotFoundException.class);

    callByComponentKey("unknown-key");
  }

  @Test
  public void throw_ForbiddenException_if_not_root() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call();
  }

  @Test
  public void throw_ForbiddenException_if_not_administrator_of_requested_project() {
    userSession.logIn();
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    callByComponentKey(project.getDbKey());
  }

  private void insertInQueue(CeQueueDto.Status status, @Nullable ComponentDto componentDto) {
    dbClient.ceQueueDao().insert(dbSession, newCeQueueDto(Uuids.createFast())
      .setStatus(status)
      .setComponent(componentDto));
    db.commit();
  }

  private void insertActivity(CeActivityDto.Status status, @Nullable ComponentDto dto) {
    CeQueueDto ceQueueDto = newCeQueueDto(Uuids.createFast());
    ceQueueDto.setComponent(dto);
    dbClient.ceActivityDao().insert(dbSession, new CeActivityDto(ceQueueDto)
      .setStatus(status));
    db.commit();
  }

  private Ce.ActivityStatusWsResponse call() {
    return callByComponentUuidOrComponentKey(null, null);
  }

  private Ce.ActivityStatusWsResponse call(String componentUuid) {
    return callByComponentUuidOrComponentKey(componentUuid, null);
  }

  private Ce.ActivityStatusWsResponse callByComponentKey(String componentKey) {
    return callByComponentUuidOrComponentKey(null, componentKey);
  }

  private Ce.ActivityStatusWsResponse callByComponentUuidOrComponentKey(@Nullable String componentUuid, @Nullable String componentKey) {
    TestRequest request = ws.newRequest();
    if (componentUuid != null) {
      request.setParam(PARAM_COMPONENT_ID, componentUuid);
    }
    if (componentKey != null) {
      request.setParam(DEPRECATED_PARAM_COMPONENT_KEY, componentKey);
    }
    return request.executeProtobuf(Ce.ActivityStatusWsResponse.class);
  }
}
