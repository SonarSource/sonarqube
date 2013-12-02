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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.permission.PermissionTemplateGroupDto;
import org.sonar.core.permission.PermissionTemplateUserDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class InternalPermissionTemplateServiceTest {

  private static final String DEFAULT_NAME = "my template";
  private static final String DEFAULT_DESC = "my description";
  private static final String DEFAULT_PATTERN = "com.foo.(.*)";
  private static final String DEFAULT_PERMISSION = UserRole.USER;
  private static final PermissionTemplateDto DEFAULT_TEMPLATE =
    new PermissionTemplateDto().setId(1L).setName(DEFAULT_NAME).setDescription(DEFAULT_DESC).setKeyPattern(DEFAULT_PATTERN);

  private PermissionTemplateDao permissionTemplateDao;
  private UserDao userDao;

  private InternalPermissionTemplateService permissionTemplateService;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp() {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    permissionTemplateDao = mock(PermissionTemplateDao.class);
    userDao = mock(UserDao.class);
    permissionTemplateService = new InternalPermissionTemplateService(permissionTemplateDao, userDao);
  }

  @Test
  public void should_create_permission_template() throws Exception {
    when(permissionTemplateDao.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC, DEFAULT_PATTERN)).thenReturn(DEFAULT_TEMPLATE);

    PermissionTemplate permissionTemplate = permissionTemplateService.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC, DEFAULT_PATTERN);

    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo(DEFAULT_NAME);
    assertThat(permissionTemplate.getDescription()).isEqualTo(DEFAULT_DESC);
    assertThat(permissionTemplate.getKeyPattern()).isEqualTo(DEFAULT_PATTERN);
  }

  @Test
  public void should_enforce_unique_template_name() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with that name already exists");

    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(DEFAULT_TEMPLATE));

    permissionTemplateService.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC, null);
  }

  @Test
  public void should_reject_empty_name_on_creation() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Name can't be blank");

    permissionTemplateService.createPermissionTemplate("", DEFAULT_DESC, null);
  }

  @Test
  public void should_reject_invalid_key_pattern_on_creation() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Invalid pattern: [azerty. Should be a valid Java regular expression.");

    permissionTemplateService.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC, "[azerty");
  }

  @Test
  public void should_delete_permission_template() throws Exception {
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.deletePermissionTemplate(1L);

    verify(permissionTemplateDao, times(1)).deletePermissionTemplate(1L);
  }

  @Test
  public void should_retrieve_permission_template() throws Exception {

    List<PermissionTemplateUserDto> usersPermissions = Lists.newArrayList(
      buildUserPermission("user_scan", GlobalPermissions.SCAN_EXECUTION),
      buildUserPermission("user_dry_run", GlobalPermissions.DRY_RUN_EXECUTION),
      buildUserPermission("user_scan_and_dry_run", GlobalPermissions.SCAN_EXECUTION),
      buildUserPermission("user_scan_and_dry_run", GlobalPermissions.DRY_RUN_EXECUTION)
      );

    List<PermissionTemplateGroupDto> groupsPermissions = Lists.newArrayList(
      buildGroupPermission("admin_group", GlobalPermissions.SYSTEM_ADMIN),
      buildGroupPermission("scan_group", GlobalPermissions.SCAN_EXECUTION),
      buildGroupPermission(null, GlobalPermissions.DRY_RUN_EXECUTION)
      );

    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto()
      .setId(1L)
      .setName("my template")
      .setDescription("my description")
      .setUsersPermissions(usersPermissions)
      .setGroupsByPermission(groupsPermissions);

    when(permissionTemplateDao.selectPermissionTemplate("my template")).thenReturn(permissionTemplateDto);

    PermissionTemplate permissionTemplate = permissionTemplateService.selectPermissionTemplate("my template");

    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.DASHBOARD_SHARING)).isEmpty();
    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.SCAN_EXECUTION)).onProperty("userName").containsOnly("user_scan", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getUsersForPermission(GlobalPermissions.DRY_RUN_EXECUTION)).onProperty("userName").containsOnly("user_dry_run", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.DASHBOARD_SHARING)).isEmpty();
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.SCAN_EXECUTION)).onProperty("groupName").containsOnly("scan_group");
    assertThat(permissionTemplate.getGroupsForPermission(GlobalPermissions.SYSTEM_ADMIN)).onProperty("groupName").containsOnly("admin_group");
  }

  @Test
  public void should_retrieve_all_permission_templates() throws Exception {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = permissionTemplateService.selectAllPermissionTemplates();

    assertThat(templates).hasSize(2);
    assertThat(templates).onProperty("id").containsOnly(1L, 2L);
    assertThat(templates).onProperty("name").containsOnly("template1", "template2");
    assertThat(templates).onProperty("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_retrieve_all_permission_templates_from_project() throws Exception {
    MockUserSession.set().setLogin("admin").addProjectPermissions(UserRole.ADMIN, "org.sample.Sample");

    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = permissionTemplateService.selectAllPermissionTemplates("org.sample.Sample");

    assertThat(templates).hasSize(2);
    assertThat(templates).onProperty("id").containsOnly(1L, 2L);
    assertThat(templates).onProperty("name").containsOnly("template1", "template2");
    assertThat(templates).onProperty("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_update_permission_template() throws Exception {

    permissionTemplateService.updatePermissionTemplate(1L, "new_name", "new_description", null);

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

    permissionTemplateService.updatePermissionTemplate(1L, "template2", "template1", null);
  }

  @Test
  public void should_validate_template_key_pattern_on_update_if_applicable() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Invalid pattern: [azerty. Should be a valid Java regular expression.");

    PermissionTemplateDto template1 = new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1));

    permissionTemplateService.updatePermissionTemplate(1L, "template1", "template1", "[azerty");
  }

  @Test
  public void should_skip_name_validation_where_not_applicable() throws Exception {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionTemplateDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    permissionTemplateService.updatePermissionTemplate(1L, "template1", "new_description", null);

    verify(permissionTemplateDao).updatePermissionTemplate(1L, "template1", "new_description", null);
  }

  @Test
  public void should_add_user_permission() throws Exception {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.addUserPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "user");

    verify(permissionTemplateDao, times(1)).addUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_user_login() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown user:");

    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectActiveUserByLogin("unknown")).thenReturn(null);

    permissionTemplateService.addUserPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_user_permission() throws Exception {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.removeUserPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "user");

    verify(permissionTemplateDao, times(1)).removeUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_group_permission() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group")).thenReturn(groupDto);
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.addGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "group");

    verify(permissionTemplateDao, times(1)).addGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_group_name() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown group:");

    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectGroupByName("unknown")).thenReturn(null);

    permissionTemplateService.addGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_group_permission() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group")).thenReturn(groupDto);
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.removeGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "group");

    verify(permissionTemplateDao, times(1)).removeGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_permission_to_anyone_group() throws Exception {
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.addGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "Anyone");

    verify(permissionTemplateDao).addGroupPermission(1L, null, DEFAULT_PERMISSION);
    verifyZeroInteractions(userDao);
  }

  @Test
  public void should_remove_permission_from_anyone_group() throws Exception {
    when(permissionTemplateDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.removeGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "Anyone");

    verify(permissionTemplateDao).removeGroupPermission(1L, null, DEFAULT_PERMISSION);
    verifyZeroInteractions(userDao);
  }

  private PermissionTemplateUserDto buildUserPermission(String userName, String permission) {
    return new PermissionTemplateUserDto().setUserName(userName).setPermission(permission);
  }

  private PermissionTemplateGroupDto buildGroupPermission(String groupName, String permission) {
    return new PermissionTemplateGroupDto().setGroupName(groupName).setPermission(permission);
  }
}
