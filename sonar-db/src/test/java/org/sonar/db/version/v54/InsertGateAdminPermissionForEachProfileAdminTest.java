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
package org.sonar.db.version.v54;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class InsertGateAdminPermissionForEachProfileAdminTest {

  private static final String TABLE_GROUP_ROLES = "group_roles";
  private static final String TABLE_USER_ROLES = "user_roles";
  private static final int SOME_GROUP_ID = 964;
  private static final int SOME_USER_ID = 112;
  private static final Integer ANYONE_GROUP_ID = null;
  private static final int SOME_RESOURCE_ID = 25;
  private static final String PROFILEADMIN_ROLE = "profileadmin";
  private static final String NOT_PROFILE_ADMIN_ROLE = "ProfileAdmin";
  private static final String GATE_ADMIN_ROLE = "gateadmin";

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, InsertGateAdminPermissionForEachProfileAdminTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    truncate(TABLE_GROUP_ROLES);
    truncate(TABLE_USER_ROLES);

    migration = new InsertGateAdminPermissionForEachProfileAdmin(db.database());
  }

  @Test
  public void migrate_without_error_on_empty_db() throws Exception {
    migration.execute();

    assertGroupRoleTableSize(0);
    assertUserRoleTableSize(0);
  }

  @Test
  public void migrate_group_AnyOne() throws Exception {
    insertGroupRole(ANYONE_GROUP_ID, null, PROFILEADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(ANYONE_GROUP_ID, null, PROFILEADMIN_ROLE);
    assertGroupRoleContainsRow(ANYONE_GROUP_ID, null, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(2);
    assertUserRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_group_AnyOne_for_resource() throws Exception {
    insertGroupRole(ANYONE_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(ANYONE_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    assertGroupRoleDoesNotContainRow(ANYONE_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(1);
    assertUserRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_group_AnyOne_when_not_profileadmin() throws Exception {
    insertGroupRole(ANYONE_GROUP_ID, SOME_RESOURCE_ID, NOT_PROFILE_ADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(ANYONE_GROUP_ID, SOME_RESOURCE_ID, NOT_PROFILE_ADMIN_ROLE);
    assertGroupRoleDoesNotContainRow(ANYONE_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(1);
    assertUserRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_group_AnyOne_already_has_role_gateadmin() throws Exception {
    insertGroupRole(ANYONE_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    insertGroupRole(ANYONE_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(ANYONE_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    assertGroupRoleContainsRow(ANYONE_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(2);
    assertUserRoleTableSize(0);
  }

  @Test
  public void migrate_group() throws Exception {
    insertGroupRole(SOME_GROUP_ID, null, PROFILEADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(SOME_GROUP_ID, null, PROFILEADMIN_ROLE);
    assertGroupRoleContainsRow(SOME_GROUP_ID, null, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(2);
    assertUserRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_group_for_resource() throws Exception {
    insertGroupRole(SOME_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    assertGroupRoleDoesNotContainRow(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(1);
    assertUserRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_group_when_not_profileadmin() throws Exception {
    insertGroupRole(SOME_GROUP_ID, SOME_RESOURCE_ID, NOT_PROFILE_ADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, NOT_PROFILE_ADMIN_ROLE);
    assertGroupRoleDoesNotContainRow(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(1);
    assertUserRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_group_already_has_role_gateadmin() throws Exception {
    insertGroupRole(SOME_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    insertGroupRole(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);

    migration.execute();

    assertGroupRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    assertGroupRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertGroupRoleTableSize(2);
    assertUserRoleTableSize(0);
  }

  @Test
  public void migrate_user() throws Exception {
    insertUserRole(SOME_USER_ID, null, PROFILEADMIN_ROLE);

    migration.execute();

    assertUserRoleContainsRow(SOME_USER_ID, null, PROFILEADMIN_ROLE);
    assertUserRoleContainsRow(SOME_USER_ID, null, GATE_ADMIN_ROLE);
    assertUserRoleTableSize(2);
    assertGroupRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_user_for_resource() throws Exception {
    insertUserRole(SOME_USER_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);

    migration.execute();

    assertUserRoleContainsRow(SOME_USER_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    assertUserRoleDoesNotContainRow(SOME_USER_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertUserRoleTableSize(1);
    assertGroupRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_user_when_not_profileadmin() throws Exception {
    insertUserRole(SOME_GROUP_ID, SOME_RESOURCE_ID, NOT_PROFILE_ADMIN_ROLE);

    migration.execute();

    assertUserRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, NOT_PROFILE_ADMIN_ROLE);
    assertUserRoleDoesNotContainRow(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertUserRoleTableSize(1);
    assertGroupRoleTableSize(0);
  }

  @Test
  public void do_not_migrate_user_already_has_role_gateadmin() throws Exception {
    insertUserRole(SOME_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    insertUserRole(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);

    migration.execute();

    assertUserRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, PROFILEADMIN_ROLE);
    assertUserRoleContainsRow(SOME_GROUP_ID, SOME_RESOURCE_ID, GATE_ADMIN_ROLE);
    assertUserRoleTableSize(2);
    assertGroupRoleTableSize(0);
  }

  private void assertGroupRoleTableSize(int expected) {
    assertThat(db.countRowsOfTable(TABLE_GROUP_ROLES)).isEqualTo(expected);
  }

  private void assertUserRoleTableSize(int expected) {
    assertThat(db.countRowsOfTable(TABLE_USER_ROLES)).isEqualTo(expected);
  }

  private void assertGroupRoleDoesNotContainRow(@Nullable Integer groupId, @Nullable Integer resourceId, String role) {
    String sql = groupRoleRowSql(groupId, resourceId, role);
    assertThat(db.countSql(sql)).isEqualTo(0);
  }

  private void assertGroupRoleContainsRow(@Nullable Integer groupId, @Nullable Integer resourceId, String role) {
    String sql = groupRoleRowSql(groupId, resourceId, role);
    assertThat(db.countSql(sql)).isEqualTo(1);
  }

  private static String groupRoleRowSql(@Nullable Integer groupId, @Nullable Integer resourceId, String role) {
    return format(
        "select count(1) from group_roles where group_id %s and resource_id %s and role = '%s'",
        whereClauseFromInteger(groupId),
        whereClauseFromInteger(resourceId),
        role);
  }

  private void assertUserRoleDoesNotContainRow(@Nullable Integer groupId, @Nullable Integer resourceId, String role) {
    String sql = userRoleRowSql(groupId, resourceId, role);
    assertThat(db.countSql(sql)).isEqualTo(0);
  }

  private void assertUserRoleContainsRow(@Nullable Integer groupId, @Nullable Integer resourceId, String role) {
    String sql = userRoleRowSql(groupId, resourceId, role);
    assertThat(db.countSql(sql)).isEqualTo(1);
  }

  private static String userRoleRowSql(@Nullable Integer userId, @Nullable Integer resourceId, String role) {
    return format(
        "select count(1) from user_roles where user_id %s and resource_id %s and role = '%s'",
        whereClauseFromInteger(userId),
        whereClauseFromInteger(resourceId),
        role);
  }

  private void insertGroupRole(@Nullable Integer groupId, @Nullable Integer resourceId, String role) {
    db.executeUpdateSql(format("insert into group_roles (group_id,resource_id,role) values(%s,%s,'%s')", nullValueFromInteger(groupId), nullValueFromInteger(resourceId), role));
  }

  private void insertUserRole(@Nullable Integer userId, @Nullable Integer resourceId, String role) {
    db.executeUpdateSql(format("insert into user_roles (user_id,resource_id,role) values(%s,%s,'%s')", nullValueFromInteger(userId), nullValueFromInteger(resourceId), role));
  }

  private static String whereClauseFromInteger(@Nullable Integer id) {
    if (id == null) {
      return "is null";
    }
    return "=" + id;
  }

  private String nullValueFromInteger(@Nullable Integer value) {
    if (value == null) {
      return "null";
    }
    return String.valueOf(value);
  }

  private void truncate(String tableName) {
    db.executeUpdateSql("truncate table " + tableName);
  }

}
