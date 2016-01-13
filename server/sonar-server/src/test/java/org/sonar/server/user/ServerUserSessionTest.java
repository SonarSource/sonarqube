/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.user.AuthorizationDao;
import org.sonar.server.exceptions.ForbiddenException;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerUserSessionTest {
  static final String LOGIN = "marius";
  static final String PROJECT_KEY = "com.foo:Bar";
  static final String PROJECT_UUID = "ABCD";
  static final String FILE_KEY = "com.foo:Bar:BarFile.xoo";
  static final String FILE_UUID = "BCDE";

  AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
  ResourceDao resourceDao = mock(ResourceDao.class);

  @Test
  public void login_should_not_be_empty() {
    UserSession session = newServerUserSession().setLogin("");
    assertThat(session.getLogin()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void has_global_permission() {
    UserSession session = newServerUserSession().setLogin(LOGIN);

    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList("profileadmin", "admin"));

    assertThat(session.hasPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(session.hasPermission(GlobalPermissions.SYSTEM_ADMIN)).isTrue();
    assertThat(session.hasPermission(GlobalPermissions.DASHBOARD_SHARING)).isFalse();
  }

  @Test
  public void check_global_Permission_ok() {
    UserSession session = newServerUserSession().setLogin(LOGIN);

    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList("profileadmin", "admin"));

    session.checkPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test(expected = ForbiddenException.class)
  public void check_global_Permission_ko() {
    UserSession session = newServerUserSession().setLogin(LOGIN);

    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList("profileadmin", "admin"));

    session.checkPermission(GlobalPermissions.DASHBOARD_SHARING);
  }

  @Test
  public void has_component_permission() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    String componentKey = FILE_KEY;
    when(resourceDao.getRootProjectByComponentKey(componentKey)).thenReturn(new ResourceDto().setKey(componentKey));
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList(componentKey));

    assertThat(session.hasComponentPermission(UserRole.USER, componentKey)).isTrue();
    assertThat(session.hasComponentPermission(UserRole.CODEVIEWER, componentKey)).isFalse();
    assertThat(session.hasComponentPermission(UserRole.ADMIN, componentKey)).isFalse();
  }

  @Test
  public void has_component_uuid_permission() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    String componentUuid = FILE_UUID;
    when(resourceDao.selectResource(componentUuid)).thenReturn(new ResourceDto().setUuid(componentUuid).setProjectUuid(PROJECT_UUID));
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(PROJECT_UUID));

    assertThat(session.hasComponentUuidPermission(UserRole.USER, componentUuid)).isTrue();
    assertThat(session.hasComponentUuidPermission(UserRole.CODEVIEWER, componentUuid)).isFalse();
    assertThat(session.hasComponentUuidPermission(UserRole.ADMIN, componentUuid)).isFalse();
  }

  @Test
  public void has_component_permission_with_only_global_permission() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    String componentKey = FILE_KEY;
    when(resourceDao.getRootProjectByComponentKey(componentKey)).thenReturn(new ResourceDto().setKey(componentKey));
    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList(UserRole.USER));

    assertThat(session.hasComponentPermission(UserRole.USER, componentKey)).isTrue();
    assertThat(session.hasComponentPermission(UserRole.CODEVIEWER, componentKey)).isFalse();
    assertThat(session.hasComponentPermission(UserRole.ADMIN, componentKey)).isFalse();
  }

  @Test
  public void has_component_uuid_permission_with_only_global_permission() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    String componentUuid = FILE_UUID;
    when(resourceDao.selectResource(componentUuid)).thenReturn(new ResourceDto().setUuid(componentUuid).setProjectUuid(PROJECT_UUID));
    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList(UserRole.USER));

    assertThat(session.hasComponentUuidPermission(UserRole.USER, componentUuid)).isTrue();
    assertThat(session.hasComponentUuidPermission(UserRole.CODEVIEWER, componentUuid)).isFalse();
    assertThat(session.hasComponentUuidPermission(UserRole.ADMIN, componentUuid)).isFalse();
  }

  @Test
  public void check_component_key_permission_ok() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey(FILE_KEY)).thenReturn(new ResourceDto().setKey(PROJECT_KEY));
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList(PROJECT_KEY));

    session.checkComponentPermission(UserRole.USER, FILE_KEY);
  }

  @Test
  public void check_component_key_permission_with_only_global_permission_ok() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey(FILE_KEY)).thenReturn(new ResourceDto().setKey(PROJECT_KEY));
    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList(UserRole.USER));

    session.checkComponentPermission(UserRole.USER, FILE_KEY);
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_key_permission_ko() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey(FILE_KEY)).thenReturn(new ResourceDto().setKey("com.foo:Bar2"));
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList(PROJECT_KEY));

    session.checkComponentPermission(UserRole.USER, FILE_KEY);
  }

  @Test
  public void check_component_uuid_permission_ok() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, "file-uuid");
    when(resourceDao.selectResource("file-uuid")).thenReturn(new ResourceDto().setProjectUuid(project.uuid()));
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkComponentUuidPermission(UserRole.USER, file.uuid());
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_uuid_permission_ko() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    when(resourceDao.selectResource("file-uuid")).thenReturn(new ResourceDto().setProjectUuid(project.uuid()));
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkComponentUuidPermission(UserRole.USER, "another-uuid");
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_key_permission_when_project_not_found() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey(FILE_KEY)).thenReturn(null);

    session.checkComponentPermission(UserRole.USER, FILE_KEY);
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_dto_permission_ko() {
    UserSession session = newServerUserSession().setLogin(LOGIN).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkComponentPermission(UserRole.USER, "another");
  }

  @Test
  public void deprecated_has_global_permission() throws Exception {
    UserSession session = newServerUserSession().setLogin(LOGIN);

    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList("profileadmin", "admin"));

    assertThat(session.hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(session.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)).isTrue();
    assertThat(session.hasGlobalPermission(GlobalPermissions.DASHBOARD_SHARING)).isFalse();
  }

  @Test
  public void deprecated_check_global_permission() throws Exception {
    UserSession session = newServerUserSession().setLogin(LOGIN);

    when(authorizationDao.selectGlobalPermissions(LOGIN)).thenReturn(Arrays.asList("profileadmin", "admin"));

    session.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private ServerUserSession newServerUserSession() {
    return new ServerUserSession(authorizationDao, resourceDao);
  }

}
