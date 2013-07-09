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
import org.sonar.core.user.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class InternalPermissionTemplateServiceTest {

  private static final String DEFAULT_NAME = "my template";
  private static final String DEFAULT_DESC = "my description";
  private static final String DEFAULT_PERMISSION = UserRole.USER;
  private static final PermissionTemplateDto DEFAULT_TEMPLATE =
    new PermissionTemplateDto().setId(1L).setName(DEFAULT_NAME).setDescription(DEFAULT_DESC);

  private PermissionDao permissionDao;
  private UserDao userDao;
  private InternalPermissionTemplateService permissionTemplateService;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void setUp() {
    MockUserSession.set().setLogin("admin").setPermissions(Permission.SYSTEM_ADMIN);
    permissionDao = mock(PermissionDao.class);
    userDao = mock(UserDao.class);
    permissionTemplateService = new InternalPermissionTemplateService(permissionDao, userDao);
  }

  @Test
  public void should_create_permission_template() throws Exception {
    when(permissionDao.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC)).thenReturn(DEFAULT_TEMPLATE);

    PermissionTemplate permissionTemplate = permissionTemplateService.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC);

    assertThat(permissionTemplate.getId()).isEqualTo(1L);
    assertThat(permissionTemplate.getName()).isEqualTo(DEFAULT_NAME);
    assertThat(permissionTemplate.getDescription()).isEqualTo(DEFAULT_DESC);
  }

  @Test
  public void should_enforce_unique_template_name() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with that name already exists");

    when(permissionDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(DEFAULT_TEMPLATE));

    permissionTemplateService.createPermissionTemplate(DEFAULT_NAME, DEFAULT_DESC);
  }

  @Test
  public void should_reject_empty_name_on_creation() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Name can't be blank");

    permissionTemplateService.createPermissionTemplate("", DEFAULT_DESC);
  }

  @Test
  public void should_delete_permission_template() throws Exception {
    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.deletePermissionTemplate(1L);

    verify(permissionDao, times(1)).deletePermissionTemplate(1L);
  }

  @Test
  public void should_retrieve_permission_template() throws Exception {

    List<PermissionTemplateUserDto> usersPermissions = Lists.newArrayList(
      buildUserPermission("user_scan", Permission.SCAN_EXECUTION.key()),
      buildUserPermission("user_dry_run", Permission.DRY_RUN_EXECUTION.key()),
      buildUserPermission("user_scan_and_dry_run", Permission.SCAN_EXECUTION.key()),
      buildUserPermission("user_scan_and_dry_run", Permission.DRY_RUN_EXECUTION.key())
    );

    List<PermissionTemplateGroupDto> groupsPermissions = Lists.newArrayList(
      buildGroupPermission("admin_group", Permission.SYSTEM_ADMIN.key()),
      buildGroupPermission("scan_group", Permission.SCAN_EXECUTION.key()),
      buildGroupPermission(null, Permission.DRY_RUN_EXECUTION.key())
    );

    PermissionTemplateDto permissionTemplateDto = new PermissionTemplateDto()
      .setId(1L)
      .setName("my template")
      .setDescription("my description")
      .setUsersPermissions(usersPermissions)
      .setGroupsByPermission(groupsPermissions);

    when(permissionDao.selectPermissionTemplate("my template")).thenReturn(permissionTemplateDto);

    PermissionTemplate permissionTemplate = permissionTemplateService.selectPermissionTemplate("my template");

    assertThat(permissionTemplate.getUsersForPermission(Permission.DASHBOARD_SHARING.key())).isEmpty();
    assertThat(permissionTemplate.getUsersForPermission(Permission.SCAN_EXECUTION.key())).onProperty("userName").containsOnly("user_scan", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getUsersForPermission(Permission.DRY_RUN_EXECUTION.key())).onProperty("userName").containsOnly("user_dry_run", "user_scan_and_dry_run");
    assertThat(permissionTemplate.getGroupsForPermission(Permission.DASHBOARD_SHARING.key())).isEmpty();
    assertThat(permissionTemplate.getGroupsForPermission(Permission.SCAN_EXECUTION.key())).onProperty("groupName").containsOnly("scan_group");
    assertThat(permissionTemplate.getGroupsForPermission(Permission.SYSTEM_ADMIN.key())).onProperty("groupName").containsOnly("admin_group");
  }

  @Test
  public void should_retrieve_all_permission_templates() throws Exception {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    List<PermissionTemplate> templates = permissionTemplateService.selectAllPermissionTemplates();

    assertThat(templates).hasSize(2);
    assertThat(templates).onProperty("id").containsOnly(1L, 2L);
    assertThat(templates).onProperty("name").containsOnly("template1", "template2");
    assertThat(templates).onProperty("description").containsOnly("template1", "template2");
  }

  @Test
  public void should_update_permission_template() throws Exception {

    permissionTemplateService.updatePermissionTemplate(1L, "new_name", "new_description");

    verify(permissionDao).updatePermissionTemplate(1L, "new_name", "new_description");
  }

  @Test
  public void should_validate_template_name_on_update_if_applicable() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("A template with that name already exists");

    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    permissionTemplateService.updatePermissionTemplate(1L, "template2", "template1");
  }

  @Test
  public void should_skip_name_validation_where_not_applicable() throws Exception {
    PermissionTemplateDto template1 =
      new PermissionTemplateDto().setId(1L).setName("template1").setDescription("template1");
    PermissionTemplateDto template2 =
      new PermissionTemplateDto().setId(2L).setName("template2").setDescription("template2");
    when(permissionDao.selectAllPermissionTemplates()).thenReturn(Lists.newArrayList(template1, template2));

    permissionTemplateService.updatePermissionTemplate(1L, "template1", "new_description");

    verify(permissionDao).updatePermissionTemplate(1L, "template1", "new_description");
  }

  @Test
  public void should_add_user_permission() throws Exception {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.addUserPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "user");

    verify(permissionDao, times(1)).addUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_user_login() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown user:");

    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectActiveUserByLogin("unknown")).thenReturn(null);

    permissionTemplateService.addUserPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_user_permission() throws Exception {
    UserDto userDto = new UserDto().setId(1L).setLogin("user").setName("user");
    when(userDao.selectActiveUserByLogin("user")).thenReturn(userDto);
    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.removeUserPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "user");

    verify(permissionDao, times(1)).removeUserPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_add_group_permission() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group")).thenReturn(groupDto);
    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.addGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "group");

    verify(permissionDao, times(1)).addGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  @Test
  public void should_validate_provided_group_name() throws Exception {
    expected.expect(BadRequestException.class);
    expected.expectMessage("Unknown group:");

    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);
    when(userDao.selectGroupByName("unknown")).thenReturn(null);

    permissionTemplateService.addGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "unknown");
  }

  @Test
  public void should_remove_group_permission() throws Exception {
    GroupDto groupDto = new GroupDto().setId(1L).setName("group");
    when(userDao.selectGroupByName("group")).thenReturn(groupDto);
    when(permissionDao.selectTemplateByName(DEFAULT_NAME)).thenReturn(DEFAULT_TEMPLATE);

    permissionTemplateService.removeGroupPermission(DEFAULT_NAME, DEFAULT_PERMISSION, "group");

    verify(permissionDao, times(1)).removeGroupPermission(1L, 1L, DEFAULT_PERMISSION);
  }

  private PermissionTemplateUserDto buildUserPermission(String userName, String permission) {
    return new PermissionTemplateUserDto().setUserName(userName).setPermission(permission);
  }

  private PermissionTemplateGroupDto buildGroupPermission(String groupName, String permission) {
    return new PermissionTemplateGroupDto().setGroupName(groupName).setPermission(permission);
  }
}
