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
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.user.GroupDto;
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
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.permission.ws.AddGroupAction.ACTION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_GROUP_ID;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_GROUP_NAME;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PROJECT_ID;
import static org.sonar.server.permission.ws.PermissionsWs.ENDPOINT;

@Category(DbTests.class)
public class AddGroupActionTest {
  UserSessionRule userSession = UserSessionRule.standalone();
  WsTester ws;
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private PermissionUpdater permissionUpdater;
  private DbClient dbClient;
  private ArgumentCaptor<PermissionChange> permissionChangeCaptor;

  @Before
  public void setUp() {
    permissionUpdater = mock(PermissionUpdater.class);
    permissionChangeCaptor = ArgumentCaptor.forClass(PermissionChange.class);
    dbClient = db.getDbClient();
    ComponentFinder componentFinder = new ComponentFinder(dbClient);
    ws = new WsTester(new PermissionsWs(
      new AddGroupAction(dbClient, new PermissionChangeBuilder(new PermissionDependenciesFinder(dbClient, componentFinder)), permissionUpdater)));
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void call_permission_service_with_right_data() throws Exception {
    insertGroup("sonar-administrators");
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.groupName()).isEqualTo("sonar-administrators");
    assertThat(permissionChange.permission()).isEqualTo(SYSTEM_ADMIN);
  }

  @Test
  public void add_with_group_id() throws Exception {
    GroupDto group = insertGroup("sonar-administrators");
    commit();

    newRequest()
      .setParam(PARAM_GROUP_ID, group.getId().toString())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.groupName()).isEqualTo("sonar-administrators");
  }

  @Test
  public void add_with_project_uuid() throws Exception {
    insertGroup("sonar-administrators");
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
    assertThat(permissionChange.groupName()).isEqualTo("sonar-administrators");
  }

  @Test
  public void add_with_project_key() throws Exception {
    insertGroup("sonar-administrators");
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).addPermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
    assertThat(permissionChange.groupName()).isEqualTo("sonar-administrators");
  }

  @Test
  public void fail_if_project_uuid_not_found() throws Exception {
    expectedException.expect(NotFoundException.class);

    insertGroup("sonar-administrators");
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_ID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_if_project_permission_withou_project() throws Exception {
    expectedException.expect(BadRequestException.class);

    insertGroup("sonar-administrators");
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_if_component_uuid_is_not_a_project() throws Exception {
    expectedException.expect(BadRequestException.class);

    insertGroup("sonar-administrators");
    ComponentDto project = newProjectDto("project-uuid").setKey("project-key");
    insertComponent(project);
    insertComponent(ComponentTesting.newFileDto(project, "file-uuid"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_ID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(ENDPOINT, ACTION)
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_group_name_and_group_id_are_missing() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Group name or group id must be provided, not both");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .execute();
  }

  @Test
  public void fail_when_project_uuid_and_project_key_are_provided() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_PROJECT_ID, "project-uuid")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(ENDPOINT, ACTION);
  }

  private void commit() {
    db.getSession().commit();
  }

  private void insertComponent(ComponentDto component) {
    dbClient.componentDao().insert(db.getSession(), component);
  }

  private GroupDto insertGroup(String name) {
    return dbClient.groupDao().insert(db.getSession(), new GroupDto().setName(name));
  }
}
