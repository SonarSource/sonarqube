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
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class MigrateAnyoneGroupPermissionsToSonarUsersIT {

  private static final String SONAR_USERS_UUID = "sonar-users-uuid";

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

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
  void execute_shouldNotMigrateAnyoneTemplatePermissions() throws SQLException {
    insertSonarUsersGroup();
    insertPermTemplatesGroups("anyone-user", "template-1", null, "user");
    insertPermTemplatesGroups("anyone-codeviewer", "template-1", null, "codeviewer");
    insertPermTemplatesGroups("anyone-issueadmin", "template-1", null, "issueadmin");
    insertPermTemplatesGroups("anyone-scan", "template-1", null, "scan");

    underTest.execute();

    assertThat(select("SELECT uuid FROM perm_templates_groups WHERE group_uuid = '" + SONAR_USERS_UUID + "'")).isEmpty();
    assertThat(select("SELECT uuid FROM perm_templates_groups WHERE group_uuid IS NULL"))
      .as("Anyone rows are left untouched by this step, they are deleted by the follow-up step")
      .hasSize(4);
    assertThat(logTester.logs(Level.WARN)).containsExactlyInAnyOrder(
      "Dropped 'Anyone' permission 'user' on permission template 'template-1', it is not migrated to 'sonar-users'",
      "Dropped 'Anyone' permission 'codeviewer' on permission template 'template-1', it is not migrated to 'sonar-users'",
      "Dropped 'Anyone' permission 'issueadmin' on permission template 'template-1', it is not migrated to 'sonar-users'",
      "Dropped 'Anyone' permission 'scan' on permission template 'template-1', it is not migrated to 'sonar-users'");
  }

  @Test
  void execute_shouldNotMigratePublicPermissionsOnPublicProject() throws SQLException {
    insertSonarUsersGroup();
    insertProject("project-1", false);
    insertGroupRole("anyone-user", null, "project-1", "user");
    insertGroupRole("anyone-codeviewer", null, "project-1", "codeviewer");
    insertGroupRole("anyone-issueadmin", null, "project-1", "issueadmin");

    underTest.execute();

    assertThat(select("SELECT entity_uuid, role FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("ENTITY_UUID"), row -> row.get("ROLE"))
      .containsExactly(tuple("project-1", "issueadmin"));
  }

  @Test
  void execute_shouldNotMigratePublicPermissionsOnPublicPortfolio() throws SQLException {
    insertSonarUsersGroup();
    insertPortfolio("portfolio-1", false);
    insertGroupRole("anyone-codeviewer", null, "portfolio-1", "codeviewer");

    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'")).isEmpty();
  }

  @Test
  void execute_shouldMigratePublicPermissionsOnPrivateProject() throws SQLException {
    insertSonarUsersGroup();
    insertProject("project-1", true);
    insertGroupRole("anyone-user", null, "project-1", "user");

    underTest.execute();

    assertThat(select("SELECT entity_uuid, role FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'"))
      .extracting(row -> row.get("ENTITY_UUID"), row -> row.get("ROLE"))
      .containsExactly(tuple("project-1", "user"));
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
  void execute_shouldBeReentrant() throws SQLException {
    insertSonarUsersGroup();
    insertGroupRole("anyone-global", null, null, "scan");
    insertGroupRole("anyone-project", null, "entity-1", "issueadmin");

    underTest.execute();
    underTest.execute();

    assertThat(select("SELECT uuid FROM group_roles WHERE group_uuid = '" + SONAR_USERS_UUID + "'")).hasSize(2);
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

  private void insertProject(String uuid, boolean isPrivate) {
    db.executeInsert("projects",
      "uuid", uuid,
      "kee", uuid,
      "qualifier", "TRK",
      "private", isPrivate,
      "updated_at", 1L,
      "creation_method", "LOCAL_API");
  }

  private void insertPortfolio(String uuid, boolean isPrivate) {
    db.executeInsert("portfolios",
      "uuid", uuid,
      "kee", uuid,
      "name", uuid,
      "root_uuid", uuid,
      "private", isPrivate,
      "selection_mode", "NONE",
      "created_at", 1L,
      "updated_at", 1L);
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
