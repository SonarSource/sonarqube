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
package org.sonar.server.user;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.exceptions.ForbiddenException;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserSessionTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void getSession_get_anonymous_by_default() throws Exception {
    UserSession.remove();

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
    // default locale
    assertThat(session.locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void get_session() throws Exception {
    UserSession.set(new UserSession().setUserId(123).setLogin("karadoc").setLocale(Locale.FRENCH));

    UserSession session = UserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.userId()).isEqualTo(123);
    assertThat(session.login()).isEqualTo("karadoc");
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void login_should_not_be_empty() throws Exception {
    UserSession session = new UserSession().setLogin("");
    assertThat(session.login()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void has_global_permission() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao);

    when(authorizationDao.selectGlobalPermissions("marius")).thenReturn(Arrays.asList("profileadmin", "admin"));

    assertThat(session.hasGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN)).isTrue();
    assertThat(session.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)).isTrue();
    assertThat(session.hasGlobalPermission(GlobalPermissions.DASHBOARD_SHARING)).isFalse();
  }

  @Test
  public void check_global_Permission_ok() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao);

    when(authorizationDao.selectGlobalPermissions("marius")).thenReturn(Arrays.asList("profileadmin", "admin"));

    session.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test(expected = ForbiddenException.class)
  public void check_global_Permission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao);

    when(authorizationDao.selectGlobalPermissions("marius")).thenReturn(Arrays.asList("profileadmin", "admin"));

    session.checkGlobalPermission(GlobalPermissions.DASHBOARD_SHARING);
  }

  @Test
  public void has_project_permission() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao).setUserId(1);
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList("com.foo:Bar"));

    assertThat(session.hasProjectPermission(UserRole.USER, "com.foo:Bar")).isTrue();
    assertThat(session.hasProjectPermission(UserRole.CODEVIEWER, "com.foo:Bar")).isFalse();
    assertThat(session.hasProjectPermission(UserRole.ADMIN, "com.foo:Bar")).isFalse();
  }

  @Test
  public void has_project_permission_by_uuid() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao).setUserId(1);
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList("ABCD"));

    assertThat(session.hasProjectPermissionByUuid(UserRole.USER, "ABCD")).isTrue();
    assertThat(session.hasProjectPermissionByUuid(UserRole.CODEVIEWER, "ABCD")).isFalse();
    assertThat(session.hasProjectPermissionByUuid(UserRole.ADMIN, "ABCD")).isFalse();
  }

  @Test
  public void check_project_permission_ok() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao).setUserId(1);
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList("com.foo:Bar"));

    session.checkProjectPermission(UserRole.USER, "com.foo:Bar");
  }

  @Test(expected = ForbiddenException.class)
  public void check_project_permission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao).setUserId(1);
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList("com.foo:Bar2"));

    session.checkProjectPermission(UserRole.USER, "com.foo:Bar");
  }

  @Test
  public void check_project_uuid_permission_ok() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkProjectUuidPermission(UserRole.USER, project.uuid());
  }

  @Test(expected = ForbiddenException.class)
  public void check_project_uuid_permission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkProjectUuidPermission(UserRole.USER, "another project");
  }

  @Test
  public void has_component_permission() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao, resourceDao).setUserId(1);

    String componentKey = "com.foo:Bar:BarFile.xoo";
    when(resourceDao.getRootProjectByComponentKey(componentKey)).thenReturn(new ResourceDto().setKey(componentKey));
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList(componentKey));

    assertThat(session.hasComponentPermission(UserRole.USER, componentKey)).isTrue();
    assertThat(session.hasComponentPermission(UserRole.CODEVIEWER, componentKey)).isFalse();
    assertThat(session.hasComponentPermission(UserRole.ADMIN, componentKey)).isFalse();
  }

  @Test
  public void check_component_key_permission_ok() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao, resourceDao).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey("com.foo:Bar:BarFile.xoo")).thenReturn(new ResourceDto().setKey("com.foo:Bar"));
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList("com.foo:Bar"));

    session.checkComponentPermission(UserRole.USER, "com.foo:Bar:BarFile.xoo");
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_key_permission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao, resourceDao).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey("com.foo:Bar:BarFile.xoo")).thenReturn(new ResourceDto().setKey("com.foo:Bar2"));
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList("com.foo:Bar"));

    session.checkComponentPermission(UserRole.USER, "com.foo:Bar:BarFile.xoo");
  }

  @Test
  public void check_component_uuid_permission_ok() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao,resourceDao).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, "file-uuid");
    when(resourceDao.getResource("file-uuid")).thenReturn(new ResourceDto().setProjectUuid(project.uuid()));
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkComponentUuidPermission(UserRole.USER, file.uuid());
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_uuid_permission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao,resourceDao).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, "file-uuid");
    when(resourceDao.getResource("file-uuid")).thenReturn(new ResourceDto().setProjectUuid(project.uuid()));
    when(authorizationDao.selectAuthorizedRootProjectsUuids(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkComponentUuidPermission(UserRole.USER, "another-uuid");
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_key_permission_when_project_not_found() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao, resourceDao).setUserId(1);

    when(resourceDao.getRootProjectByComponentKey("com.foo:Bar:BarFile.xoo")).thenReturn(null);

    session.checkComponentPermission(UserRole.USER, "com.foo:Bar:BarFile.xoo");
  }

  @Test(expected = ForbiddenException.class)
  public void check_component_dto_permission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    ResourceDao resourceDao = mock(ResourceDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao, resourceDao).setUserId(1);

    ComponentDto project = ComponentTesting.newProjectDto();
    when(authorizationDao.selectAuthorizedRootProjectsKeys(1, UserRole.USER)).thenReturn(newArrayList(project.uuid()));

    session.checkComponentPermission(UserRole.USER, "another");
  }

  static class SpyUserSession extends UserSession {
    private AuthorizationDao authorizationDao;
    private ResourceDao resourceDao;

    SpyUserSession(String login, AuthorizationDao authorizationDao) {
      this(login, authorizationDao, null);
    }

    SpyUserSession(String login, AuthorizationDao authorizationDao, @Nullable ResourceDao resourceDao) {
      this.authorizationDao = authorizationDao;
      this.resourceDao = resourceDao;
      setLogin(login);
    }

    @Override
    AuthorizationDao authorizationDao() {
      return authorizationDao;
    }

    @Override
    ResourceDao resourceDao() {
      return resourceDao;
    }

  }
}
