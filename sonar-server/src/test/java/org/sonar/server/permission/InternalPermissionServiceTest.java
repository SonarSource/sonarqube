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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.permission.WithPermissionQuery;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.MockUserSession;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class InternalPermissionServiceTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  private Map<String, Object> params;
  private InternalPermissionService service;
  private UserDao userDao;
  private ResourceDao resourceDao;
  private PermissionFacade permissionFacade;
  private PermissionFinder finder;

  @Before
  public void setUpCommonStubbing() {
    userDao = mock(UserDao.class);
    when(userDao.selectActiveUserByLogin("user")).thenReturn(new UserDto().setId(2L).setLogin("user").setActive(true));
    when(userDao.selectGroupByName("group")).thenReturn(new GroupDto().setId(2L).setName("group"));

    resourceDao = mock(ResourceDao.class);
    permissionFacade = mock(PermissionFacade.class);
    finder = mock(PermissionFinder.class);

    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    service = new InternalPermissionService(userDao, resourceDao, permissionFacade, finder);
  }

  @Test
  public void return_global_permissions() {
    assertThat(service.globalPermissions()).containsOnly(
      GlobalPermissions.SYSTEM_ADMIN, GlobalPermissions.QUALITY_PROFILE_ADMIN, GlobalPermissions.DASHBOARD_SHARING,
      GlobalPermissions.DRY_RUN_EXECUTION, GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.PROVISIONING);
  }

  @Test
  public void find_users_with_permissions() throws Exception {
    service.findUsersWithPermission(ImmutableMap.<String, Object>of(
      "permission", "user",
      "component", "org.sample.Sample",
      "selected", "all"));

    ArgumentCaptor<WithPermissionQuery> argumentCaptor = ArgumentCaptor.forClass(WithPermissionQuery.class);
    verify(finder).findUsersWithPermission(argumentCaptor.capture());

    WithPermissionQuery query = argumentCaptor.getValue();
    assertThat(query.component()).isEqualTo("org.sample.Sample");
    assertThat(query.permission()).isEqualTo("user");
    assertThat(query.membership()).isEqualTo(WithPermissionQuery.ANY);
  }

  @Test
  public void find_groups_with_permissions() throws Exception {
    service.findGroupsWithPermission(ImmutableMap.<String, Object>of(
      "permission", "admin",
      "component", "org.sample.Sample",
      "selected", "all"));

    ArgumentCaptor<WithPermissionQuery> argumentCaptor = ArgumentCaptor.forClass(WithPermissionQuery.class);
    verify(finder).findGroupsWithPermission(argumentCaptor.capture());

    WithPermissionQuery query = argumentCaptor.getValue();
    assertThat(query.component()).isEqualTo("org.sample.Sample");
    assertThat(query.permission()).isEqualTo("admin");
    assertThat(query.membership()).isEqualTo(WithPermissionQuery.ANY);
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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    setUpComponentUserPermissions("user", 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

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
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Component unknown does not exist");
    }
  }

  @Test
  public void fail_on_insufficient_global_rights() throws Exception {
    try {
      params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
      MockUserSession.set().setLogin("unauthorized").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
      service.addPermission(params);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class).hasMessage("Insufficient privileges");
    }
  }

  @Test
  public void fail_on_insufficient_project_rights() throws Exception {
    try {
      when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(
        new ResourceDto().setId(10L).setKey("org.sample.Sample"));
      params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "user");
      MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN);

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
  public void apply_permission_template_on_many_projects() throws Exception {
    ComponentDto c1 = mock(ComponentDto.class);
    when(c1.getId()).thenReturn(1L);
    when(resourceDao.findByKey("org.sample.Sample1")).thenReturn(c1);
    ComponentDto c2 = mock(ComponentDto.class);
    when(c2.getId()).thenReturn(2L);
    when(resourceDao.findByKey("org.sample.Sample2")).thenReturn(c2);
    ComponentDto c3 = mock(ComponentDto.class);
    when(c3.getId()).thenReturn(3L);
    when(resourceDao.findByKey("org.sample.Sample3")).thenReturn(c3);
    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "org.sample.Sample1,org.sample.Sample2,org.sample.Sample3");

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate("my_template_key", 1L);
    verify(permissionFacade).applyPermissionTemplate("my_template_key", 2L);
    verify(permissionFacade).applyPermissionTemplate("my_template_key", 3L);
  }

  @Test(expected = ForbiddenException.class)
  public void apply_permission_template_on_many_projects_without_permission() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions();

    ComponentDto c1 = mock(ComponentDto.class);
    when(c1.getId()).thenReturn(1L);
    when(resourceDao.findByKey("org.sample.Sample1")).thenReturn(c1);
    ComponentDto c2 = mock(ComponentDto.class);
    when(c2.getId()).thenReturn(2L);
    when(resourceDao.findByKey("org.sample.Sample2")).thenReturn(c2);
    ComponentDto c3 = mock(ComponentDto.class);
    when(c3.getId()).thenReturn(3L);
    when(resourceDao.findByKey("org.sample.Sample3")).thenReturn(c3);
    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "org.sample.Sample1,org.sample.Sample2,org.sample.Sample3");

    service.applyPermissionTemplate(params);

    verify(permissionFacade, never()).applyPermissionTemplate(anyString(), anyLong());
  }

  @Test
  public void apply_permission_template_on_one_project() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "org.sample.Sample");

    ComponentDto c = mock(ComponentDto.class);
    when(c.getId()).thenReturn(1L);
    when(resourceDao.findByKey("org.sample.Sample")).thenReturn(c);

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate("my_template_key", 1L);
  }

  @Test(expected = ForbiddenException.class)
  public void apply_permission_template_on_one_project_without_permission() {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN);

    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "1");

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate("my_template_key", 1L);
  }

  @Test
  public void apply_default_permission_template_on_standard_project() {
    final String componentKey = "component";
    final long componentId = 1234l;
    final String qualifier = Qualifiers.PROJECT;

    ComponentDto mockComponent = mock(ComponentDto.class);
    when(mockComponent.getId()).thenReturn(componentId);
    when(mockComponent.key()).thenReturn(componentKey);
    when(mockComponent.qualifier()).thenReturn(qualifier);
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, componentKey);

    when(resourceDao.findByKey(componentKey)).thenReturn(mockComponent);
    service.applyDefaultPermissionTemplate(componentKey);
    verify(resourceDao).findByKey(componentKey);
    verify(permissionFacade).grantDefaultRoles(componentId, qualifier);
  }

  @Test(expected = ForbiddenException.class)
  public void apply_default_permission_template_on_provisioned_project_without_permission() {
    final String componentKey = "component";
    final long componentId = 1234l;
    final String qualifier = Qualifiers.PROJECT;

    ComponentDto mockComponent = mock(ComponentDto.class);
    when(mockComponent.getId()).thenReturn(componentId);
    when(mockComponent.qualifier()).thenReturn(qualifier);

    when(resourceDao.findByKey(componentKey)).thenReturn(mockComponent);
    when(resourceDao.selectProvisionedProject(componentKey)).thenReturn(mock(ResourceDto.class));
    service.applyDefaultPermissionTemplate(componentKey);
  }

  @Test
  public void apply_default_permission_template_on_provisioned_project_with_permission() {
    MockUserSession.set().setLogin("provisioning").setGlobalPermissions(GlobalPermissions.PROVISIONING);
    final String componentKey = "component";
    final long componentId = 1234l;
    final String qualifier = Qualifiers.PROJECT;

    ComponentDto mockComponent = mock(ComponentDto.class);
    when(mockComponent.getId()).thenReturn(componentId);
    when(mockComponent.qualifier()).thenReturn(qualifier);

    when(resourceDao.findByKey(componentKey)).thenReturn(mockComponent);
    when(resourceDao.selectProvisionedProject(componentKey)).thenReturn(mock(ResourceDto.class));
    service.applyDefaultPermissionTemplate(componentKey);
  }

  @Test(expected = BadRequestException.class)
  public void apply_default_permission_template_on_non_existing_project() {
    final String componentKey = "component";

    when(resourceDao.findByKey(componentKey)).thenReturn(null);
    service.applyDefaultPermissionTemplate(componentKey);
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
    when(permissionFacade.selectUserPermissions(login, null)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpGlobalGroupPermissions(String groupName, String... permissions) {
    when(permissionFacade.selectGroupPermissions(groupName, null)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpComponentUserPermissions(String login, Long componentId, String... permissions) {
    when(permissionFacade.selectUserPermissions(login, componentId)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpComponentGroupPermissions(String groupName, Long componentId, String... permissions) {
    when(permissionFacade.selectGroupPermissions(groupName, componentId)).thenReturn(Lists.newArrayList(permissions));
  }
}
