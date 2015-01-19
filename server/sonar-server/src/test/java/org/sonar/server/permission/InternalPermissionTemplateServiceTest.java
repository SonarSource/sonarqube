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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.*;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InternalPermissionTemplateServiceTest {

  private static final String DEFAULT_KEY = "my_template";
  private static final String DEFAULT_DESC = "my description";
  private static final String DEFAULT_PATTERN = "com.foo.(.*)";
  private static final String DEFAULT_PERMISSION = UserRole.USER;
  private static final PermissionTemplateDto DEFAULT_TEMPLATE =
    new PermissionTemplateDto().setId(1L).setName(DEFAULT_KEY).setDescription(DEFAULT_DESC).setKeyPattern(DEFAULT_PATTERN);

  @Mock
  PermissionTemplateDao permissionTemplateDao;

  @Mock
  UserDao userDao;

  @Mock
  PermissionFinder finder;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  DbSession session;

  InternalPermissionTemplateService service;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    MyBatis myBatis = mock(MyBatis.class);
    when(myBatis.openSession(false)).thenReturn(session);
    service = new InternalPermissionTemplateService(myBatis, permissionTemplateDao, userDao, finder);
  }

  @Test
  public void find_users_with_permission_template() throws Exception {
    service.findUsersWithPermissionTemplate(ImmutableMap.<String, Object>of(
      "permission", "user",
      "template", "my_template",
      "selected", "all"));
    verify(finder).findUsersWithPermissionTemplate(any(PermissionQuery.class));
  }

  @Test
  public void find_groups_with_permission_template() throws Exception {
    service.findGroupsWithPermissionTemplate(ImmutableMap.<String, Object>of(
      "permission", "user",
      "template", "my_template",
      "selected", "all"));

    verify(finder).findGroupsWithPermissionTemplate(any(PermissionQuery.class));
  }

  @Test
  public void should_create_permission_template() throws Exception {
    when(permissionTemplateDao.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, DEFAULT_PATTERN)).thenReturn(DEFAULT_TEMPLATE);

    PermissionTemplate permissionTemplate = service.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, DEFAULT_PATTERN);

    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo(DEFAULT_KEY);
    assertThat(permissionTemplate.getDescription()).isEqualTo(DEFAULT_DESC);
    assertThat(permissionTemplate.getKeyPattern()).isEqualTo(DEFAULT_PATTERN);
  }

  @Test
  public void should_enforce_unique_template_name() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with that name already exists");

    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(DEFAULT_TEMPLATE));

    service.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, null);
  }

  @Test
  public void should_reject_empty_name_on_creation() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Name can't be blank");

    service.createPermissionTemplate("", DEFAULT_DESC, null);
  }

  @Test
  public void should_reject_invalid_key_pattern_on_creation() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Invalid pattern: [azerty. Should be a valid Java regular expression.");

    service.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, "[azerty");
  }

  @Test
  public void should_delete_permission_template() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.deletePermissionTemplate(1L);

    verify(permissionTemplateDao, times(1)).deletePermissionTemplate(1L);
  }

  @Test
  public void should_retrieve_permission_template() throws Exception {

    List<PermissionTemplateUserDto> usersPermissions = Lists.newArrayList(
      buildUserPermission("user_scan", GlobalPermissions.SCAN_EXECUTION),
      buildUserPermission("user_dry_run", GlobalPermissions.PREVIEW_EXECUTION),
      buildUserPermission("user_scan_and_dry_run", GlobalPermissions.SCAN_EXECUTION),
      buildUserPermission("user_scan_and_dry_run", GlobalPermissions.PREVIEW_EXECUTION)
      );

    List<PermissionTemplateGroupDto> groupsPermissions = Lists.newArrayList(
      buildGroupPermission("admin_group", GlobalPermissions.SYSTEM_ADMIN),
      buildGroupPermission("scan_group", GlobalPermissions.SCAN_EXECUTION),
      buildGroupPermission(null, GlobalPermissions.PREVIEW_EXECUTION)
      );

    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto()
      .setId(1L)
      .setName("my template")
      .setDescription("my description")
      .setUsersPermissions(usersPermissions)
      .setGroupsByPermission(groupsPermissions);

    when(permissionTemplateDao.selectPermissionTemplate("my template")).thenReturn(permissionTemplateDto);

    PermissionTemplate permissionTemplate = service.selectPermissionTemplate("my template");

    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.DASHBOARD_SHARING)).isEmpty();
    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.SCAN_EXECUTION)).extracting("userName").containsOnly("user_scan", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.PREVIEW_EXECUTION)).extracting("userName").containsOnly("user_dry_run", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.DASHBOARD_SHARING)).isEmpty();
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.SCAN_EXECUTION)).extracting("groupName").containsOnly("scan_group");
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.SYSTEM_ADMIN)).extracting("groupName").containsOnly("admin_group");
  }

  @Test
  public void should_retrieve_all_permission_templates() throws Exception {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = service.selectAllPermissionTemplates();

    assertThat(templates).hasSize(2);
    assertThat(templates).extracting("id").containsOnly(1L, 2L);
    assertThat(templates).extracting("name").containsOnly("template1", "template2");
    assertThat(templates).extracting("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_retrieve_all_permission_templates_from_project() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = service.selectAllPermissionTemplates("org.sample.Sample");

    assertThat(templates).hasSize(2);
    assertThat(templates).extracting("id").containsOnly(1L, 2L);
    assertThat(templates).extracting("name").containsOnly("template1", "template2");
    assertThat(templates).extracting("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_update_permission_template() throws Exception {

    service.updatePermissionTemplate(1L, "new_name", "new_description", null);

    verify(permissionTemplateDao).updatePermissionTemplate(1L, "new_name", "new_description", null);
  }

  @Test
  public void should_validate_template_name_on_update_if_applicable() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with that name already exists");

    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    service.updatePermissionTemplate(1L, "template2", "template1", null);
  }

  @Test
  public void should_validate_template_key_pattern_on_update_if_applicable() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Invalid pattern: [azerty. Should be a valid Java regular expression.");

    PermissionTemplateDto template1 = new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1));

    service.updatePermissionTemplate(1L, "template1", "template1", "[azerty");
  }

  @Test
  public void should_skip_name_validation_where_not_applicable() throws Exception {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    service.updatePermissionTemplate(1L, "template1", "new_description", null);

    verify(permissionTemplateDao).updatePermissionTemplate(1L, "template1", "new_description", null);
  }

  @Test
  public void should_add_user_permission() throws Exception {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.addUserPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "user");

    verify(permissionTemplateDao, times(1)).addUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_user_login() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown user:");

    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectActiveUserByLogin("unknown")).thenReturn(null);

    service.addUserPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_user_permission() throws Exception {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.removeUserPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "user");

    verify(permissionTemplateDao, times(1)).removeUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_group_permission() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group")).thenReturn(groupDto);
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.addGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "group");

    verify(permissionTemplateDao, times(1)).addGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_group_name() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown group:");

    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectGroupByName("unknown")).thenReturn(null);

    service.addGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_group_permission() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group")).thenReturn(groupDto);
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.removeGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "group");

    verify(permissionTemplateDao, times(1)).removeGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_permission_to_anyone_group() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.addGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "Anyone");

    verify(permissionTemplateDao).addGroupPermission(1L, null, DEFAULT_PERMISSION);
    verifyZeroInteractions(userDao);
  }

  @Test
  public void should_remove_permission_from_anyone_group() throws Exception {
    when(permissionTemplateDao.selectTemplateByKey(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    service.removeGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "Anyone");

    verify(permissionTemplateDao).removeGroupPermission(1L, null, DEFAULT_PERMISSION);
    verifyZeroInteractions(userDao);
  }

  @Test
  public void should_remove_group_from_templates() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group", session)).thenReturn(groupDto);

    service.removeGroupFromTemplates("group");

    verify(permissionTemplateDao).removeByGroup(eq(1L), eq(session));
  }

  private PermissionTemplateUserDto buildUserPermission(String userName, String permission) {
    return new PermissionTemplateUserDto().setUserName(userName).setPermission(permission);
  }

  private PermissionTemplateGroupDto buildGroupPermission(String groupName, String permission) {
    return new PermissionTemplateGroupDto().setGroupName(groupName).setPermission(permission);
  }
}
