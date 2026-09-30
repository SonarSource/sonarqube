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

class RemoveAnyoneGroupPermissionsIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveAnyoneGroupPermissions.class);

  private final DataChange underTest = new RemoveAnyoneGroupPermissions(db.database());

  @Test
  void execute_shouldDeleteOnlyAnyoneGroupRowsFromGroupRoles() throws SQLException {
    insertGroupRole("anyone-role", null);
    insertGroupRole("group-role", "group-uuid");

    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("group-role");
  }

  @Test
  void execute_shouldDeleteOnlyAnyoneGroupRowsFromPermTemplatesGroups() throws SQLException {
    insertPermTemplatesGroups("anyone-perm", null);
    insertPermTemplatesGroups("group-perm", "group-uuid");

    underTest.execute();

    assertThat(select("SELECT uuid FROM perm_templates_groups"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("group-perm");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    insertGroupRole("anyone-role", null);
    insertPermTemplatesGroups("anyone-perm", null);

    underTest.execute();
    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles")).isEmpty();
    assertThat(select("SELECT uuid FROM perm_templates_groups")).isEmpty();
  }

  private void insertGroupRole(String uuid, String groupUuid) {
    db.executeInsert("group_roles",
      "uuid", uuid,
      "group_uuid", groupUuid,
      "role", "user");
  }

  private void insertPermTemplatesGroups(String uuid, String groupUuid) {
    db.executeInsert("perm_templates_groups",
      "uuid", uuid,
      "template_uuid", "template",
      "group_uuid", groupUuid,
      "permission_reference", "user");
  }

  private List<Map<String, Object>> select(String sql) {
    return db.select(sql);
  }
}
