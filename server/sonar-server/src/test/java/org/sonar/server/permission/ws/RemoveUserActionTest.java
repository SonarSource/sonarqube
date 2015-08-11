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
import static org.sonar.server.component.ComponentTesting.newFileDto;
import static org.sonar.server.component.ComponentTesting.newProjectDto;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_USER_LOGIN;
import static org.sonar.server.permission.ws.RemoveUserAction.ACTION;

@Category(DbTests.class)
public class RemoveUserActionTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  UserSessionRule userSession = UserSessionRule.standalone();
  WsTester ws;
  PermissionUpdater permissionUpdater;
  DbClient dbClient;
  DbSession dbSession;
  ArgumentCaptor<PermissionChange> permissionChangeCaptor = ArgumentCaptor.forClass(PermissionChange.class);

  @Before
  public void setUp() {
    permissionUpdater = mock(PermissionUpdater.class);
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    ws = new WsTester(new PermissionsWs(
      new RemoveUserAction(permissionUpdater, new PermissionWsCommons(dbClient, new ComponentFinder(dbClient), userSession))));
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void call_permission_service_with_right_data() throws Exception {
    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.userLogin()).isEqualTo("ray.bradbury");
    assertThat(permissionChange.permission()).isEqualTo(SYSTEM_ADMIN);
  }

  @Test
  public void remove_with_project_uuid() throws Exception {
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_UUID, "project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
  }

  @Test
  public void remove_with_project_key() throws Exception {
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
  }

  @Test
  public void fail_when_project_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_UUID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    expectedException.expect(BadRequestException.class);
    insertComponent(newFileDto(newProjectDto(), "file-uuid"));

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(PARAM_PROJECT_UUID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "george.orwell")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_user_login_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_USER_LOGIN, "jrr.tolkien")
      .execute();
  }

  private void insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(dbSession, component);
    dbSession.commit();
  }
}
