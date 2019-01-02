/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static java.lang.String.format;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.WARN;

public class RestoreSonarUsersGroupsTest {

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);
  private static final String DEFAULT_ORGANIZATION_UUID = "def-org";
  private static final String SONAR_USERS_NAME = "sonar-users";
  private static final String SONAR_USERS_PENDING_DESCRIPTION = "<PENDING>";
  private static final String SONAR_USERS_FINAL_DESCRIPTION = "Any new users created will automatically join this group";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(RestoreSonarUsersGroupsTest.class, "initial.sql");

  private System2 system2 = mock(System2.class);

  private RestoreSonarUsersGroups underTest = new RestoreSonarUsersGroups(db.database(), system2, new DefaultOrganizationUuidProviderImpl());

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW.getTime());
  }

  @Test
  public void insert_sonar_users_group_when_it_does_not_exist() throws SQLException {
    setupDefaultOrganization();
    setDefaultGroup("default-group");

    underTest.execute();

    checkSonarUsersHasBeenCreated();
  }

  @Test
  public void display_log_when_creating_sonar_users_group() throws SQLException {
    setupDefaultOrganization();
    setDefaultGroup("default-group");

    underTest.execute();

    checkSonarUsersHasBeenCreated();
    assertThat(logTester.logs(WARN)).containsOnly("The default group has been updated from 'default-group' to 'sonar-users'. Please verify your permission schema that everything is in order");
  }

  @Test
  public void copy_permission_from_existing_default_group_to_sonar_users_when_it_does_not_exist() throws Exception {
    setupDefaultOrganization();
    long defaultGroupId = setDefaultGroup("default-group");
    insertGroupRole(defaultGroupId, "user", null);
    insertGroupRole(defaultGroupId, "admin", 1L);
    insertPermissionTemplate(defaultGroupId, "user", 10L);

    underTest.execute();

    checkSonarUsersHasBeenCreated();
    checkUserRolesOnSonarUsers(tuple("user", null, DEFAULT_ORGANIZATION_UUID), tuple("admin", 1L, DEFAULT_ORGANIZATION_UUID));
    checkPermissionTemplatesOnSonarUsers(tuple("user", 10L, NOW, NOW));
  }

  @Test
  public void update_sonar_users_group_when_existing_with_incorrect_description() throws Exception {
    setupDefaultOrganization();
    insertGroup(SONAR_USERS_NAME, "Other description", PAST, PAST);

    underTest.execute();

    checkSonarUsersHasBeenUpdated();
  }

  @Test
  public void update_sonar_users_group_when_default_group_setting_is_null() throws SQLException {
    setupDefaultOrganization();
    insertDefaultGroupProperty(null);
    insertGroup(SONAR_USERS_NAME, "Other description", PAST, PAST);

    underTest.execute();

    checkSonarUsersHasBeenUpdated();
  }

  @Test
  public void does_nothing_when_sonar_users_exist_with_right_description() throws SQLException {
    setupDefaultOrganization();
    insertGroup(SONAR_USERS_NAME, SONAR_USERS_FINAL_DESCRIPTION, PAST, PAST);

    underTest.execute();

    checkSonarUsersHasNotBeenUpdated();
  }

  @Test
  public void display_log_when_moving_default_group_to_sonar_users_group() throws SQLException {
    setupDefaultOrganization();
    insertGroup(SONAR_USERS_NAME, "wrong desc", PAST, PAST);
    setDefaultGroup("default-group");

    underTest.execute();

    checkSonarUsersHasBeenUpdated();
    assertThat(logTester.logs(WARN)).containsOnly("The default group has been updated from 'default-group' to 'sonar-users'. Please verify your permission schema that everything is in order");
  }

  @Test
  public void does_not_copy_permission_existing_default_group_to_sonar_users_when_it_already_exists() throws Exception {
    setupDefaultOrganization();
    long defaultGroupId = setDefaultGroup("default-group");
    insertGroupRole(defaultGroupId, "user", null);
    insertGroupRole(defaultGroupId, "admin", 1L);
    insertPermissionTemplate(defaultGroupId, "user", 10L);
    // sonar-users has no permission on it
    insertGroup(SONAR_USERS_NAME, SONAR_USERS_FINAL_DESCRIPTION, PAST, PAST);

    underTest.execute();

    checkSonarUsersHasNotBeenUpdated();
    // No permission set on sonar-users
    checkUserRolesOnSonarUsers();
    checkPermissionTemplatesOnSonarUsers();
  }

  @Test
  public void does_not_display_log_when_default_group_is_sonar_users() throws SQLException {
    setupDefaultOrganization();
    insertGroup(SONAR_USERS_NAME, SONAR_USERS_FINAL_DESCRIPTION, PAST, PAST);
    insertDefaultGroupProperty(SONAR_USERS_NAME);

    underTest.execute();

    assertThat(logTester.logs(WARN)).isEmpty();
  }

  @Test
  public void continue_migration_when_description_is_pending() throws Exception {
    setupDefaultOrganization();
    // Default group with is permissions
    long defaultGroupId = setDefaultGroup("default-group");
    insertGroupRole(defaultGroupId, "admin", 1L);
    insertGroupRole(defaultGroupId, "user", 2L);
    insertGroupRole(defaultGroupId, "codeviewer", null);
    insertPermissionTemplate(defaultGroupId, "user", 10L);
    insertPermissionTemplate(defaultGroupId, "admin", 11L);
    // sonar-users group with partial permissions from default group
    long sonarUsersGroupId = insertGroup(SONAR_USERS_NAME, SONAR_USERS_PENDING_DESCRIPTION, PAST, PAST);
    insertGroupRole(sonarUsersGroupId, "admin", 1L);
    insertPermissionTemplate(sonarUsersGroupId, "user", 10L);

    underTest.execute();

    checkSonarUsersHasBeenUpdated();
    checkUserRolesOnSonarUsers(tuple("admin", 1L, DEFAULT_ORGANIZATION_UUID), tuple("user", 2L, DEFAULT_ORGANIZATION_UUID), tuple("codeviewer", null, DEFAULT_ORGANIZATION_UUID));
    checkPermissionTemplatesOnSonarUsers(tuple("user", 10L, PAST, PAST), tuple("admin", 11L, NOW, NOW));
  }

  @Test
  public void does_not_update_other_groups() throws SQLException {
    setupDefaultOrganization();
    insertGroup("another-group", "another-group", PAST, PAST);
    insertGroup(SONAR_USERS_NAME, SONAR_USERS_FINAL_DESCRIPTION, PAST, PAST);

    underTest.execute();

    checkSonarUsersHasNotBeenUpdated();
    assertThat(db.countRowsOfTable("groups")).isEqualTo(2);
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    setupDefaultOrganization();
    long defaultGroupId = setDefaultGroup("default-group");
    insertGroupRole(defaultGroupId, "user", null);
    insertGroupRole(defaultGroupId, "admin", 1L);
    insertPermissionTemplate(defaultGroupId, "user", 10L);

    underTest.execute();
    checkSonarUsersHasBeenCreated();
    checkUserRolesOnSonarUsers(tuple("user", null, DEFAULT_ORGANIZATION_UUID), tuple("admin", 1L, DEFAULT_ORGANIZATION_UUID));
    checkPermissionTemplatesOnSonarUsers(tuple("user", 10L, NOW, NOW));

    underTest.execute();
    checkSonarUsersHasBeenCreated();
    checkUserRolesOnSonarUsers(tuple("user", null, DEFAULT_ORGANIZATION_UUID), tuple("admin", 1L, DEFAULT_ORGANIZATION_UUID));
    checkPermissionTemplatesOnSonarUsers(tuple("user", 10L, NOW, NOW));
  }

  @Test
  public void fail_when_no_default_group_in_setting_and_sonar_users_does_not_exist() throws Exception {
    setupDefaultOrganization();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group setting sonar.defaultGroup is defined to a 'sonar-users' group but it doesn't exist.");

    underTest.execute();
  }

  @Test
  public void fail_when_default_group_setting_is_set_to_an_unknown_group() throws SQLException {
    setupDefaultOrganization();
    insertDefaultGroupProperty("unknown");
    insertGroup(SONAR_USERS_NAME, SONAR_USERS_FINAL_DESCRIPTION, PAST, PAST);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default group setting sonar.defaultGroup is defined to an unknown group.");

    underTest.execute();
  }

  private void checkSonarUsersHasBeenCreated() {
    Map<String, Object> group = selectSonarUsersGroup();
    checkSonarUsersCommonInfo(group);
    assertThat(group.get("CREATED_AT")).isEqualTo(NOW);
    assertThat(group.get("UPDATED_AT")).isEqualTo(NOW);
  }

  private void checkSonarUsersHasBeenUpdated() {
    Map<String, Object> group = selectSonarUsersGroup();
    checkSonarUsersCommonInfo(group);
    assertThat(group.get("CREATED_AT")).isEqualTo(PAST);
    assertThat(group.get("UPDATED_AT")).isEqualTo(NOW);
  }

  private void checkSonarUsersHasNotBeenUpdated() {
    Map<String, Object> group = selectSonarUsersGroup();
    checkSonarUsersCommonInfo(group);
    assertThat(group.get("CREATED_AT")).isEqualTo(PAST);
    assertThat(group.get("UPDATED_AT")).isEqualTo(PAST);
  }

  private void checkSonarUsersCommonInfo(Map<String, Object> group) {
    assertThat(group.get("NAME")).isEqualTo("sonar-users");
    assertThat(group.get("DESCRIPTION")).isEqualTo("Any new users created will automatically join this group");
    assertThat(group.get("ORGANIZATION_UUID")).isEqualTo(DEFAULT_ORGANIZATION_UUID);
  }

  private void checkUserRolesOnSonarUsers(Tuple... expectedTuples) {
    List<Tuple> tuples = db.select("select gr.role, gr.resource_id, gr.organization_uuid from group_roles gr " +
      "inner join groups g on g.id=gr.group_id " +
      "where g.name='sonar-users'").stream()
      .map(map -> new Tuple(map.get("ROLE"), map.get("RESOURCE_ID"), map.get("ORGANIZATION_UUID")))
      .collect(Collectors.toList());
    assertThat(tuples).containsOnly(expectedTuples);
  }

  private void checkPermissionTemplatesOnSonarUsers(Tuple... expectedTuples) {
    List<Tuple> tuples = db.select("select ptg.permission_reference, ptg.template_id, ptg.created_at, ptg.updated_at from perm_templates_groups ptg " +
      "inner join groups g on g.id=ptg.group_id " +
      "where g.name='sonar-users'").stream()
      .map(map -> new Tuple(map.get("PERMISSION_REFERENCE"), map.get("TEMPLATE_ID"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(Collectors.toList());
    assertThat(tuples).containsOnly(expectedTuples);
  }

  private Map<String, Object> selectSonarUsersGroup() {
    return db.selectFirst("select name, description, organization_uuid, created_at, updated_at from groups where name='sonar-users'");
  }

  private long insertGroup(String name, String description, Date createdAt, Date updatedAt) {
    db.executeInsert(
      "GROUPS",
      "NAME", name,
      "DESCRIPTION", description,
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID,
      "CREATED_AT", createdAt,
      "UPDATED_AT", updatedAt);
    return (Long) db.selectFirst(format("select id from groups where name='%s'", name)).get("ID");
  }

  private void insertDefaultGroupProperty(@Nullable String groupName) {
    db.executeInsert(
      "PROPERTIES",
      "PROP_KEY", "sonar.defaultGroup",
      "TEXT_VALUE", groupName,
      "IS_EMPTY", Boolean.toString(groupName == null),
      "CREATED_AT", "1000");
  }

  private void insertGroupRole(@Nullable Long groupId, String permission, @Nullable Long projectId) {
    db.executeInsert(
      "GROUP_ROLES",
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID,
      "GROUP_ID", groupId,
      "RESOURCE_ID", projectId,
      "ROLE", permission);
  }

  private void insertPermissionTemplate(@Nullable Long groupId, String permission, @Nullable Long templateId) {
    db.executeInsert(
      "PERM_TEMPLATES_GROUPS",
      "GROUP_ID", groupId,
      "TEMPLATE_ID", templateId,
      "PERMISSION_REFERENCE", permission,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }

  private void setupDefaultOrganization() {
    db.executeInsert("ORGANIZATIONS",
      "UUID", DEFAULT_ORGANIZATION_UUID,
      "KEE", DEFAULT_ORGANIZATION_UUID, "NAME",
      DEFAULT_ORGANIZATION_UUID, "GUARDED", false,
      "CREATED_AT", nextLong(),
      "UPDATED_AT", nextLong());
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", DEFAULT_ORGANIZATION_UUID);
  }

  private long setDefaultGroup(String name) {
    insertDefaultGroupProperty(name);
    return insertGroup(name, name, PAST, PAST);
  }

}
