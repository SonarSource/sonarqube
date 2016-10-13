/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.platform.PersistentSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_KEY;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;

public class RegisterPermissionTemplatesTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public LogTester logTester = new LogTester();

  private PersistentSettings settings = mock(PersistentSettings.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private RegisterPermissionTemplates underTest = new RegisterPermissionTemplates(db.getDbClient(), settings, defaultOrganizationProvider);

  @Test
  public void insert_default_permission_template_if_fresh_install() {
    db.users().insertGroup(db.getDefaultOrganization(), DefaultGroups.ADMINISTRATORS);

    underTest.start();

    PermissionTemplateDto defaultTemplate = selectTemplate();
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");

    List<PermissionTemplateGroupDto> groupPermissions = selectGroupPermissions(defaultTemplate);
    assertThat(groupPermissions).hasSize(4);
    expectGroupPermission(groupPermissions, UserRole.ADMIN, DefaultGroups.ADMINISTRATORS);
    expectGroupPermission(groupPermissions, UserRole.ISSUE_ADMIN, DefaultGroups.ADMINISTRATORS);
    expectGroupPermission(groupPermissions, UserRole.CODEVIEWER, DefaultGroups.ANYONE);
    expectGroupPermission(groupPermissions, UserRole.USER, DefaultGroups.ANYONE);

    // template is marked as default
    verify(settings).saveProperty(DEFAULT_TEMPLATE_PROPERTY, defaultTemplate.getUuid());

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void ignore_administrators_permissions_if_group_does_not_exist() {
    underTest.start();

    PermissionTemplateDto defaultTemplate = selectTemplate();
    assertThat(defaultTemplate.getName()).isEqualTo("Default template");

    List<PermissionTemplateGroupDto> groupPermissions = selectGroupPermissions(defaultTemplate);
    assertThat(groupPermissions).hasSize(2);
    expectGroupPermission(groupPermissions, UserRole.CODEVIEWER, DefaultGroups.ANYONE);
    expectGroupPermission(groupPermissions, UserRole.USER, DefaultGroups.ANYONE);

    // marked as default
    verify(settings).saveProperty(DEFAULT_TEMPLATE_PROPERTY, defaultTemplate.getUuid());

    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Cannot setup default permission for group: sonar-administrators");
  }

  @Test
  public void do_not_create_default_template_if_already_exists() {
    markTaskAsAlreadyExecuted();

    underTest.start();

    assertThat(selectTemplate()).isNull();
    verify(settings, never()).saveProperty(eq(DEFAULT_TEMPLATE_PROPERTY), anyString());
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
  }

  @Test
  public void reference_TRK_template_as_default_when_present() {
    when(settings.getString(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT))).thenReturn("my_projects_template");
    markTaskAsAlreadyExecuted();

    underTest.start();

    verify(settings).saveProperty(DEFAULT_TEMPLATE_PROPERTY, "my_projects_template");
  }

  private void markTaskAsAlreadyExecuted() {
    db.getDbClient().loadedTemplateDao().insert(new LoadedTemplateDto(DEFAULT_TEMPLATE_KEY, LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE));
  }

  private PermissionTemplateDto selectTemplate() {
    return db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), DEFAULT_TEMPLATE_KEY);
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
}
