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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class AddArchitectureAdminPermissionToProjectAdminsTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddArchitectureAdminPermissionToProjectAdmins.class);

  private final AddArchitectureAdminPermissionToProjectAdmins underTest = new AddArchitectureAdminPermissionToProjectAdmins(db.database());

  @Test
  void execute_grants_architectureadmin_to_user_with_project_admin() throws SQLException {
    String userUuid = "user-1";
    String entityUuid = "project-1";
    insertProject(entityUuid, "TRK");
    insertUserRole(userUuid, entityUuid, "admin");

    underTest.execute();

    assertThat(selectUserRoles(userUuid, entityUuid)).containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_grants_architectureadmin_to_group_with_project_admin() throws SQLException {
    String groupUuid = "group-1";
    String entityUuid = "project-1";
    insertProject(entityUuid, "TRK");
    insertGroupRole(groupUuid, entityUuid, "admin");

    underTest.execute();

    assertThat(selectGroupRoles(groupUuid, entityUuid)).containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_to_user_with_portfolio_admin() throws SQLException {
    String userUuid = "user-1";
    String portfolioUuid = "portfolio-1";
    insertUserRole(userUuid, portfolioUuid, "admin");

    underTest.execute();

    assertThat(selectUserRoles(userUuid, portfolioUuid)).containsExactly("admin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_to_group_with_portfolio_admin() throws SQLException {
    String groupUuid = "group-1";
    String portfolioUuid = "portfolio-1";
    insertGroupRole(groupUuid, portfolioUuid, "admin");

    underTest.execute();

    assertThat(selectGroupRoles(groupUuid, portfolioUuid)).containsExactly("admin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_to_user_with_app_admin() throws SQLException {
    String userUuid = "user-1";
    String appUuid = "app-1";
    insertProject(appUuid, "APP");
    insertUserRole(userUuid, appUuid, "admin");

    underTest.execute();

    assertThat(selectUserRoles(userUuid, appUuid)).containsExactly("admin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_to_group_with_app_admin() throws SQLException {
    String groupUuid = "group-1";
    String appUuid = "app-1";
    insertProject(appUuid, "APP");
    insertGroupRole(groupUuid, appUuid, "admin");

    underTest.execute();

    assertThat(selectGroupRoles(groupUuid, appUuid)).containsExactly("admin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_for_global_admin() throws SQLException {
    String userUuid = "user-1";
    insertUserRole(userUuid, null, "admin");

    underTest.execute();

    assertThat(selectUserRoles(userUuid, null)).containsExactly("admin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_for_global_group_admin() throws SQLException {
    String groupUuid = "group-1";
    insertGroupRole(groupUuid, null, "admin");

    underTest.execute();

    assertThat(selectGroupRoles(groupUuid, null)).containsExactly("admin");
  }

  @Test
  void execute_does_not_grant_architectureadmin_for_unrelated_permission() throws SQLException {
    String userUuid = "user-1";
    String entityUuid = "project-1";
    insertProject(entityUuid, "TRK");
    insertUserRole(userUuid, entityUuid, "user");

    underTest.execute();

    assertThat(selectUserRoles(userUuid, entityUuid)).containsExactly("user");
  }

  @Test
  void execute_is_idempotent() throws SQLException {
    String userUuid = "user-1";
    String entityUuid = "project-1";
    insertProject(entityUuid, "TRK");
    insertUserRole(userUuid, entityUuid, "admin");

    underTest.execute();
    underTest.execute();

    assertThat(selectUserRoles(userUuid, entityUuid)).containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_skips_user_already_having_architectureadmin() throws SQLException {
    String userUuid = "user-1";
    String entityUuid = "project-1";
    insertProject(entityUuid, "TRK");
    insertUserRole(userUuid, entityUuid, "admin");
    insertUserRole(userUuid, entityUuid, "architectureadmin");

    underTest.execute();

    assertThat(selectUserRoles(userUuid, entityUuid)).containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_updates_all_templates_groups_with_admin() throws SQLException {
    String defaultTemplateUuid = "template-1";
    String otherTemplateUuid = "template-2";
    String groupUuid = "group-uuid";
    insertTemplateGroupPermission(defaultTemplateUuid, groupUuid, "admin");
    insertTemplateGroupPermission(otherTemplateUuid, groupUuid, "admin");

    underTest.execute();

    assertThat(selectTemplateGroupPermissions(defaultTemplateUuid, groupUuid))
      .containsExactlyInAnyOrder("admin", "architectureadmin");
    assertThat(selectTemplateGroupPermissions(otherTemplateUuid, groupUuid))
      .containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_updates_all_templates_users_with_admin() throws SQLException {
    String defaultTemplateUuid = "template-1";
    String otherTemplateUuid = "template-2";
    String userUuid = "user-uuid";
    insertTemplateUserPermission(defaultTemplateUuid, userUuid, "admin");
    insertTemplateUserPermission(otherTemplateUuid, userUuid, "admin");

    underTest.execute();

    assertThat(selectTemplateUserPermissions(defaultTemplateUuid, userUuid))
      .containsExactlyInAnyOrder("admin", "architectureadmin");
    assertThat(selectTemplateUserPermissions(otherTemplateUuid, userUuid))
      .containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_skips_template_group_update_when_no_group_has_admin_permission() throws SQLException {
    String templateUuid = "template-1";
    String groupUuid = "group-uuid";
    insertTemplateGroupPermission(templateUuid, groupUuid, "user");

    underTest.execute();

    assertThat(selectTemplateGroupPermissions(templateUuid, groupUuid)).containsExactly("user");
  }

  @Test
  void execute_skips_template_user_update_when_no_user_has_admin_permission() throws SQLException {
    String templateUuid = "template-1";
    String userUuid = "user-uuid";
    insertTemplateUserPermission(templateUuid, userUuid, "user");

    underTest.execute();

    assertThat(selectTemplateUserPermissions(templateUuid, userUuid)).containsExactly("user");
  }

  @Test
  void execute_is_idempotent_for_template_groups() throws SQLException {
    String templateUuid = "template-1";
    String groupUuid = "group-uuid";
    insertTemplateGroupPermission(templateUuid, groupUuid, "admin");

    underTest.execute();
    underTest.execute();

    assertThat(selectTemplateGroupPermissions(templateUuid, groupUuid))
      .containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  @Test
  void execute_is_idempotent_for_template_users() throws SQLException {
    String templateUuid = "template-1";
    String userUuid = "user-uuid";
    insertTemplateUserPermission(templateUuid, userUuid, "admin");

    underTest.execute();
    underTest.execute();

    assertThat(selectTemplateUserPermissions(templateUuid, userUuid))
      .containsExactlyInAnyOrder("admin", "architectureadmin");
  }

  private void insertUserRole(String userUuid, String entityUuid, String role) {
    db.executeInsert("user_roles",
      "uuid", UUID.randomUUID().toString(),
      "user_uuid", userUuid,
      "entity_uuid", entityUuid,
      "role", role);
  }

  private void insertGroupRole(String groupUuid, String entityUuid, String role) {
    db.executeInsert("group_roles",
      "uuid", UUID.randomUUID().toString(),
      "group_uuid", groupUuid,
      "entity_uuid", entityUuid,
      "role", role);
  }

  private void insertProject(String uuid, String qualifier) {
    db.executeInsert("projects",
      "uuid", uuid,
      "kee", uuid,
      "qualifier", qualifier,
      "private", true,
      "creation_method", "LOCAL_API",
      "contains_ai_code", false,
      "detected_ai_code", false,
      "ai_code_fix_enabled", false,
      "created_at", 1_000_000L,
      "updated_at", 1_000_000L);
  }

  private void insertTemplateGroupPermission(String templateUuid, String groupUuid, String permission) {
    db.executeInsert("perm_templates_groups",
      "uuid", UUID.randomUUID().toString(),
      "template_uuid", templateUuid,
      "group_uuid", groupUuid,
      "permission_reference", permission,
      "created_at", new Timestamp(1_000_000L),
      "updated_at", new Timestamp(1_000_000L));
  }

  private void insertTemplateUserPermission(String templateUuid, String userUuid, String permission) {
    db.executeInsert("perm_templates_users",
      "uuid", UUID.randomUUID().toString(),
      "template_uuid", templateUuid,
      "user_uuid", userUuid,
      "permission_reference", permission,
      "created_at", new Timestamp(1_000_000L),
      "updated_at", new Timestamp(1_000_000L));
  }

  private List<String> selectUserRoles(String userUuid, String entityUuid) {
    String condition = entityUuid == null
      ? "entity_uuid IS NULL"
      : "entity_uuid = '" + entityUuid + "'";
    return db.select("SELECT role FROM user_roles WHERE user_uuid = '" + userUuid + "' AND " + condition)
      .stream()
      .map(row -> (String) row.get("ROLE"))
      .toList();
  }

  private List<String> selectGroupRoles(String groupUuid, String entityUuid) {
    String condition = entityUuid == null
      ? "entity_uuid IS NULL"
      : "entity_uuid = '" + entityUuid + "'";
    return db.select("SELECT role FROM group_roles WHERE group_uuid = '" + groupUuid + "' AND " + condition)
      .stream()
      .map(row -> (String) row.get("ROLE"))
      .toList();
  }

  private List<String> selectTemplateGroupPermissions(String templateUuid, String groupUuid) {
    return db.select("SELECT permission_reference FROM perm_templates_groups WHERE template_uuid = '" + templateUuid + "' AND group_uuid = '" + groupUuid + "'")
      .stream()
      .map(row -> (String) row.get("PERMISSION_REFERENCE"))
      .toList();
  }

  private List<String> selectTemplateUserPermissions(String templateUuid, String userUuid) {
    return db.select("SELECT permission_reference FROM perm_templates_users WHERE template_uuid = '" + templateUuid + "' AND user_uuid = '" + userUuid + "'")
      .stream()
      .map(row -> (String) row.get("PERMISSION_REFERENCE"))
      .toList();
  }

}
