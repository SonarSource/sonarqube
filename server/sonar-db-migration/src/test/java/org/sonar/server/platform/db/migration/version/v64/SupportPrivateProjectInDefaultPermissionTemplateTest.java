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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SupportPrivateProjectInDefaultPermissionTemplateTest {

  private static final String DEFAULT_ORGANIZATION_UUID = "def org uuid";
  private static final String OTHER_ORGANIZATION_UUID = "not def org uuid";
  private static final String PERMISSION_USER = "user";
  private static final String PERMISSION_CODEVIEWER = "codeviewer";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SupportPrivateProjectInDefaultPermissionTemplateTest.class, "organizations_and_groups_and_permission_templates.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SupportPrivateProjectInDefaultPermissionTemplate underTest = new SupportPrivateProjectInDefaultPermissionTemplate(db.database(),
    new DefaultOrganizationUuidProviderImpl());

  @Test
  public void fails_with_ISE_when_no_default_organization_is_set() throws SQLException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization uuid is missing");

    underTest.execute();
  }

  @Test
  public void fails_with_ISE_when_default_organization_does_not_exist_in_table_ORGANIZATIONS() throws SQLException {
    setDefaultOrganizationProperty("blabla");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization with uuid 'blabla' does not exist in table ORGANIZATIONS");

    underTest.execute();
  }

  @Test
  public void execute_fails_with_ISE_when_default_organization_has_no_default_groupId() throws SQLException {
    setupDefaultOrganization(null, "pt1", "pt2");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No default group id is defined for default organization (uuid=def org uuid)");

    underTest.execute();
  }

  @Test
  public void execute_fails_with_ISE_when_default_group_of_default_organization_does_not_exist() throws SQLException {
    setupDefaultOrganization(112, "pT1", "pT2");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Permission template with uuid pT1 not found");

    underTest.execute();
  }

  @Test
  public void execute_does_nothing_when_default_organization_has_default_permission_template_for_projects() throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    setupDefaultOrganization(groupId, null, null);

    underTest.execute();
  }

  @Test
  public void execute_fails_with_ISE_when_default_organization_has_default_permission_template_for_views_but_not_for_projects() throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    setupDefaultOrganization(groupId, null, "pt1");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Inconsistent state for default organization (uuid=def org uuid): no project default template is defined but view default template is");

    underTest.execute();
  }

  @Test
  public void execute_ignores_default_permission_template_for_view_of_default_organization_if_it_does_not_exist_and_removes_the_reference() throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    IdAndUuid projectDefPermTemplate = insertPermissionTemplate(DEFAULT_ORGANIZATION_UUID);
    setupDefaultOrganization(groupId, projectDefPermTemplate.uuid, "fooBar");

    underTest.execute();

    assertThat(
      db.select("select default_perm_template_project as \"project\", default_perm_template_view as \"view\" from organizations where uuid='" + DEFAULT_ORGANIZATION_UUID + "'"))
        .extracting((row) -> row.get("project"), (row) -> row.get("view"))
        .containsOnly(tuple(projectDefPermTemplate.uuid, null));
  }

  @Test
  public void execute_does_not_fail_when_default_organization_has_default_permission_template_for_view() throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    IdAndUuid projectDefPermTemplate = insertPermissionTemplate(DEFAULT_ORGANIZATION_UUID);
    setupDefaultOrganization(groupId, projectDefPermTemplate.uuid, null);

    underTest.execute();
  }

  @Test
  public void execute_adds_permission_USER_and_CODEVIEWER_to_default_group_of_default_organization_in_its_default_project_template() throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    IdAndUuid projectDefPermTemplate = insertPermissionTemplate(DEFAULT_ORGANIZATION_UUID);
    setupDefaultOrganization(groupId, projectDefPermTemplate.uuid, null);
    int otherGroupId = insertGroup(OTHER_ORGANIZATION_UUID);
    IdAndUuid otherProjectDefPermTemplate = insertPermissionTemplate(OTHER_ORGANIZATION_UUID);
    insertOrganization(OTHER_ORGANIZATION_UUID, otherGroupId, otherProjectDefPermTemplate.uuid, null);

    underTest.execute();

    verifyPermissionOfGroupInTemplate(projectDefPermTemplate, groupId, PERMISSION_USER, PERMISSION_CODEVIEWER);
    verifyPermissionOfGroupInTemplate(otherProjectDefPermTemplate, otherGroupId);
  }

  @Test
  public void execute_does_not_fail_if_default_group_already_has_permission_USER_and_adds_only_CODEVIEWER_to_default_group_of_default_organization_in_its_default_project_template()
    throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    IdAndUuid projectDefPermTemplate = insertPermissionTemplate(DEFAULT_ORGANIZATION_UUID);
    insertGroupPermission(projectDefPermTemplate, groupId, PERMISSION_USER);
    setupDefaultOrganization(groupId, projectDefPermTemplate.uuid, null);
    int otherGroupId = insertGroup(OTHER_ORGANIZATION_UUID);
    IdAndUuid otherProjectDefPermTemplateUuid = insertPermissionTemplate(OTHER_ORGANIZATION_UUID);
    insertOrganization(OTHER_ORGANIZATION_UUID, otherGroupId, otherProjectDefPermTemplateUuid.uuid, null);

    underTest.execute();

    verifyPermissionOfGroupInTemplate(projectDefPermTemplate, groupId, PERMISSION_USER, PERMISSION_CODEVIEWER);
    verifyPermissionOfGroupInTemplate(otherProjectDefPermTemplateUuid, otherGroupId);
  }

  @Test
  public void execute_does_not_fail_if_default_group_already_has_permission_CODEVIEWER_and_adds_only_USER_to_default_group_of_default_organization_in_its_default_project_template()
    throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    IdAndUuid projectDefPermTemplate = insertPermissionTemplate(DEFAULT_ORGANIZATION_UUID);
    insertGroupPermission(projectDefPermTemplate, groupId, PERMISSION_CODEVIEWER);
    setupDefaultOrganization(groupId, projectDefPermTemplate.uuid, null);
    int otherGroupId = insertGroup(OTHER_ORGANIZATION_UUID);
    IdAndUuid otherProjectDefPermTemplateUuid = insertPermissionTemplate(OTHER_ORGANIZATION_UUID);
    insertOrganization(OTHER_ORGANIZATION_UUID, otherGroupId, otherProjectDefPermTemplateUuid.uuid, null);

    underTest.execute();

    verifyPermissionOfGroupInTemplate(projectDefPermTemplate, groupId, PERMISSION_USER, PERMISSION_CODEVIEWER);
    verifyPermissionOfGroupInTemplate(otherProjectDefPermTemplateUuid, otherGroupId);
  }

  @Test
  public void execute_is_reentrant()
    throws SQLException {
    int groupId = insertGroup(DEFAULT_ORGANIZATION_UUID);
    IdAndUuid projectDefPermTemplate = insertPermissionTemplate(DEFAULT_ORGANIZATION_UUID);
    setupDefaultOrganization(groupId, projectDefPermTemplate.uuid, null);
    int otherGroupId = insertGroup(OTHER_ORGANIZATION_UUID);
    IdAndUuid otherProjectDefPermTemplateUuid = insertPermissionTemplate(OTHER_ORGANIZATION_UUID);
    insertOrganization(OTHER_ORGANIZATION_UUID, otherGroupId, otherProjectDefPermTemplateUuid.uuid, null);

    underTest.execute();

    underTest.execute();

    verifyPermissionOfGroupInTemplate(projectDefPermTemplate, groupId, PERMISSION_USER, PERMISSION_CODEVIEWER);
    verifyPermissionOfGroupInTemplate(otherProjectDefPermTemplateUuid, otherGroupId);
  }

  private void insertGroupPermission(IdAndUuid permissionTemplate, int groupId, String permission) {
    db.executeInsert(
      "PERM_TEMPLATES_GROUPS",
      "GROUP_ID", groupId,
      "TEMPLATE_ID", permissionTemplate.id,
      "PERMISSION_REFERENCE", permission);
  }

  private void verifyPermissionOfGroupInTemplate(IdAndUuid permTemplate, int groupId, String... permissions) {
    verifyPermissionOfGroupInTemplate(permTemplate.uuid, groupId, permissions);
  }

  private void verifyPermissionOfGroupInTemplate(String permTemplateUuid, int groupId, String... permissions) {
    assertThat(
      db.select("select permission_reference as \"permission\" from perm_templates_groups ptg inner join permission_templates pt on pt.kee='" + permTemplateUuid
        + "' where ptg.template_id=pt.id and group_id=" + groupId)
        .stream()
        .flatMap(row -> Stream.of((String) row.get("permission")))
        .collect(MoreCollectors.toList()))
          .containsOnly(permissions);
  }

  private void setupDefaultOrganization(@Nullable Integer defaultGroupId, @Nullable String projectPermTemplateUuid, @Nullable String viewPermTemplateUuid) {
    setDefaultOrganizationProperty(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(DEFAULT_ORGANIZATION_UUID, defaultGroupId, projectPermTemplateUuid, viewPermTemplateUuid);
  }

  private void setDefaultOrganizationProperty(String defaultOrganizationUuid) {
    db.executeInsert(
      "INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", defaultOrganizationUuid);
  }

  private void insertOrganization(String uuid, @Nullable Integer defaultGroupId, @Nullable String projectPermTemplateUuid, @Nullable String viewPermTemplateUuid) {
    db.executeInsert("ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", false,
      "default_group_id", defaultGroupId == null ? null : valueOf(defaultGroupId),
      "default_perm_template_project", projectPermTemplateUuid,
      "default_perm_template_view", viewPermTemplateUuid,
      "CREATED_AT", nextLong(),
      "UPDATED_AT", nextLong());
  }

  private int insertGroup(String organizationUuid) {
    String name = "name" + randomAlphabetic(20);
    db.executeInsert(
      "GROUPS",
      "ORGANIZATION_UUID", organizationUuid,
      "NAME", name);

    return ((Long) db.selectFirst("select id as \"ID\" from groups where name='" + name + "'").get("ID")).intValue();
  }

  private IdAndUuid insertPermissionTemplate(String organizationUuid) {
    String random = RandomStringUtils.randomAlphanumeric(20);
    String uuid = "ptUuid" + random;
    db.executeInsert(
      "PERMISSION_TEMPLATES",
      "ORGANIZATION_UUID", organizationUuid,
      "NAME", "name" + random,
      "KEE", uuid);
    return new IdAndUuid(
      ((Long) db.selectFirst("select id as \"ID\" from permission_templates where kee='" + uuid + "'").get("ID")).intValue(),
      uuid);
  }

  private static final class IdAndUuid {
    private final int id;
    private final String uuid;

    private IdAndUuid(int id, String uuid) {
      this.id = id;
      this.uuid = uuid;
    }
  }
}
