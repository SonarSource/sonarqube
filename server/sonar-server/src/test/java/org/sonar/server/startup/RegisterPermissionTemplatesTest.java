/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.startup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public class RegisterPermissionTemplatesTest {
  private static final String DEFAULT_TEMPLATE_UUID = "default_template";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private RegisterPermissionTemplates underTest = new RegisterPermissionTemplates(db.getDbClient(), defaultOrganizationProvider);

  @Test
  public void fail_with_ISE_if_default_template_must_be_created_and_no_default_group_is_defined() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group for organization " + db.getDefaultOrganization().getUuid() + " is not defined");

    underTest.start();
  }

  @Test
  public void fail_with_ISE_if_default_template_must_be_created_and_default_group_does_not_exist() {
    setDefaultGroupId(new GroupDto().setId(22));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group with id 22 for organization " + db.getDefaultOrganization().getUuid() + " doesn't exist");

    underTest.start();
  }

  @Test
  public void insert_default_permission_template_if_fresh_install() {
    GroupDto defaultGroup = createAndSetDefaultGroup();
    db.users().insertGroup(db.getDefaultOrganization(), DefaultGroups.ADMINISTRATORS);

    underTest.start();

    PermissionTemplateDto defaultTemplate = selectTemplate();
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");

    List<PermissionTemplateGroupDto> groupPermissions = selectGroupPermissions(defaultTemplate);
    assertThat(groupPermissions).hasSize(4);
    expectGroupPermission(groupPermissions, UserRole.ADMIN, DefaultGroups.ADMINISTRATORS);
    expectGroupPermission(groupPermissions, UserRole.ISSUE_ADMIN, DefaultGroups.ADMINISTRATORS);
    expectGroupPermission(groupPermissions, UserRole.CODEVIEWER, defaultGroup.getName());
    expectGroupPermission(groupPermissions, UserRole.USER, defaultGroup.getName());

    verifyDefaultTemplates();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void ignore_administrators_permissions_if_group_does_not_exist() {
    GroupDto defaultGroup = createAndSetDefaultGroup();

    underTest.start();

    PermissionTemplateDto defaultTemplate = selectTemplate();
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");

    List<PermissionTemplateGroupDto> groupPermissions = selectGroupPermissions(defaultTemplate);
    assertThat(groupPermissions).hasSize(2);
    expectGroupPermission(groupPermissions, UserRole.CODEVIEWER, defaultGroup.getName());
    expectGroupPermission(groupPermissions, UserRole.USER, defaultGroup.getName());

    verifyDefaultTemplates();

    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Cannot setup default permission for group: sonar-administrators");
  }

  @Test
  public void do_not_create_default_template_if_already_exists_but_register_when_it_is_not() {
    db.permissionTemplates().insertTemplate(newPermissionTemplateDto()
      .setOrganizationUuid(db.getDefaultOrganization().getUuid())
      .setUuid(DEFAULT_TEMPLATE_UUID));

    underTest.start();

    verifyDefaultTemplates();
  }

  @Test
  public void do_not_fail_if_default_template_exists_and_is_registered() {
    PermissionTemplateDto projectTemplate = db.permissionTemplates().insertTemplate(newPermissionTemplateDto()
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .setUuid(DEFAULT_TEMPLATE_UUID));
    db.organizations().setDefaultTemplates(projectTemplate, null);

    underTest.start();

    verifyDefaultTemplates();
  }

  private PermissionTemplateDto selectTemplate() {
    return db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), DEFAULT_TEMPLATE_UUID);
  }

  private List<PermissionTemplateGroupDto> selectGroupPermissions(PermissionTemplateDto template) {
    return db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateId(db.getSession(), template.getId());
  }

  private void expectGroupPermission(List<PermissionTemplateGroupDto> groupPermissions, String expectedPermission,
    String expectedGroupName) {
    assertThat(
      groupPermissions.stream().anyMatch(gp -> gp.getPermission().equals(expectedPermission) && Objects.equals(gp.getGroupName(), expectedGroupName)))
        .isTrue();
  }

  private void verifyDefaultTemplates() {
    Optional<DefaultTemplates> defaultTemplates = db.getDbClient().organizationDao().getDefaultTemplates(db.getSession(), db.getDefaultOrganization().getUuid());
    assertThat(defaultTemplates)
      .isPresent();
    assertThat(defaultTemplates.get().getProjectUuid()).isEqualTo(DEFAULT_TEMPLATE_UUID);
  }

  private void setDefaultGroupId(GroupDto defaultGroup) {
    db.getDbClient().organizationDao().setDefaultGroupId(db.getSession(), db.getDefaultOrganization().getUuid(), defaultGroup);
    db.commit();
  }

  private GroupDto createAndSetDefaultGroup() {
    GroupDto res = db.users().insertGroup(db.getDefaultOrganization());
    setDefaultGroupId(res);
    return res;
  }
}
