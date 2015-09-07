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

package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.permission.ws.AddUserAction.ACTION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_USER_LOGIN;
import static org.sonar.server.permission.ws.PermissionsWs.ENDPOINT;

@Category(DbTests.class)
public class AddUserActionTest {
  UserSessionRule userSession = UserSessionRule.standalone();
  WsTester ws;
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private PermissionUpdater permissionUpdater;
  private DbClient dbClient;
  private DbSession dbSession;
  private ArgumentCaptor<PermissionChange> permissionChangeCaptor = ArgumentCaptor.forClass(PermissionChange.class);

  @Before
  public void setUp() {
    permissionUpdater = mock(PermissionUpdater.class);
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    ws = new WsTester(new PermissionsWs(
      new AddUserAction(dbClient, permissionUpdater, new PermissionChangeBuilder(new PermissionDependenciesFinder(dbClient, componentFinder)))));
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void call_permission_service_with_right_data() throws Exception {
    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.userLogin()).isEqualTo("ray.bradbury");
    assertThat(permissionChange.permission()).isEqualTo(SYSTEM_ADMIN);
  }

  @Test
  public void add_user_permission_with_project_uuid() throws Exception {
    dbClient.componentDao().insert(dbSession, newProjectDto("project-uuid").setKey("project-key"));
    commit();

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
  }

  @Test
  public void add_user_permissions_with_project_key() throws Exception {
    dbClient.componentDao().insert(dbSession, newProjectDto("project-uuid").setKey("project-key"));
    commit();

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
  }

  @Test
  public void fail_when_project_uuid_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_permission_without_project() throws Exception {
    expectedException.expect(BadRequestException.class);

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    expectedException.expect(BadRequestException.class);
    insertComponent(newFileDto(newProjectDto("project-uuid"), "file-uuid"));
    commit();

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_ID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "george.orwell")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_user_login_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "jrr.tolkien")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    ws.newPostRequest(ENDPOINT, ACTION)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  private void insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
  }

  private void commit() {
    dbSession.commit();
  }
}
