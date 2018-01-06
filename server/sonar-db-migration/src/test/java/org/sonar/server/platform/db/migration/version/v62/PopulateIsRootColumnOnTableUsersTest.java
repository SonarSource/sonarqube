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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateIsRootColumnOnTableUsersTest {
  private static final String USERS_TABLE = "users";
  private static final String ROLE_ADMIN = "admin";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateIsRootColumnOnTableUsersTest.class, "users_and_permissions_tables.sql");

  private PopulateIsRootColumnOnTableUsers underTest = new PopulateIsRootColumnOnTableUsers(dbTester.database());

  @Test
  public void execute_on_empty_users_table_has_no_effect() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(USERS_TABLE)).isEqualTo(0);
  }

  @Test
  public void execute_sets_active_user_with_no_permission_has_not_root() throws SQLException {
    insertUser("foo", true);

    underTest.execute();

    verifySingleUser("foo", false);
  }

  @Test
  public void execute_sets_inactive_user_with_no_permission_has_not_root() throws SQLException {
    insertUser("foo", false);

    underTest.execute();

    verifySingleUser("foo", false);
  }

  @Test
  public void execute_sets_active_user_with_admin_role_has_root() throws SQLException {
    int userId = insertUser("foo", true);
    insertRole(userId, ROLE_ADMIN);

    underTest.execute();

    verifySingleUser("foo", true);
  }

  @Test
  public void execute_sets_inactive_user_with_admin_role_has_not_root() throws SQLException {
    int userId = insertUser("bar", false);
    insertRole(userId, ROLE_ADMIN);

    underTest.execute();

    verifySingleUser("bar", false);
  }

  @Test
  public void execute_sets_active_user_in_group_with_admin_role_has_root() throws SQLException {
    int userId = insertUser("doo", true);
    int groupId = insertGroup("admin group");
    insertGroupRole(groupId, ROLE_ADMIN);
    addUserToGroup(userId, groupId);

    underTest.execute();

    verifySingleUser("doo", true);
  }

  @Test
  public void execute_sets_inactive_user_in_group_with_admin_role_has_not_root() throws SQLException {
    int userId = insertUser("bar", false);
    int groupId = insertGroup("admin group");
    insertGroupRole(groupId, ROLE_ADMIN);
    addUserToGroup(userId, groupId);

    underTest.execute();

    verifySingleUser("bar", false);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    int adminGroupId = insertGroup("admin group");
    insertGroupRole(adminGroupId, ROLE_ADMIN);
    int groupId = insertGroup("other group");
    int[] userIds = {
      insertUser("inactive_direct_admin", false),
      insertUser("active_direct_admin", true),
      insertUser("inactive_group_admin", false),
      insertUser("active_group_admin", true),
      insertUser("group_and_direct_admin", true),
      insertUser("group_perm_user", true),
      insertUser("no_perm_user", true),
      insertUser("all_groups_user", true)
    };
    // inactive_direct_admin
    insertRole(userIds[0], ROLE_ADMIN);
    // active_direct_admin
    insertRole(userIds[1], ROLE_ADMIN);
    // inactive_group_admin
    addUserToGroup(userIds[2], adminGroupId);
    // active_group_admin
    addUserToGroup(userIds[3], adminGroupId);
    // group_and_direct_admin
    addUserToGroup(userIds[4], adminGroupId);
    insertRole(userIds[4], ROLE_ADMIN);
    // group_perm_user
    addUserToGroup(userIds[5], groupId);
    // all_groups_user
    addUserToGroup(userIds[7], adminGroupId);
    addUserToGroup(userIds[7], groupId);

    underTest.execute();
    verifyUsers();

    underTest.execute();
    verifyUsers();
  }

  private void verifyUsers() {
    verifyUser("inactive_direct_admin", false);
    verifyUser("active_direct_admin", true);
    verifyUser("inactive_group_admin", false);
    verifyUser("active_group_admin", true);
    verifyUser("group_and_direct_admin", true);
    verifyUser("group_perm_user", false);
    verifyUser("all_groups_user", true);
    verifyUser("no_perm_user", false);
  }

  private void insertRole(int userId, String role) {
    dbTester.executeInsert("user_roles", "user_id", userId, "role", role);
  }

  private int insertUser(String login, boolean active) {
    dbTester.executeInsert(USERS_TABLE, "login", login, "active", active);
    Long userId = (Long) dbTester.selectFirst("select id as \"id\" from users where login = '" + login + "'").get("id");
    return userId.intValue();
  }

  private int insertGroup(String groupName) {
    dbTester.executeInsert("groups", "name", groupName);
    Long groupId = (Long) dbTester.selectFirst("select id as \"id\" from groups where name = '" + groupName + "'").get("id");
    return groupId.intValue();
  }

  private void insertGroupRole(int groupId, String role) {
    dbTester.executeInsert("group_roles", "group_id", groupId, "role", role);
  }

  private void addUserToGroup(int userId, int groupId) {
    dbTester.executeInsert("groups_users", "user_id", userId, "group_id", groupId);
  }

  private void verifySingleUser(String login, boolean root) {
    List<Map<String, Object>> rows = dbTester.select("select login as \"login\", is_root as \"isRoot\" from users");
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.iterator().next();
    assertThat(row.get("login")).isEqualTo(login);
    assertThat(row.get("isRoot")).isEqualTo(root);
  }

  private void verifyUser(String login, boolean root) {
    Map<String, Object> row = dbTester.selectFirst("select login as \"login\", is_root as \"isRoot\" from users where login = '" + login + "'");
    assertThat(row.get("isRoot")).isEqualTo(root);
  }
}
