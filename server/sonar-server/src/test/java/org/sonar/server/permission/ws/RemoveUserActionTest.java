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
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.permission.ws.RemoveUserAction.ACTION;

public class RemoveUserActionTest {
  UserSessionRule userSession = UserSessionRule.standalone();
  WsTester ws;
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private PermissionService permissionService;

  @Before
  public void setUp() {
    permissionService = mock(PermissionService.class);
    ws = new WsTester(new PermissionsWs(
      new RemoveUserAction(permissionService)));
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void call_permission_service_with_right_data() throws Exception {
    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(RemoveUserAction.PARAM_USER_LOGIN, "ray.bradbury")
      .setParam(RemoveUserAction.PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    ArgumentCaptor<PermissionChange> permissionChangeCaptor = ArgumentCaptor.forClass(PermissionChange.class);
    verify(permissionService).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.user()).isEqualTo("ray.bradbury");
    assertThat(permissionChange.permission()).isEqualTo(SYSTEM_ADMIN);
  }

  @Test
  public void get_request_are_not_authorized() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(RemoveUserAction.PARAM_USER_LOGIN, "george.orwell")
      .setParam(RemoveUserAction.PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_user_login_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(RemoveUserAction.PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_is_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(RemoveUserAction.PARAM_USER_LOGIN, "jrr.tolkien")
      .execute();
  }
}
