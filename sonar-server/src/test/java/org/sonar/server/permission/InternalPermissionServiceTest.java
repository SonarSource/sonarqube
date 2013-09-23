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
import org.sonar.core.permission.GlobalPermissions;
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
    MockUserSession.set().setLogin("admin").setPermissions(GlobalPermissions.SYSTEM_ADMIN);

    roleDao = mock(RoleDao.class);
    userDao = mock(UserDao.class);
    when(userDao.selectActiveUserByLogin("user")).thenReturn(new UserDto().setId(2L).setLogin("user").setActive(true));
    when(userDao.selectGroupByName("group")).thenReturn(new GroupDto().setId(2L).setName("group"));

    resourceDao = mock(ResourceDao.class);

    permissionFacade = mock(PermissionFacade.class);

    service = new InternalPermissionService(roleDao, userDao, resourceDao, permissionFacade);
  }

  @Test
  public void return_global_permissions()  {
    assertThat(service.globalPermissions()).containsOnly(
      GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN, GlobalPermissions.DASHBOARD_SHARING,
      GlobalPermissions.DRY_RUN_EXECUTION, GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.PROVISIONING);
  }

  @Test
  public void add_global_user_permission() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermissions.DASHBOARD_SHARING);
    setUpGlobalUserPermissions("user", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertUserPermission(eq((Long) null), eq(2L), eq("shareDashboard"));
  }

  @Test
  public void add_component_user_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "user");
    setUpComponentUserPermissions("user", 10L, "codeviewer");

    service.addPermission(params);

    verify(permissionFacade).insertUserPermission(eq(10L), eq(2L), eq("user"));
  }

  @Test
  public void remove_global_user_permission() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalUserPermissions("user", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(permissionFacade).deleteUserPermission(eq((Long) null), eq(2L), eq("profileadmin"));
  }

  @Test
  public void remove_component_user_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "codeviewer");
    setUpComponentUserPermissions("user", 10L, "codeviewer");

    service.removePermission(params);

    verify(permissionFacade).deleteUserPermission(eq(10L), eq(2L), eq("codeviewer"));
  }

  @Test
  public void add_global_group_permission() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermissions.DASHBOARD_SHARING);
    setUpGlobalGroupPermissions("group", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq((Long) null), eq(2L), eq("shareDashboard"));
  }

  @Test
  public void add_component_group_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "user");
    setUpGlobalGroupPermissions("group", "codeviewer");

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq(10L), eq(2L), eq("user"));
  }

  @Test
  public void add_global_permission_to_anyone_group() throws Exception {
    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq((Long) null), eq((Long) null), eq("profileadmin"));
  }

  @Test
  public void add_component_permission_to_anyone_group() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "user");

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq(10L), eq((Long) null), eq("user"));
  }

  @Test
  public void remove_global_group_permission() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalGroupPermissions("group", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq((Long) null), eq(2L), eq("profileadmin"));
  }

  @Test
  public void remove_component_group_permission() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "codeviewer");
    setUpComponentGroupPermissions("group", 10L, "codeviewer");

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq(10L), eq(2L), eq("codeviewer"));
  }

  @Test
  public void remove_global_permission_from_anyone_group() throws Exception {
    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalGroupPermissions(DefaultGroups.ANYONE, GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq((Long) null), eq((Long) null), eq("profileadmin"));
  }

  @Test
  public void remove_component_permission_from_anyone_group() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "codeviewer");
    setUpComponentGroupPermissions(DefaultGroups.ANYONE, 10L, "codeviewer");

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq(10L), eq((Long) null), eq("codeviewer"));
  }

  @Test
  public void skip_redundant_add_global_user_permission_change() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalUserPermissions("user", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade, never()).insertUserPermission(anyLong(), anyLong(), anyString());
  }

  @Test
  public void skip_redundant_add_component_user_permission_change() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "codeviewer");
    setUpComponentUserPermissions("user",  10L, "codeviewer");

    service.addPermission(params);

    verify(permissionFacade, never()).insertUserPermission(anyLong(), anyLong(), anyString());
  }

  @Test
  public void skip_redundant_add_global_group_permission_change() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalGroupPermissions("group", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade, never()).insertGroupPermission(anyLong(), anyLong(), anyString());
  }

  @Test
  public void skip_redundant_add_component_group_permission_change() throws Exception {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
      new ResourceDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "codeviewer");
    setUpComponentGroupPermissions("group", 10L, "codeviewer");

    service.addPermission(params);

    verify(permissionFacade, never()).insertGroupPermission(anyLong(), anyLong(), anyString());
  }

  @Test
  public void fail_when_user_and_group_are_provided() throws Exception {
    try {
      params = buildPermissionChangeParams("user", "group", GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Only one of user or group parameter should be provided");
    }
  }

  @Test
  public void fail_when_user_is_not_found() throws Exception {
    try {
      when(userDao.selectActiveUserByLogin("user")).thenReturn(null);
      params = buildPermissionChangeParams("unknown", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("User unknown does not exist");
    }
  }

  @Test
  public void fail_when_group_is_not_found() throws Exception {
    try {
      params = buildPermissionChangeParams(null, "unknown", GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Group unknown does not exist");
    }
  }

  @Test
  public void fail_when_component_is_not_found() throws Exception {
    try {
      params = buildPermissionChangeParams(null, "group", "unknown", "user");
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Component unknown does not exists.");
    }
  }

  @Test
  public void fail_on_insufficient_rights() throws Exception {
    try {
      params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
      MockUserSession.set().setLogin("unauthorized").setPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage("Insufficient privileges");
    }
  }

  @Test
  public void fail_on_anonymous_access() throws Exception {
    throwable.expect(UnauthorizedException.class);
    params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);

    MockUserSession.set();

    service.addPermission(params);
  }

  @Test
  public void apply_permission_template() throws Exception {
    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "1,2,3");

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate("my_template_key", 1L);
    verify(permissionFacade).applyPermissionTemplate("my_template_key", 2L);
    verify(permissionFacade).applyPermissionTemplate("my_template_key", 3L);
  }

  private Map<String, Object> buildPermissionChangeParams(String login, String group, String permission) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("permission", permission);
    return params;
  }

  private Map<String, Object> buildPermissionChangeParams(String login, String group, String component, String permission) {
    Map<String, Object> params = Maps.newHashMap();
    params.put("user", login);
    params.put("group", group);
    params.put("component", component);
    params.put("permission", permission);
    return params;
  }

  private void setUpGlobalUserPermissions(String login, String... permissions) {
    when(roleDao.selectUserPermissions(login, null)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpGlobalGroupPermissions(String groupName, String... permissions) {
    when(roleDao.selectGroupPermissions(groupName, null)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpComponentUserPermissions(String login, Long componentId, String... permissions) {
    when(roleDao.selectUserPermissions(login, componentId)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpComponentGroupPermissions(String groupName, Long componentId, String... permissions) {
    when(roleDao.selectGroupPermissions(groupName, componentId)).thenReturn(Lists.newArrayList(permissions));
  }
}
