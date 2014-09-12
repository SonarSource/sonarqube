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

package org.sonar.server.permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.AuthorizedComponentDto;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.permission.PermissionQuery;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.db.IssueAuthorizationDao;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.search.IndexClient;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InternalPermissionServiceTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  UserDao userDao;

  @Mock
  ResourceDao resourceDao;

  @Mock
  ComponentDao componentDao;

  @Mock
  PermissionFacade permissionFacade;

  @Mock
  PermissionFinder finder;

  @Mock
  IssueAuthorizationDao issueAuthorizationDao;

  @Mock
  IndexClient index;

  Map<String, Object> params;
  InternalPermissionService service;

  @Before
  public void setUpCommonStubbing() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.issueAuthorizationDao()).thenReturn(issueAuthorizationDao);

    when(index.get(IssueAuthorizationIndex.class)).thenReturn(mock(IssueAuthorizationIndex.class));

    when(userDao.selectActiveUserByLogin("user", session)).thenReturn(new UserDto().setId(2L).setLogin("user").setActive(true));
    when(userDao.selectGroupByName("group", session)).thenReturn(new GroupDto().setId(2L).setName("group"));

    service = new InternalPermissionService(dbClient, userDao, resourceDao, permissionFacade, finder, index);
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

    ArgumentCaptor<PermissionQuery> argumentCaptor = ArgumentCaptor.forClass(PermissionQuery.class);
    verify(finder).findUsersWithPermission(argumentCaptor.capture());

    PermissionQuery query = argumentCaptor.getValue();
    assertThat(query.component()).isEqualTo("org.sample.Sample");
    assertThat(query.permission()).isEqualTo("user");
    assertThat(query.membership()).isEqualTo(PermissionQuery.ANY);
  }

  @Test
  public void find_groups_with_permissions() throws Exception {
    service.findGroupsWithPermission(ImmutableMap.<String, Object>of(
      "permission", "admin",
      "component", "org.sample.Sample",
      "selected", "all"));

    ArgumentCaptor<PermissionQuery> argumentCaptor = ArgumentCaptor.forClass(PermissionQuery.class);
    verify(finder).findGroupsWithPermission(argumentCaptor.capture());

    PermissionQuery query = argumentCaptor.getValue();
    assertThat(query.component()).isEqualTo("org.sample.Sample");
    assertThat(query.permission()).isEqualTo("admin");
    assertThat(query.membership()).isEqualTo(PermissionQuery.ANY);
  }

  @Test
  public void add_global_user_permission() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermissions.DASHBOARD_SHARING);
    setUpGlobalUserPermissions("user", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertUserPermission(eq((Long) null), eq(2L), eq("shareDashboard"), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void add_component_user_permission() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "user");
    setUpComponentUserPermissions("user", 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.addPermission(params);

    verify(permissionFacade).insertUserPermission(eq(10L), eq(2L), eq("user"), eq(session));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test
  public void remove_global_user_permission() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalUserPermissions("user", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(permissionFacade).deleteUserPermission(eq((Long) null), eq(2L), eq("profileadmin"), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void remove_component_user_permission() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));
    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "codeviewer");
    setUpComponentUserPermissions("user", 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.removePermission(params);

    verify(permissionFacade).deleteUserPermission(eq(10L), eq(2L), eq("codeviewer"), eq(session));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test
  public void add_global_group_permission() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermissions.DASHBOARD_SHARING);
    setUpGlobalGroupPermissions("group", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq((Long) null), eq(2L), eq("shareDashboard"), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void add_component_group_permission() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "user");
    setUpGlobalGroupPermissions("group", "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq(10L), eq(2L), eq("user"), eq(session));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test
  public void add_global_permission_to_anyone_group() throws Exception {
    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq((Long) null), eq((Long) null), eq("profileadmin"), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void add_component_permission_to_anyone_group() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "user");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.addPermission(params);

    verify(permissionFacade).insertGroupPermission(eq(10L), eq((Long) null), eq("user"), eq(session));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test
  public void remove_global_group_permission() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalGroupPermissions("group", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq((Long) null), eq(2L), eq("profileadmin"), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void remove_component_group_permission() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "codeviewer");
    setUpComponentGroupPermissions("group", 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq(10L), eq(2L), eq("codeviewer"), eq(session));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test
  public void remove_global_permission_from_anyone_group() throws Exception {
    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalGroupPermissions(DefaultGroups.ANYONE, GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq((Long) null), eq((Long) null), eq("profileadmin"), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void remove_component_permission_from_anyone_group() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, DefaultGroups.ANYONE, "org.sample.Sample", "codeviewer");
    setUpComponentGroupPermissions(DefaultGroups.ANYONE, 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.removePermission(params);

    verify(permissionFacade).deleteGroupPermission(eq(10L), eq((Long) null), eq("codeviewer"), eq(session));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test
  public void skip_redundant_add_global_user_permission_change() throws Exception {
    params = buildPermissionChangeParams("user", null, GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalUserPermissions("user", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade, never()).insertUserPermission(anyLong(), anyLong(), anyString(), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void skip_redundant_add_component_user_permission_change() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams("user", null, "org.sample.Sample", "codeviewer");
    setUpComponentUserPermissions("user", 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.addPermission(params);

    verify(permissionFacade, never()).insertUserPermission(anyLong(), anyLong(), anyString(), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void skip_redundant_add_global_group_permission_change() throws Exception {
    params = buildPermissionChangeParams(null, "group", GlobalPermissions.QUALITY_PROFILE_ADMIN);
    setUpGlobalGroupPermissions("group", GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.addPermission(params);

    verify(permissionFacade, never()).insertGroupPermission(anyLong(), anyLong(), anyString(), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void skip_redundant_add_component_group_permission_change() throws Exception {
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));

    params = buildPermissionChangeParams(null, "group", "org.sample.Sample", "codeviewer");
    setUpComponentGroupPermissions("group", 10L, "codeviewer");
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    service.addPermission(params);

    verify(permissionFacade, never()).insertGroupPermission(anyLong(), anyLong(), anyString(), eq(session));
    verifyZeroInteractions(issueAuthorizationDao);
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
      when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(new AuthorizedComponentDto().setId(10L).setKey("org.sample.Sample"));
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
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample1", session)).thenReturn(c1);
    ComponentDto c2 = mock(ComponentDto.class);
    when(c2.getId()).thenReturn(2L);
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample2", session)).thenReturn(c2);
    ComponentDto c3 = mock(ComponentDto.class);
    when(c3.getId()).thenReturn(3L);
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample3", session)).thenReturn(c3);
    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "org.sample.Sample1,org.sample.Sample2,org.sample.Sample3");

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate(session, "my_template_key", 1L);
    verify(permissionFacade).applyPermissionTemplate(session, "my_template_key", 2L);
    verify(permissionFacade).applyPermissionTemplate(session, "my_template_key", 3L);

    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample1")));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample2")));
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample3")));
  }

  @Test(expected = ForbiddenException.class)
  public void apply_permission_template_on_many_projects_without_permission() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions();

    ComponentDto c1 = mock(ComponentDto.class);
    when(c1.getId()).thenReturn(1L);
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample1", session)).thenReturn(c1);
    ComponentDto c2 = mock(ComponentDto.class);
    when(c2.getId()).thenReturn(2L);
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample2", session)).thenReturn(c2);
    ComponentDto c3 = mock(ComponentDto.class);
    when(c3.getId()).thenReturn(3L);
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample3", session)).thenReturn(c3);
    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "org.sample.Sample1,org.sample.Sample2,org.sample.Sample3");

    service.applyPermissionTemplate(params);

    verify(permissionFacade, never()).applyPermissionTemplate(eq(session), anyString(), anyLong());
    verifyZeroInteractions(issueAuthorizationDao);
  }

  @Test
  public void apply_permission_template_on_one_project() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "org.sample.Sample");

    ComponentDto c = mock(ComponentDto.class);
    when(c.getId()).thenReturn(1L);
    when(componentDao.getAuthorizedComponentByKey("org.sample.Sample", session)).thenReturn(c);

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate(session, "my_template_key", 1L);
    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "org.sample.Sample")));
  }

  @Test(expected = ForbiddenException.class)
  public void apply_permission_template_on_one_project_without_permission() {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN);

    params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", "1");

    service.applyPermissionTemplate(params);

    verify(permissionFacade).applyPermissionTemplate(session, "my_template_key", 1L);
    verifyZeroInteractions(issueAuthorizationDao);
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

    when(componentDao.getAuthorizedComponentByKey(componentKey, session)).thenReturn(mockComponent);
    service.applyDefaultPermissionTemplate(componentKey);
    verify(permissionFacade).grantDefaultRoles(session, componentId, qualifier);

    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "component")));
  }

  @Test(expected = ForbiddenException.class)
  public void apply_default_permission_template_on_provisioned_project_without_permission() {
    final String componentKey = "component";
    final long componentId = 1234l;
    final String qualifier = Qualifiers.PROJECT;

    ComponentDto mockComponent = mock(ComponentDto.class);
    when(mockComponent.getId()).thenReturn(componentId);
    when(mockComponent.qualifier()).thenReturn(qualifier);

    when(componentDao.getAuthorizedComponentByKey(componentKey, session)).thenReturn(mockComponent);
    when(resourceDao.selectProvisionedProject(session, componentKey)).thenReturn(mock(ResourceDto.class));
    service.applyDefaultPermissionTemplate(componentKey);

    verifyZeroInteractions(issueAuthorizationDao);
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

    when(componentDao.getAuthorizedComponentByKey(componentKey, session)).thenReturn(mockComponent);
    when(resourceDao.selectProvisionedProject(session, componentKey)).thenReturn(mock(ResourceDto.class));
    service.applyDefaultPermissionTemplate(componentKey);

    verify(issueAuthorizationDao).synchronizeAfter(eq(session), any(Date.class), eq(ImmutableMap.of("project", "component")));
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
    when(permissionFacade.selectUserPermissions(session, login, null)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpGlobalGroupPermissions(String groupName, String... permissions) {
    when(permissionFacade.selectGroupPermissions(session, groupName, null)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpComponentUserPermissions(String login, Long componentId, String... permissions) {
    when(permissionFacade.selectUserPermissions(session, login, componentId)).thenReturn(Lists.newArrayList(permissions));
  }

  private void setUpComponentGroupPermissions(String groupName, Long componentId, String... permissions) {
    when(permissionFacade.selectGroupPermissions(session, groupName, componentId)).thenReturn(Lists.newArrayList(permissions));
  }
}
