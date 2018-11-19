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

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateOrganizationMembersTableTest {

  private static final String TABLE = "organization_members";

  private static final String DEFAULT_ORGANIZATION_UUID = "def org uuid";

  private static final String PERMISSION_PROVISIONING = "provisioning";
  private static final String PERMISSION_ADMIN = "admin";
  private static final String PERMISSION_BROWSE = "user";
  private static final String PERMISSION_CODEVIEWER = "codeviewer";

  private static final String ORG1_UUID = "ORG1_UUID";
  private static final String ORG2_UUID = "ORG2_UUID";

  private static final String USER1_LOGIN = "USER1";
  private static final String USER2_LOGIN = "USER2";
  private static final String USER3_LOGIN = "USER3";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateOrganizationMembersTableTest.class, "initial.sql");

  private PopulateOrganizationMembersTable underTest = new PopulateOrganizationMembersTable(db.database(), new DefaultOrganizationUuidProviderImpl());

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
  public void execute_has_no_effect_when_table_is_empty() throws SQLException {
    setupDefaultOrganization();

    underTest.execute();
  }

  @Test
  public void execute_is_reentrant_when_table_is_empty() throws SQLException {
    setupDefaultOrganization();

    underTest.execute();
    underTest.execute();
  }

  @Test
  public void migrate_user_having_direct_global_permissions() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    insertOrganization(ORG2_UUID);
    int userId = insertUser(USER1_LOGIN);
    insertUserRole(userId, PERMISSION_PROVISIONING, ORG1_UUID, null);
    insertUserRole(userId, PERMISSION_ADMIN, ORG1_UUID, null);
    insertUserRole(userId, PERMISSION_ADMIN, ORG2_UUID, null);

    underTest.execute();

    verifyUserMembership(userId, ORG1_UUID, ORG2_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migrate_user_having_direct_project_permissions() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    insertOrganization(ORG2_UUID);
    int userId = insertUser(USER1_LOGIN);
    insertUserRole(userId, PERMISSION_BROWSE, ORG1_UUID, 1);
    insertUserRole(userId, PERMISSION_CODEVIEWER, ORG1_UUID, 1);
    insertUserRole(userId, PERMISSION_ADMIN, ORG2_UUID, 2);

    underTest.execute();

    verifyUserMembership(userId, ORG1_UUID, ORG2_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migrate_user_having_global_permissions_from_group() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    insertOrganization(ORG2_UUID);
    int userId = insertUser(USER1_LOGIN);
    int group1Id = insertNewGroup(ORG1_UUID);
    int group2Id = insertNewGroup(ORG2_UUID);
    insertUserGroup(userId, group1Id);
    insertUserGroup(userId, group2Id);

    underTest.execute();

    verifyUserMembership(userId, ORG1_UUID, ORG2_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void user_without_any_permission_should_be_member_of_default_organization() throws Exception {
    setupDefaultOrganization();
    int userId = insertUser(USER1_LOGIN);

    underTest.execute();

    verifyUserMembership(userId, DEFAULT_ORGANIZATION_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migrate_users_having_any_kind_of_permission() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    insertOrganization(ORG2_UUID);
    int user1 = insertUser(USER1_LOGIN);
    int user2 = insertUser(USER2_LOGIN);
    int user3 = insertUser(USER3_LOGIN);
    int groupId = insertNewGroup(ORG1_UUID);
    insertUserGroup(user2, groupId);
    insertUserRole(user1, PERMISSION_PROVISIONING, ORG1_UUID, null);
    insertUserRole(user1, PERMISSION_BROWSE, ORG2_UUID, 1);

    underTest.execute();

    verifyUserMembership(user1, ORG1_UUID, ORG2_UUID, DEFAULT_ORGANIZATION_UUID);
    verifyUserMembership(user2, ORG1_UUID, DEFAULT_ORGANIZATION_UUID);
    verifyUserMembership(user3, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migrate_missing_membership_on_direct_permission() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    insertOrganization(ORG2_UUID);
    int userId = insertUser(USER1_LOGIN);
    insertUserRole(userId, PERMISSION_ADMIN, ORG1_UUID, null);
    insertUserRole(userId, PERMISSION_PROVISIONING, ORG2_UUID, null);
    // Membership on organization 1 already exists, migration will add membership on organization 2 and default organization
    insertOrganizationMember(userId, ORG1_UUID);

    underTest.execute();

    verifyUserMembership(userId, ORG1_UUID, ORG2_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migrate_missing_membership_on_group_permission() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    insertOrganization(ORG2_UUID);
    int userId = insertUser(USER1_LOGIN);
    int group1Id = insertNewGroup(ORG1_UUID);
    int group2Id = insertNewGroup(ORG2_UUID);
    insertUserGroup(userId, group1Id);
    insertUserGroup(userId, group2Id);
    // Membership on organization 1 already exists, migration will add membership on organization 2 and default organization
    insertOrganizationMember(userId, ORG1_UUID);

    underTest.execute();

    verifyUserMembership(userId, ORG1_UUID, ORG2_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migrate_active_users_to_default_organization() throws Exception {
    setupDefaultOrganization();
    int user1Id = insertUser(USER1_LOGIN, false);
    int user2Id = insertUser(USER2_LOGIN, false);
    int user3Id = insertUser(USER3_LOGIN, false);
    int group1Id = insertNewGroup(ORG1_UUID);
    insertUserRole(user1Id, PERMISSION_ADMIN, ORG1_UUID, null);
    insertUserGroup(user2Id, group1Id);

    underTest.execute();

    verifyUserMembership(user1Id);
    verifyUserMembership(user2Id);
    verifyUserMembership(user3Id);
  }

  @Test
  public void ignore_already_associated_users() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    int userId = insertUser(USER1_LOGIN);
    insertUserRole(userId, PERMISSION_PROVISIONING, ORG1_UUID, null);
    // User is already associated to organization 1 and to default organization, it should not fail
    insertOrganizationMember(userId, ORG1_UUID);
    insertOrganizationMember(userId, DEFAULT_ORGANIZATION_UUID);

    underTest.execute();

    verifyUserMembership(userId, ORG1_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    setupDefaultOrganization();
    insertOrganization(ORG1_UUID);
    int userId = insertUser(USER1_LOGIN);
    insertUserRole(userId, PERMISSION_PROVISIONING, ORG1_UUID, null);
    verifyUserMembership(userId);

    underTest.execute();
    verifyUserMembership(userId, ORG1_UUID, DEFAULT_ORGANIZATION_UUID);

    underTest.execute();
    verifyUserMembership(userId, ORG1_UUID, DEFAULT_ORGANIZATION_UUID);
  }

  private void insertOrganizationMember(int userId, String organizationUuid) {
    db.executeInsert(TABLE, "USER_ID", userId, "ORGANIZATION_UUID", organizationUuid);
  }

  private void insertOrganization(String uuid) {
    db.executeInsert("ORGANIZATIONS", "UUID", uuid, "KEE", uuid, "NAME", uuid, "GUARDED", false, "CREATED_AT", nextLong(), "UPDATED_AT", nextLong());
  }

  private int insertUser(String login) {
    return insertUser(login, true);
  }

  private int insertUser(String login, boolean enabled) {
    db.executeInsert("USERS", "LOGIN", login, "NAME", login, "ACTIVE", enabled, "IS_ROOT", false);
    return ((Long) db.selectFirst(format("select ID from users where login='%s'", login)).get("ID")).intValue();
  }

  private void insertUserRole(int userId, String permission, String organizationUuid, @Nullable Integer componentId) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
      .putAll(ImmutableMap.of("USER_ID", userId, "ROLE", permission, "ORGANIZATION_UUID", organizationUuid));
    Optional.ofNullable(componentId).ifPresent(id -> builder.put("RESOURCE_ID", id));
    db.executeInsert("USER_ROLES", builder.build());
  }

  private int insertNewGroup(String organizationUuid) {
    String groupName = randomAlphabetic(10);
    db.executeInsert("GROUPS", "NAME", groupName, "ORGANIZATION_UUID", organizationUuid);
    return ((Long) db.selectFirst(format("select ID from groups where name='%s' and organization_uuid='%s'", groupName, organizationUuid)).get("ID")).intValue();
  }

  private void insertUserGroup(int userId, int groupId) {
    db.executeInsert("GROUPS_USERS", "USER_ID", userId, "GROUP_ID", groupId);
  }

  private void setupDefaultOrganization() {
    setDefaultOrganizationProperty(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(DEFAULT_ORGANIZATION_UUID);
  }

  private void setDefaultOrganizationProperty(String defaultOrganizationUuid) {
    db.executeInsert(
      "INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", defaultOrganizationUuid);
  }

  private void verifyUserMembership(int userId, String... organizationUuids) {
    List<Map<String, Object>> rows = db.select(format("SELECT ORGANIZATION_UUID FROM " + TABLE + " WHERE USER_ID = %s", userId));
    List<String> userOrganizationUuids = rows.stream()
      .map(values -> (String) values.get("ORGANIZATION_UUID"))
      .collect(MoreCollectors.toList());
    assertThat(userOrganizationUuids).containsOnly(organizationUuids);
  }

}
