/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.permission;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.permission.GlobalPermission;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.MockUserSession;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class InternalPermissionServiceTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  private Map<String, Object> params;
  private InternalPermissionService service;
  private RoleDao roleDao;
  private UserDao userDao;
  private ResourceDao resourceDao;
  private PermissionFacade permissionFacade;

  @Before
  public void setUpCommonStubbing() {
    MockUserSession.set().setLogin("admin").setPermissions(GlobalPermission.SYSTEM_ADMIN);

    roleDao = mock(RoleDao.class);
    userDao = mock(UserDao.class);
    when(userDao.selectActiveUserByLogin("user")).thenReturn(new UserDto().setId(2L).setLogin("user").setActive(true));
    when(userDao.selectGroupByName("group")).thenReturn(new GroupDto().setId(2L).setName("group"));

    resourceDao = mock(ResourceDao.class);

    permissionFacade = mock(PermissionFacade.class);

    service = new InternalPermissionService(roleDao, userDao, resourceDao, permissionFacade);
  }

  @Test
  public void should_add_global_user_permission() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermission.DASHBOARD_SHARING);
    setUpUserPermissions("user", GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.addPermission(params);

    verify(permissionFacade).insertUserPermission(eq((Long) null), eq(2L), eq("shareDashboard"));
  }

  @Test
  public void should_add_component_user_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "user");
    setUpUserPermissions("user", "codeviewer");

    service.addPermission(params);

    verify(permissionFacade).insertUserPermission(eq(10L), eq(2L), eq("user"));
  }

  @Test
  public void should_remove_global_user_permission() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermission.QUALITY_PROFILE_ADMIN);
    setUpUserPermissions("user", GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.removePermission(params);

    verify(permissionFacade).deleteUserPermission(eq((Long) null), eq(2L), eq("profileadmin"));
  }

  @Test
  public void should_remove_component_user_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "codeviewer");
    setUpUserPermissions("user", "codeviewer");

    service.removePermission(params);

    verify(permissionFacade).deleteUserPermission(eq(10L), eq(2L), eq("codeviewer"));
  }

  @Test
  public void should_add_global_group_permission() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermission.DASHBOARD_SHARING);
    setUpGroupPermissions("group", GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq((Long) null), eq(2L), eq("shareDashboard"));
  }

  @Test
  public void should_add_component_group_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "user");
    setUpGroupPermissions("group", "codeviewer");

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq(10L), eq(2L), eq("user"));
  }

  @Test
  public void should_add_global_permission_to_anyone_group() throws Exception {
    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, GlobalPermission.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq((Long) null), eq((Long) null), eq("profileadmin"));
  }

  @Test
  public void should_add_component_permission_to_anyone_group() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "user");

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq(10L), eq((Long) null), eq("user"));
  }

  @Test
  public void should_remove_global_group_permission() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermission.QUALITY_PROFILE_ADMIN);
    setUpGroupPermissions("group", GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq((Long) null), eq(2L), eq("profileadmin"));
  }

  @Test
  public void should_remove_component_group_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "codeviewer");
    setUpGroupPermissions("group", "codeviewer");

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq(10L), eq(2L), eq("codeviewer"));
  }

  @Test
  public void should_remove_global_permission_from_anyone_group() throws Exception {
    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, GlobalPermission.QUALITY_PROFILE_ADMIN);
    setUpGroupPermissions(DefaultGroups.ANYONE, GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq((Long) null), eq((Long) null), eq("profileadmin"));
  }

  @Test
  public void should_remove_component_permission_from_anyone_group() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "codeviewer");
    setUpGroupPermissions(DefaultGroups.ANYONE, "codeviewer");

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq(10L), eq((Long) null), eq("codeviewer"));
  }

  @Test
  public void should_skip_redundant_add_user_permission_change() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermission.QUALITY_PROFILE_ADMIN);
    setUpUserPermissions("user", GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.addPermission(params);

    verify(permissionFacade, never()).insertUserPermission(anyLong(), anyLong(), anyString());
  }

  @Test
  public void should_skip_redundant_add_group_permission_change() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermission.QUALITY_PROFILE_ADMIN);
    setUpGroupPermissions("group", GlobalPermission.QUALITY_PROFILE_ADMIN.key());

    service.addPermission(params);

    verify(permissionFacade, never()).insertGroupPermission(anyLong(), anyLong(), anyString());
  }

  @Test
  public void should_fail_when_user_and_group_are_provided() throws Exception {
    try {
      params = buildPermissionChangeParams("user", "group", GlobalPermission.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Only one of user or group parameter should be provided");
    }
  }

  @Test
  public void should_fail_when_user_is_not_found() throws Exception {
    try {
      when(userDao.selectActiveUserByLogin("user")).thenReturn(null);
      params = buildPermissionChangeParams("unknown", null, GlobalPermission.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("User unknown does not exist");
    }
  }

  @Test
  public void should_fail_when_group_is_not_found() throws Exception {
    try {
      params = buildPermissionChangeParams(null, "unknown", GlobalPermission.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Group unknown does not exist");
    }
  }

  @Test
  public void should_fail_when_component_is_not_found() throws Exception {
    try {
      params = buildPermissionChangeParams(null, "group", "unknown", "user");
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Component unknown does not exists.");
    }
  }

  @Test
  public void should_fail_on_insufficient_rights() throws Exception {
    try {
      params = buildPermissionChangeParams("user", null, GlobalPermission.QUALITY_PROFILE_ADMIN);
      MockUserSession.set().setLogin("unauthorized").setPermissions(GlobalPermission.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage("Insufficient privileges");
    }
  }

  @Test
  public void should_fail_on_anonymous_access() throws Exception {
    throwable.expect(UnauthorizedException.class);
    params = buildPermissionChangeParams("user", null, GlobalPermission.QUALITY_PROFILE_ADMIN);

    MockUserSession.set();

    service.addPermission(params);
  }

  @Test
  public void should_apply_permission_template() throws Exception {
    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "1,2,3");

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate("my_template_key", 1L);
    verify(permissionFacade).applyPermissionTemplate("my_template_key", 2L);
    verify(permissionFacade).applyPermissionTemplate("my_template_key", 3L);
  }

  private Map<String, Object> buildPermissionChangeParams(String login, String group, GlobalPermission perm) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("permission", perm.key());
    return params;
  }

  private Map<String, Object> buildPermissionChangeParams(String login, String group, String component, String perm) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("component", component);
    params.put("permission", perm);
    return params;
  }

  private void setUpUserPermissions(String login, String... permissions) {
    when(roleDao.selectUserPermissions(login)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpGroupPermissions(String groupName, String... permissions) {
    when(roleDao.selectGroupPermissions(groupName)).thenReturn(Lists.newArrayList(permissions));
  }
}
