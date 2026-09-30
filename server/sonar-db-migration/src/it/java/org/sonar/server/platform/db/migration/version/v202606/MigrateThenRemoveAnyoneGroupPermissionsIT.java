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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * End-to-end scenario test running {@link MigrateAnyoneGroupPermissionsToSonarUsers} then
 * {@link RemoveAnyoneGroupPermissions} back to back, in the exact order the real {@code DbVersion202606} registry
 * runs them, against a schema built through the real historical migration chain (not a hand-rolled fixture). Seeds
 * a mix of global, project-scoped, and template 'Anyone' permissions plus an unrelated real group's permissions,
 * and asserts the final state matches what both steps' individual unit tests claim in isolation: everything is
 * migrated to 'sonar-users', except a real group's own permissions, which are left untouched.
 */
class MigrateThenRemoveAnyoneGroupPermissionsIT {

  private static final String SONAR_USERS_UUID = "sonar-users-uuid";
  private static final String REAL_GROUP_UUID = "real-group-uuid";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateAnyoneGroupPermissionsToSonarUsers.class);

  @Test
  void execute_shouldMigrateGroupRolesAndTemplatePermissionsAndLeaveRealGroupsUntouched() throws SQLException {
    db.executeInsert("groups", "uuid", SONAR_USERS_UUID, "name", "sonar-users");

    // Anyone: global permissions, one new, one already granted to sonar-users
    insertGroupRole("anyone-global-new", null, null, "scan");
    insertGroupRole("anyone-global-dup", null, null, "provisioning");
    insertGroupRole("sonar-users-global-existing", SONAR_USERS_UUID, null, "provisioning");

    // Anyone: project-scoped permission, must end up on sonar-users too
    insertGroupRole("anyone-project", null, "entity-1", "issueadmin");

    // Anyone: template permission, must end up on sonar-users too
    insertPermTemplatesGroups("anyone-template", "template-1", null, "issueadmin");

    // Real group, untouched by both steps
    insertGroupRole("real-group-role", REAL_GROUP_UUID, "entity-1", "codeviewer");
    insertPermTemplatesGroups("real-group-template", "template-1", REAL_GROUP_UUID, "codeviewer");

    new MigrateAnyoneGroupPermissionsToSonarUsers(db.database()).execute();
    new RemoveAnyoneGroupPermissions(db.database()).execute();

    assertThat(select("SELECT group_uuid FROM group_roles WHERE group_uuid IS NULL"))
      .as("no 'Anyone' row survives the sequence, migrated or not")
      .isEmpty();
    assertThat(select("SELECT group_uuid FROM perm_templates_groups WHERE group_uuid IS NULL"))
      .as("no 'Anyone' row survives the sequence, migrated or not")
      .isEmpty();

    assertThat(select("SELECT entity_uuid, role FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .as("global and project-scoped permissions all land on sonar-users, no duplicate")
      .extracting(row -> row.get("ENTITY_UUID"), row -> row.get("ROLE"))
      .containsExactlyInAnyOrder(tuple(null, "scan"), tuple(null, "provisioning"), tuple("entity-1", "issueadmin"));

    assertThat(select("SELECT template_uuid, permission_reference FROM perm_templates_groups WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .as("the template permission also lands on sonar-users")
      .extracting(row -> row.get("TEMPLATE_UUID"), row -> row.get("PERMISSION_REFERENCE"))
      .containsExactly(tuple("template-1", "issueadmin"));

    assertThat(select("SELECT entity_uuid, role FROM group_roles WHERE group_uuid = '" + REAL_GROUP_UUID + "'"))
      .as("a real group's permissions are untouched by both steps")
      .extracting(row -> row.get("ENTITY_UUID"), row -> row.get("ROLE"))
      .containsExactly(tuple("entity-1", "codeviewer"));

    assertThat(select("SELECT template_uuid, permission_reference FROM perm_templates_groups WHERE group_uuid = '" + REAL_GROUP_UUID + "'"))
      .as("a real group's template permission is untouched by both steps")
      .extracting(row -> row.get("TEMPLATE_UUID"), row -> row.get("PERMISSION_REFERENCE"))
      .containsExactly(tuple("template-1", "codeviewer"));
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
