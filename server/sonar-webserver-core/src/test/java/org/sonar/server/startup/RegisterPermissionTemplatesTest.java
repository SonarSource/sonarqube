/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.slf4j.event.Level;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.server.property.InternalProperties.DEFAULT_PROJECT_TEMPLATE;

public class RegisterPermissionTemplatesTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private RegisterPermissionTemplates underTest = new RegisterPermissionTemplates(db.getDbClient(), UuidFactoryFast.getInstance(), System2.INSTANCE, new DefaultGroupFinder(db.getDbClient()));

  @Test
  public void insert_default_permission_template_if_fresh_install() {
    GroupDto defaultGroup = db.users().insertDefaultGroup();
    db.users().insertGroup(DefaultGroups.ADMINISTRATORS);

    underTest.start();

    PermissionTemplateDto defaultTemplate = selectTemplate();
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");

    List<PermissionTemplateGroupDto> groupPermissions = selectGroupPermissions(defaultTemplate);
    assertThat(groupPermissions).hasSize(5);
    expectGroupPermission(groupPermissions, ProjectPermission.ADMIN, DefaultGroups.ADMINISTRATORS);
    expectGroupPermission(groupPermissions, ProjectPermission.CODEVIEWER, defaultGroup.getName());
    expectGroupPermission(groupPermissions, ProjectPermission.USER, defaultGroup.getName());
    expectGroupPermission(groupPermissions, ProjectPermission.ISSUE_ADMIN, defaultGroup.getName());
    expectGroupPermission(groupPermissions, ProjectPermission.SECURITYHOTSPOT_ADMIN, defaultGroup.getName());

    verifyDefaultTemplateForProject(defaultTemplate.getUuid());

    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

  @Test
  public void ignore_administrators_permissions_if_group_does_not_exist() {
    GroupDto defaultGroup = db.users().insertDefaultGroup();

    underTest.start();

    PermissionTemplateDto defaultTemplate = selectTemplate();
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");

    List<PermissionTemplateGroupDto> groupPermissions = selectGroupPermissions(defaultTemplate);
    assertThat(groupPermissions).hasSize(4);
    expectGroupPermission(groupPermissions, ProjectPermission.CODEVIEWER, defaultGroup.getName());
    expectGroupPermission(groupPermissions, ProjectPermission.USER, defaultGroup.getName());
    expectGroupPermission(groupPermissions, ProjectPermission.ISSUE_ADMIN, defaultGroup.getName());
    expectGroupPermission(groupPermissions, ProjectPermission.SECURITYHOTSPOT_ADMIN, defaultGroup.getName());

    verifyDefaultTemplateForProject(defaultTemplate.getUuid());

    assertThat(logTester.logs(Level.ERROR)).contains("Cannot setup default permission for group: sonar-administrators");
  }

  @Test
  public void do_not_fail_if_default_template_exists() {
    db.users().insertDefaultGroup();
    PermissionTemplateDto projectTemplate = db.permissionTemplates().insertTemplate(newPermissionTemplateDto());
    db.getDbClient().internalPropertiesDao().save(db.getSession(), DEFAULT_PROJECT_TEMPLATE, projectTemplate.getUuid());
    db.commit();

    underTest.start();

    verifyDefaultTemplateForProject(projectTemplate.getUuid());
  }

  private PermissionTemplateDto selectTemplate() {
    return db.getDbClient().permissionTemplateDao().selectByName(db.getSession(), "Default template");
  }

  private List<PermissionTemplateGroupDto> selectGroupPermissions(PermissionTemplateDto template) {
    return db.getDbClient().permissionTemplateDao().selectGroupPermissionsByTemplateUuid(db.getSession(), template.getUuid());
  }

  private void expectGroupPermission(List<PermissionTemplateGroupDto> groupPermissions, ProjectPermission expectedPermission,
    String expectedGroupName) {
    assertThat(
      groupPermissions.stream().anyMatch(gp -> gp.getPermission().equals(expectedPermission.getKey()) && Objects.equals(gp.getGroupName(), expectedGroupName)))
        .isTrue();
  }

  private void verifyDefaultTemplateForProject(String expectedDefaultTemplateForProjectUuid) {
    Optional<String> defaultPermissionTemplateForProject = db.getDbClient().internalPropertiesDao().selectByKey(db.getSession(), DEFAULT_PROJECT_TEMPLATE);
    assertThat(defaultPermissionTemplateForProject).isPresent();
    assertThat(defaultPermissionTemplateForProject).contains(expectedDefaultTemplateForProjectUuid);
  }

}
