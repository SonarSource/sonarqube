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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class AddArchitectureAdminPermissionToAdminsGroupTest {

  private static final String ADMINS_GROUP = "sonar-administrators";
  private static final String ADMIN_PERMISSION = "admin";
  private static final String ARCHITECTURE_ADMIN_PERMISSION = "architectureadmin";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddArchitectureAdminPermissionToAdminsGroup.class);

  private final AddArchitectureAdminPermissionToAdminsGroup underTest = new AddArchitectureAdminPermissionToAdminsGroup(db.database());

  @Test
  void execute_grants_architectureadmin_to_admins_group_still_holding_admin() throws SQLException {
    String groupUuid = insertGroup(ADMINS_GROUP);
    insertGroupRole(groupUuid, ADMIN_PERMISSION);

    underTest.execute();

    assertThat(selectGlobalRoles(groupUuid)).containsExactlyInAnyOrder(ADMIN_PERMISSION, ARCHITECTURE_ADMIN_PERMISSION);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  void execute_does_nothing_when_admins_group_does_not_exist() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("group_roles")).isZero();
  }

  @Test
  void execute_does_not_grant_architectureadmin_when_admins_group_exists_without_any_role() throws SQLException {
    String groupUuid = insertGroup(ADMINS_GROUP);

    underTest.execute();

    assertThat(selectGlobalRoles(groupUuid)).isEmpty();
    assertThat(db.countRowsOfTable("group_roles")).isZero();
  }

  @Test
  void execute_does_not_grant_architectureadmin_when_admins_group_only_has_unrelated_permission() throws SQLException {
    String groupUuid = insertGroup(ADMINS_GROUP);
    insertGroupRole(groupUuid, "profileadmin");

    underTest.execute();

    assertThat(selectGlobalRoles(groupUuid)).containsExactly("profileadmin");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(1);
  }

  @Test
  void execute_does_not_grant_architectureadmin_to_other_groups() throws SQLException {
    String otherGroupUuid = insertGroup("some-other-group");
    insertGroupRole(otherGroupUuid, ADMIN_PERMISSION);

    underTest.execute();

    assertThat(selectGlobalRoles(otherGroupUuid)).containsExactly(ADMIN_PERMISSION);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(1);
  }

  @Test
  void execute_is_idempotent() throws SQLException {
    String groupUuid = insertGroup(ADMINS_GROUP);
    insertGroupRole(groupUuid, ADMIN_PERMISSION);

    underTest.execute();
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);

    underTest.execute();

    assertThat(selectGlobalRoles(groupUuid)).containsExactlyInAnyOrder(ADMIN_PERMISSION, ARCHITECTURE_ADMIN_PERMISSION);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  void execute_skips_admins_group_already_having_architectureadmin() throws SQLException {
    String groupUuid = insertGroup(ADMINS_GROUP);
    insertGroupRole(groupUuid, ADMIN_PERMISSION);
    insertGroupRole(groupUuid, ARCHITECTURE_ADMIN_PERMISSION);

    underTest.execute();

    assertThat(selectGlobalRoles(groupUuid)).containsExactlyInAnyOrder(ADMIN_PERMISSION, ARCHITECTURE_ADMIN_PERMISSION);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  private String insertGroup(String name) {
    String groupUuid = "group-" + name;
    db.executeInsert("groups",
      "uuid", groupUuid,
      "name", name);
    return groupUuid;
  }

  private void insertGroupRole(String groupUuid, String role) {
    db.executeInsert("group_roles",
      "uuid", "gr-" + role,
      "group_uuid", groupUuid,
      "entity_uuid", null,
      "role", role);
  }

  private List<String> selectGlobalRoles(String groupUuid) {
    return db.select("select role from group_roles where group_uuid = '" + groupUuid + "' and entity_uuid is null")
      .stream()
      .map(row -> (String) row.get("ROLE"))
      .toList();
  }
}
