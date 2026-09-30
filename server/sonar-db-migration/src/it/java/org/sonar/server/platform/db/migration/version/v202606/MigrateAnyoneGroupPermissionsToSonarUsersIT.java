/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class MigrateAnyoneGroupPermissionsToSonarUsersIT {

  private static final String SONAR_USERS_UUID = "sonar-users-uuid";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateAnyoneGroupPermissionsToSonarUsers.class);

  private final DataChange underTest = new MigrateAnyoneGroupPermissionsToSonarUsers(db.database());

  @Test
  void execute_shouldCopyGlobalAnyonePermissionToSonarUsersWhenNoEquivalentExists() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("anyone-global", null, null, "scan");

    underTest.execute();

    assertThat(select("SELECT group_uuid FROM group_roles WHERE role = 'scan' AND group_uuid IS NULL"))
      .as("Anyone row is left untouched by this step, it is deleted by the follow-up step")
      .hasSize(1);
    assertThat(select("SELECT entity_uuid, role FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("ENTITY_UUID"), row -> row.get("ROLE"))
      .containsExactly(tuple(null, "scan"));
  }

  @Test
  void execute_shouldCopyProjectScopedAnyonePermissionToSonarUsersWhenNoEquivalentExists() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("anyone-project", null, "entity-1", "issueadmin");

    underTest.execute();

    assertThat(select("SELECT group_uuid FROM group_roles WHERE role = 'issueadmin' AND group_uuid IS NULL"))
      .as("Anyone row is left untouched by this step, it is deleted by the follow-up step")
      .hasSize(1);
    assertThat(select("SELECT entity_uuid, role FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("ENTITY_UUID"), row -> row.get("ROLE"))
      .containsExactly(tuple("entity-1", "issueadmin"));
  }

  @Test
  void execute_shouldCopyTemplatePermissionToSonarUsersWhenNoEquivalentExists() throws SQLException {
    insertSonarUsersGroup();
    insertPermTemplatesGroups("anyone-perm", "template-1", null, "issueadmin");

    underTest.execute();

    assertThat(select("SELECT group_uuid FROM perm_templates_groups WHERE group_uuid IS NULL"))
      .as("Anyone row is left untouched by this step, it is deleted by the follow-up step")
      .hasSize(1);
    assertThat(select("SELECT template_uuid, permission_reference FROM perm_templates_groups WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("TEMPLATE_UUID"), row -> row.get("PERMISSION_REFERENCE"))
      .containsExactly(tuple("template-1", "issueadmin"));
  }

  @Test
  void execute_shouldNotDuplicateGlobalPermissionWhenSonarUsersAlreadyHasIt() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("anyone-global", null, null, "scan");
    insertGroupRole("existing-global", SONAR_USERS_UUID, null, "scan");

    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("existing-global");
  }

  @Test
  void execute_shouldNotDuplicateProjectScopedPermissionWhenSonarUsersAlreadyHasIt() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("anyone-project", null, "entity-1", "issueadmin");
    insertGroupRole("existing-project", SONAR_USERS_UUID, "entity-1", "issueadmin");

    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("existing-project");
  }

  @Test
  void execute_shouldNotDuplicateTemplatePermissionWhenSonarUsersAlreadyHasIt() throws SQLException {
    insertSonarUsersGroup();
    insertPermTemplatesGroups("anyone-perm", "template-1", null, "issueadmin");
    insertPermTemplatesGroups("existing-perm", "template-1", SONAR_USERS_UUID, "issueadmin");

    underTest.execute();

    assertThat(select("SELECT uuid FROM perm_templates_groups WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("existing-perm");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("anyone-global", null, null, "scan");
    insertGroupRole("anyone-project", null, "entity-1", "issueadmin");
    insertPermTemplatesGroups("anyone-perm", "template-1", null, "issueadmin");

    underTest.execute();
    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'")).hasSize(2);
    assertThat(select("SELECT uuid FROM perm_templates_groups WHERE group_uuid = '" + SONAR_USERS_UUID + "'")).hasSize(1);
  }

  @Test
  void execute_shouldNotTouchRealGroupRoles() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("group-role", "some-group-uuid", "entity-1", "issueadmin");

    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("group-role");
  }

  @Test
  void execute_shouldNotTouchRealTemplateGroups() throws SQLException {
    insertSonarUsersGroup();
    insertPermTemplatesGroups("group-perm", "template-1", "some-group-uuid", "user");

    underTest.execute();

    assertThat(select("SELECT uuid FROM perm_templates_groups"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("group-perm");
  }

  @Test
  void execute_shouldNoOpWhenSonarUsersGroupDoesNotExist() throws SQLException {
    insertGroupRole("anyone-role", null, null, "scan");
    insertPermTemplatesGroups("anyone-perm", "template-1", null, "issueadmin");

    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles")).hasSize(1);
    assertThat(select("SELECT uuid FROM perm_templates_groups")).hasSize(1);
  }

  private void insertSonarUsersGroup() {
    db.executeInsert("groups",
      "uuid", SONAR_USERS_UUID,
      "name", "sonar-users");
  }

  private void insertGroupRole(String uuid, String groupUuid, String entityUuid, String role) {
    db.executeInsert("group_roles",
      "uuid", uuid,
      "group_uuid", groupUuid,
      "entity_uuid", entityUuid,
      "role", role);
  }

  private void insertPermTemplatesGroups(String uuid, String templateUuid, String groupUuid, String permission) {
    db.executeInsert("perm_templates_groups",
      "uuid", uuid,
      "template_uuid", templateUuid,
      "group_uuid", groupUuid,
      "permission_reference", permission);
  }

  private List<Map<String, Object>> select(String sql) {
    return db.select(sql);
  }
}
