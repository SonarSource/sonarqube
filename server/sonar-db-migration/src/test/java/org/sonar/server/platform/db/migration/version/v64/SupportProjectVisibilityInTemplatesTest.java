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
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SupportProjectVisibilityInTemplatesTest {
  private static final Integer GROUP_ANYONE = null;
  private static final String PERMISSION_USER = "user";
  private static final String PERMISSION_CODEVIEWER = "codeviewer";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SupportProjectVisibilityInTemplatesTest.class, "permission_templates_and_groups.sql");

  private SupportProjectVisibilityInTemplates underTest = new SupportProjectVisibilityInTemplates(db.database());

  @Test
  public void execute_has_no_effect_if_tables_are_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_has_no_effect_if_templates_has_no_group() throws SQLException {
    insertPermissionTemplate("key");

    underTest.execute();

    assertUnchanged("key");
    assertNoGroup();
  }

  @Test
  public void execute_deletes_permission_USER_of_group_AnyOne_in_permission_template() throws SQLException {
    int ptId = insertPermissionTemplate("key");
    insertGroupPermission(ptId, GROUP_ANYONE, PERMISSION_USER);

    underTest.execute();

    assertUnchanged("key");
    assertNoGroup();
  }

  @Test
  public void execute_does_not_delete_permissions_different_from_USER_and_CODEVIEWER_for_group_AnyOne_in_permission_template() throws SQLException {
    int ptId = insertPermissionTemplate("key");
    insertGroupPermission(ptId, GROUP_ANYONE, "admin");
    insertGroupPermission(ptId, GROUP_ANYONE, "issueadmin");
    insertGroupPermission(ptId, GROUP_ANYONE, "scan");
    insertGroupPermission(ptId, GROUP_ANYONE, "foo");

    underTest.execute();

    assertUnchanged("key");
    assertHasGroupPermissions(ptId, GROUP_ANYONE, "admin", "issueadmin", "scan", "foo");
  }

  @Test
  public void execute_does_not_delete_any_permissions_from_other_group_in_permission_template() throws SQLException {
    int ptId = insertPermissionTemplate("key");
    insertGroupPermission(ptId, 12, PERMISSION_USER);
    insertGroupPermission(ptId, 12, PERMISSION_CODEVIEWER);
    insertGroupPermission(ptId, 12, "admin");
    insertGroupPermission(ptId, 12, "issueadmin");
    insertGroupPermission(ptId, 12, "scan");
    insertGroupPermission(ptId, 12, "bar");

    underTest.execute();

    assertUnchanged("key");
    assertHasGroupPermissions(ptId, 12, PERMISSION_CODEVIEWER, PERMISSION_USER, "admin", "issueadmin", "scan", "bar");
  }

  @Test
  public void execute_deletes_permission_CODEVIEWER_of_group_AnyOne_in_permission_template() throws SQLException {
    int ptId = insertPermissionTemplate("key");
    insertGroupPermission(ptId, GROUP_ANYONE, PERMISSION_CODEVIEWER);

    underTest.execute();

    assertUnchanged("key");
    assertNoGroup();
  }

  @Test
  public void execute_is_reentrant() throws SQLException {
    int ptId1 = insertPermissionTemplate("key1");
    insertGroupPermission(ptId1, GROUP_ANYONE, PERMISSION_USER);
    insertGroupPermission(ptId1, GROUP_ANYONE, PERMISSION_CODEVIEWER);
    insertGroupPermission(ptId1, 11, PERMISSION_USER);
    insertGroupPermission(ptId1, 12, "foo");
    insertGroupPermission(ptId1, 12, PERMISSION_CODEVIEWER);
    insertGroupPermission(ptId1, 12, "bar");
    int ptId2 = insertPermissionTemplate("key2");
    insertGroupPermission(ptId2, GROUP_ANYONE, PERMISSION_CODEVIEWER);
    insertGroupPermission(ptId2, GROUP_ANYONE, "moh");
    insertGroupPermission(ptId2, 50, PERMISSION_USER);
    insertGroupPermission(ptId2, 51, "admin");

    underTest.execute();

    verifyFor_execute_is_reentrant(ptId1, ptId2);

    underTest.execute();

    verifyFor_execute_is_reentrant(ptId1, ptId2);
  }

  private void verifyFor_execute_is_reentrant(int ptId1, int ptId2) {
    assertUnchanged("key1");
    assertHasGroupPermissions(ptId1, GROUP_ANYONE);
    assertHasGroupPermissions(ptId1, 11, PERMISSION_USER);
    assertHasGroupPermissions(ptId1, 12, PERMISSION_CODEVIEWER, "foo", "bar");
    assertUnchanged("key2");
    assertHasGroupPermissions(ptId2, GROUP_ANYONE, "moh");
    assertHasGroupPermissions(ptId2, 50, PERMISSION_USER);
    assertHasGroupPermissions(ptId2, 51, "admin");
  }

  private void insertGroupPermission(int templateId, @Nullable Integer groupId, String permission) {
    db.executeInsert(
      "perm_templates_groups",
      "group_id", groupId,
      "template_id", templateId,
      "permission_reference", permission);
  }

  private int insertPermissionTemplate(String key) {
    db.executeInsert(
      "permission_templates",
      "organization_uuid", "org_" + key,
      "name", "name_" + key,
      "kee", key);
    return ((Long) db.selectFirst("select id as \"ID\" from permission_templates where kee='" + key + "'").get("ID")).intValue();
  }

  private void assertUnchanged(String key) {
    Map<String, Object> row = db.selectFirst("select" +
      " organization_uuid as \"organizationUuid\"," +
      " name as \"name\"," +
      " description as \"desc\"," +
      " key_pattern \"pattern\"," +
      " created_at as \"createdAt\"," +
      " updated_at as \"updatedAt\"" +
      " from permission_templates where kee='" + key + "'");
    assertThat(row.get("organizationUuid")).isEqualTo("org_" + key);
    assertThat(row.get("name")).isEqualTo("name_" + key);
    assertThat(row.get("desc")).isNull();
    assertThat(row.get("pattern")).isNull();
    assertThat(row.get("createdAt")).isNull();
    assertThat(row.get("updatedAt")).isNull();
  }

  private void assertNoGroup() {
    assertThat(db.countRowsOfTable("perm_templates_groups")).isEqualTo(0);
  }

  private void assertHasGroupPermissions(int templateId, @Nullable Integer groupId, String... permissions) {
    assertThat(db.select("select permission_reference as \"perm\" from perm_templates_groups" +
      " where" +
      " template_id=" + templateId + "" +
      " and group_id " + (groupId == null ? " is null" : "=" + groupId)))
        .extracting(map -> (String) map.get("perm"))
        .containsOnly(permissions);
  }
}
