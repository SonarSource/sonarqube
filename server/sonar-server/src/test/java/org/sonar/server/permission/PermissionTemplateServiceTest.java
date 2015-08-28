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
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.PermissionTemplateGroupDto;
import org.sonar.db.permission.PermissionTemplateUserDto;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PermissionTemplateServiceTest {

  private static final String DEFAULT_KEY = "my_template";
  private static final String DEFAULT_DESC = "my description";
  private static final String DEFAULT_PATTERN = "com.foo.(.*)";
  private static final String DEFAULT_PERMISSION = UserRole.USER;
  private static final PermissionTemplateDto DEFAULT_TEMPLATE =
    new PermissionTemplateDto().setId(1L).setName(DEFAULT_KEY).setDescription(DEFAULT_DESC).setKeyPattern(DEFAULT_PATTERN);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
  UserDao userDao = mock(UserDao.class);
  GroupDao groupDao = mock(GroupDao.class);
  PermissionFinder finder = mock(PermissionFinder.class);
  DbSession session = mock(DbSession.class);

  PermissionTemplateService underTest;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp() {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.permissionTemplateDao()).thenReturn(permissionTemplateDao);
    when(dbClient.userDao()).thenReturn(userDao);
    when(dbClient.groupDao()).thenReturn(groupDao);
    underTest = new PermissionTemplateService(dbClient, userSessionRule, finder);
  }

  @Test
  public void find_users_with_permission_template() {
    underTest.findUsersWithPermissionTemplate(ImmutableMap.<String, Object>of(
      "permission", "user",
      "template", "my_template",
      "selected", "all"));
    verify(finder).findUsersWithPermissionTemplate(any(PermissionQuery.class));
  }

  @Test
  public void find_groups_with_permission_template() {
    underTest.findGroupsWithPermissionTemplate(ImmutableMap.<String, Object>of(
      "permission", "user",
      "template", "my_template",
      "selected", "all"));

    verify(finder).findGroupsWithPermissionTemplate(any(PermissionQuery.class));
  }

  @Test
  public void should_create_permission_template() {
    when(permissionTemplateDao.insert(any(DbSession.class), any(PermissionTemplateDto.class))).thenReturn(DEFAULT_TEMPLATE);

    PermissionTemplate permissionTemplate = underTest.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, DEFAULT_PATTERN);

    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo(DEFAULT_KEY);
    assertThat(permissionTemplate.getDescription()).isEqualTo(DEFAULT_DESC);
    assertThat(permissionTemplate.getKeyPattern()).isEqualTo(DEFAULT_PATTERN);
  }

  @Test
  public void should_enforce_unique_template_name() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with the name 'my_template' already exists (case insensitive).");

    when(permissionTemplateDao.selectByName(any(DbSession.class), anyString())).thenReturn(DEFAULT_TEMPLATE);

    underTest.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, null);
  }

  @Test
  public void should_reject_empty_name_on_creation() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("The template name must not be blank");

    underTest.createPermissionTemplate("", DEFAULT_DESC, null);
  }

  @Test
  public void should_reject_invalid_key_pattern_on_creation() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");

    underTest.createPermissionTemplate(DEFAULT_KEY, DEFAULT_DESC, "[azerty");
  }

  @Test
  public void should_delete_permission_template() {
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.deletePermissionTemplate(1L);

    verify(permissionTemplateDao).deleteById(any(DbSession.class), eq(1L));
  }

  @Test
  public void should_retrieve_permission_template() {

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

    when(permissionTemplateDao.selectByUuidWithUserAndGroupPermissions("my template")).thenReturn(permissionTemplateDto);

    PermissionTemplate permissionTemplate = underTest.selectPermissionTemplate("my template");

    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.DASHBOARD_SHARING)).isEmpty();
    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.SCAN_EXECUTION)).extracting("userName").containsOnly("user_scan", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.PREVIEW_EXECUTION)).extracting("userName").containsOnly("user_dry_run", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.DASHBOARD_SHARING)).isEmpty();
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.SCAN_EXECUTION)).extracting("groupName").containsOnly("scan_group");
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.SYSTEM_ADMIN)).extracting("groupName").containsOnly("admin_group");
  }

  @Test
  public void should_retrieve_all_permission_templates() {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = underTest.selectAllPermissionTemplates();

    assertThat(templates).hasSize(2);
    assertThat(templates).extracting("id").containsOnly(1L, 2L);
    assertThat(templates).extracting("name").containsOnly("template1", "template2");
    assertThat(templates).extracting("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_retrieve_all_permission_templates_from_project() {
    userSessionRule.login("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = underTest.selectAllPermissionTemplates("org.sample.Sample");

    assertThat(templates).hasSize(2);
    assertThat(templates).extracting("id").containsOnly(1L, 2L);
    assertThat(templates).extracting("name").containsOnly("template1", "template2");
    assertThat(templates).extracting("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_update_permission_template() {

    underTest.updatePermissionTemplate(1L, "new_name", "new_description", null);

    verify(permissionTemplateDao).update(1L, "new_name", "new_description", null);
  }

  @Test
  public void should_validate_template_name_on_update_if_applicable() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with the name 'template2' already exists (case insensitive).");

    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectByName(any(DbSession.class), eq("template2"))).thenReturn(template2);

    underTest.updatePermissionTemplate(1L, "template2", "template2", null);
  }

  @Test
  public void should_validate_template_key_pattern_on_update_if_applicable() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("The 'projectKeyPattern' parameter must be a valid Java regular expression. '[azerty' was passed");

    PermissionTemplateDto template1 = new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1));

    underTest.updatePermissionTemplate(1L, "template1", "template1", "[azerty");
  }

  @Test
  public void should_skip_name_validation_where_not_applicable() {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    underTest.updatePermissionTemplate(1L, "template1", "new_description", null);

    verify(permissionTemplateDao).update(1L, "template1", "new_description", null);
  }

  @Test
  public void should_add_user_permission() {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.addUserPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "user");

    verify(permissionTemplateDao, times(1)).insertUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_user_login() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown user:");

    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectActiveUserByLogin("unknown")).thenReturn(null);

    underTest.addUserPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_user_permission() {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.removeUserPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "user");

    verify(permissionTemplateDao, times(1)).deleteUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_group_permission() {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(groupDao.selectByName(any(DbSession.class), eq("group"))).thenReturn(groupDto);
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.addGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "group");

    verify(permissionTemplateDao, times(1)).insertGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_group_name() {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown group:");

    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);
    when(groupDao.selectByName(any(DbSession.class), eq("unknown"))).thenReturn(null);

    underTest.addGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_group_permission() {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(groupDao.selectByName(any(DbSession.class), eq("group"))).thenReturn(groupDto);
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.removeGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "group");

    verify(permissionTemplateDao, times(1)).deleteGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_permission_to_anyone_group() {
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.addGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "Anyone");

    verify(permissionTemplateDao).insertGroupPermission(1L, null, DEFAULT_PERMISSION);
    verifyZeroInteractions(userDao);
  }

  @Test
  public void should_remove_permission_from_anyone_group() {
    when(permissionTemplateDao.selectByUuid(DEFAULT_KEY)).thenReturn(DEFAULT_TEMPLATE);

    underTest.removeGroupPermission(DEFAULT_KEY, DEFAULT_PERMISSION, "Anyone");

    verify(permissionTemplateDao).deleteGroupPermission(1L, null, DEFAULT_PERMISSION);
    verifyZeroInteractions(userDao);
  }

  @Test
  public void should_remove_group_from_templates() {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(groupDao.selectByName(session, "group")).thenReturn(groupDto);

    underTest.removeGroupFromTemplates("group");

    verify(permissionTemplateDao).deleteByGroup(eq(session), eq(1L));
  }

  private PermissionTemplateUserDto buildUserPermission(String userName, String permission) {
    return new PermissionTemplateUserDto().setUserName(userName).setPermission(permission);
  }

  private PermissionTemplateGroupDto buildGroupPermission(String groupName, String permission) {
    return new PermissionTemplateGroupDto().setGroupName(groupName).setPermission(permission);
  }
}
