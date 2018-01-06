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

import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class MakeComponentsPrivateBasedOnPermissionsTest {
  private static final String ROLE_USER = "user";
  private static final String ROLE_CODEVIEWER = "codeviewer";
  private static final String PROJECT_QUALIFIER = "TRK";
  private static final String VIEW_QUALIFIER = "VW";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeComponentsPrivateBasedOnPermissionsTest.class, "projects_and_group_roles_and_user_roles.sql");

  private final Random random = new Random();
  private final String randomPublicConditionRole = random.nextBoolean() ? ROLE_CODEVIEWER : ROLE_USER;
  private final String randomQualifier = random.nextBoolean() ? PROJECT_QUALIFIER : VIEW_QUALIFIER;
  private final String randomRole = "role_" + random.nextInt(12);
  private MakeComponentsPrivateBasedOnPermissions underTest = new MakeComponentsPrivateBasedOnPermissions(db.database());

  @Test
  public void execute_does_nothing_on_empty_tables() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_makes_project_private_if_Anyone_has_only_user_permission_and_project_has_at_least_one_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_USER, pId1, null);
    insertGroupPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
  }

  @Test
  public void execute_makes_project_private_if_Anyone_has_only_user_permission_and_project_has_one_user_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_USER, pId1, null);
    insertUserPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
  }

  @Test
  public void execute_keeps_project_public_if_Anyone_has_only_user_permission_and_project_has_no_user_nor_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_USER, pId1, null);

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_makes_project_private_if_Anyone_has_only_codeviewer_permission_and_project_has_one_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_CODEVIEWER, pId1, null);
    insertGroupPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
  }

  @Test
  public void execute_makes_project_private_if_Anyone_has_only_codeviewer_permission_and_project_has_one_user_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_CODEVIEWER, pId1, null);
    insertUserPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
  }

  @Test
  public void execute_keeps_project_public_if_Anyone_has_only_codeviewer_permission_and_project_has_no_user_nor_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_CODEVIEWER, pId1, null);

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_makes_project_private_if_Anyone_has_neither_user_nor_codeviewer_permission_and_project_has_one_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(randomRole, pId1, null);
    insertGroupPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
  }

  @Test
  public void execute_makes_project_private_if_Anyone_has_neither_user_nor_codeviewer_permission_and_project_has_one_user_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(randomRole, pId1, null);
    insertUserPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
  }

  @Test
  public void execute_keeps_project_public_if_Anyone_has_neither_user_nor_codeviewer_permission_and_project_has_no_user_nor_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission("foo", pId1, null);

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_keeps_project_public_if_Anyone_has_both_user_and_codeviewer_permission_and_project_has_one_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_USER, pId1, null);
    insertGroupPermission(ROLE_CODEVIEWER, pId1, null);
    insertGroupPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_keeps_project_public_if_Anyone_has_both_user_and_codeviewer_permission_and_project_has_user_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_USER, pId1, null);
    insertGroupPermission(ROLE_CODEVIEWER, pId1, null);
    insertUserPermission("foo", pId1, random.nextInt(10));

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_keeps_project_public_if_Anyone_has_both_user_and_codeviewer_permission_and_project_has_no_user_nor_other_group_permission() throws SQLException {
    long pId1 = insertRootComponent("p1", false);
    insertGroupPermission(ROLE_USER, pId1, null);
    insertGroupPermission(ROLE_CODEVIEWER, pId1, null);

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_keeps_project_public_if_it_has_no_user_nor_group_permission_at_all() throws SQLException {
    insertRootComponent("p1", false);

    underTest.execute();

    assertThat(isPrivate("p1")).isFalse();
  }

  @Test
  public void execute_does_not_change_private_projects_to_public_when_they_actually_should_be() throws SQLException {
    long p1Id = insertRootComponent("p1", true); // both user and codeviewer
    long p2Id = insertRootComponent("p2", true); // only user but no other permission
    long p3Id = insertRootComponent("p3", true); // only codeviewer but no other permission
    long p4Id = insertRootComponent("p4", true); // neither codeviewer nor user but no other permission
    insertGroupPermission(ROLE_CODEVIEWER, p1Id, null);
    insertGroupPermission(ROLE_USER, p1Id, null);
    insertGroupPermission(ROLE_CODEVIEWER, p2Id, null);
    insertGroupPermission(ROLE_USER, p3Id, null);
    insertGroupPermission(randomRole, p4Id, null);

    underTest.execute();

    assertThat(isPrivate("p1")).isTrue();
    assertThat(isPrivate("p2")).isTrue();
    assertThat(isPrivate("p3")).isTrue();
    assertThat(isPrivate("p4")).isTrue();
  }

  @Test
  public void execute_changes_non_root_rows_to_private_based_on_permissions_of_their_root_row() throws SQLException {
    // root stays public, children are unchanged
    long pId1 = insertRootComponent("root1", false);
    insertGroupPermission(randomPublicConditionRole, pId1, null);
    insertComponent("u1", "root1", false);
    // root becomes privates, children are changed accordingly
    long pId2 = insertRootComponent("root2", false);
    int someUserId = random.nextInt(50);
    insertGroupPermission(randomRole, pId2, someUserId);
    insertComponent("u2", "root2", false);
    insertComponent("u3", "root2", true);

    underTest.execute();

    assertThat(isPrivate("root1")).isFalse();
    assertThat(isPrivate("u1")).isFalse();
    assertThat(isPrivate("root2")).isTrue();
    assertThat(isPrivate("u2")).isTrue();
    assertThat(isPrivate("u3")).isTrue();
  }

  @Test
  public void execute_does_not_fix_inconsistencies_of_non_root_rows_if_root_stays_public_or_is_already_private() throws SQLException {
    // root stays public, children are unchanged
    long pId1 = insertRootComponent("root1", false);
    insertGroupPermission(randomPublicConditionRole, pId1, null);
    insertComponent("u1", "root1", false);
    insertComponent("u2", "root1", true); // inconsistent information is not fixed
    // root is already private but children are inconsistent => not fixed
    insertRootComponent("root2", true);
    insertGroupPermission(randomPublicConditionRole, pId1, null);
    insertComponent("u3", "root2", false);
    insertComponent("u4", "root2", true);

    underTest.execute();

    assertThat(isPrivate("root1")).isFalse();
    assertThat(isPrivate("u1")).isFalse();
    assertThat(isPrivate("u2")).isTrue();
    assertThat(isPrivate("root2")).isTrue();
    assertThat(isPrivate("u3")).isFalse();
    assertThat(isPrivate("u4")).isTrue();
  }

  @Test
  public void execute_does_change_non_root_rows_which_root_does_not_exist() throws SQLException {
    // non existent root, won't be changed
    long pId1 = insertComponent("u1", "non existent root", false);
    insertGroupPermission(randomPublicConditionRole, pId1, null);
    insertComponent("u2", "non existent root", true);

    underTest.execute();

    assertThat(isPrivate("u1")).isFalse();
    assertThat(isPrivate("u2")).isTrue();
  }

  @Test
  public void execute_deletes_any_permission_to_group_Anyone_for_root_components_which_are_made_private() throws SQLException {
    long idRoot1 = insertRootComponent("root1", false);
    int someGroupId = random.nextInt(50);
    int someUserId = random.nextInt(50);
    insertGroupPermission(randomRole, idRoot1, null);
    insertGroupPermission(randomRole, idRoot1, someGroupId);
    insertUserPermission(randomRole, idRoot1, someUserId);

    underTest.execute();

    assertThat(isPrivate("root1")).isTrue();
    assertThat(permissionsOfGroupAnyone(idRoot1)).isEmpty();
    assertThat(permissionsOfGroup(idRoot1, someGroupId)).containsOnly(randomRole, ROLE_USER, ROLE_CODEVIEWER);
    assertThat(permissionsOfUser(idRoot1, someUserId)).containsOnly(randomRole, ROLE_USER, ROLE_CODEVIEWER);
  }

  @Test
  public void execute_ensures_any_user_of_with_at_least_one_permission_on_root_component_which_is_made_private_also_has_permissions_USER_and_CODEVIEWER() throws SQLException {
    long idRoot = insertRootComponent("root1", false);
    String someRole = "role_" + random.nextInt(12);
    int user1 = insertUser();
    int user2 = insertUser();
    insertUserPermission(someRole, idRoot, user1);

    underTest.execute();

    assertThat(isPrivate("root1")).isTrue();
    assertThat(permissionsOfGroupAnyone(idRoot)).isEmpty();
    assertThat(permissionsOfUser(idRoot, user1)).containsOnly(someRole, ROLE_USER, ROLE_CODEVIEWER);
    assertThat(permissionsOfUser(idRoot, user2)).isEmpty();
  }

  @Test
  public void execute_ensures_any_group_of_with_at_least_one_permission_on_root_component_which_is_made_private_also_has_permissions_USER_and_CODEVIEWER() throws SQLException {
    long idRoot = insertRootComponent("root1", false);
    String someRole = "role_" + random.nextInt(12);
    int group1 = insertGroup();
    int group2 = insertGroup();
    insertGroupPermission(someRole, idRoot, group1);

    underTest.execute();

    assertThat(isPrivate("root1")).isTrue();
    assertThat(permissionsOfGroup(idRoot, group1)).containsOnly(someRole, ROLE_USER, ROLE_CODEVIEWER);
    assertThat(permissionsOfGroup(idRoot, group2)).isEmpty();
  }

  @Test
  public void execute_does_not_delete_permissions_to_group_Anyone_for_root_components_which_are_already_private() throws SQLException {
    long idRoot = insertRootComponent("root1", true);
    String someRole = "role_" + random.nextInt(12);
    int someGroupId = random.nextInt(50);
    int someUserId = random.nextInt(50);
    insertGroupPermission(someRole, idRoot, null);
    insertGroupPermission(someRole, idRoot, someGroupId);
    insertGroupPermission(randomPublicConditionRole, idRoot, someGroupId);
    insertUserPermission(someRole, idRoot, someUserId);
    insertUserPermission(randomPublicConditionRole, idRoot, someUserId);

    underTest.execute();

    assertThat(isPrivate("root1")).isTrue();
    assertThat(permissionsOfGroupAnyone(idRoot)).containsOnly(someRole);
    assertThat(permissionsOfGroup(idRoot, someGroupId)).containsOnly(someRole, ROLE_USER, ROLE_CODEVIEWER);
    assertThat(permissionsOfUser(idRoot, someUserId)).containsOnly(someRole, ROLE_USER, ROLE_CODEVIEWER);
  }

  @Test
  public void execute_ensures_any_user_of_with_at_least_one_permission_on_root_component_which_is_already_private_also_has_permissions_USER_and_CODEVIEWER() throws SQLException {
    long idRoot = insertRootComponent("root1", true);
    String someRole = "role_" + random.nextInt(12);
    int user1 = insertUser();
    int user2 = insertUser();
    insertUserPermission(someRole, idRoot, user1);

    underTest.execute();

    assertThat(isPrivate("root1")).isTrue();
    assertThat(permissionsOfGroupAnyone(idRoot)).isEmpty();
    assertThat(permissionsOfUser(idRoot, user1)).containsOnly(someRole, ROLE_USER, ROLE_CODEVIEWER);
    assertThat(permissionsOfUser(idRoot, user2)).isEmpty();
  }

  @Test
  public void execute_ensures_any_group_of_with_at_least_one_permission_on_root_component_which_is_already_private_also_has_permissions_USER_and_CODEVIEWER() throws SQLException {
    long idRoot = insertRootComponent("root1", true);
    String someRole = "role_" + random.nextInt(12);
    int group1 = insertGroup();
    int group2 = insertGroup();
    insertGroupPermission(someRole, idRoot, group1);

    underTest.execute();

    assertThat(isPrivate("root1")).isTrue();
    assertThat(permissionsOfGroup(idRoot, group1)).containsOnly(someRole, ROLE_USER, ROLE_CODEVIEWER);
    assertThat(permissionsOfGroup(idRoot, group2)).isEmpty();
  }

  @Test
  public void execute_deletes_any_USER_or_BROWSE_permission_of_public_project() throws SQLException {
    long idRoot = insertRootComponent("root1", false);
    int someGroupId = random.nextInt(55);
    int someUserId = random.nextInt(55);
    String someRole = "role_" + random.nextInt(12);
    Stream.of(ROLE_USER, ROLE_CODEVIEWER, someRole)
      .forEach(role -> {
        insertGroupPermission(role, idRoot, null);
        insertGroupPermission(role, idRoot, someGroupId);
        insertUserPermission(role, idRoot, someUserId);
      });
    assertThat(isPrivate("root1")).isFalse();
    assertThat(permissionsOfGroupAnyone(idRoot)).containsOnly(ROLE_USER, ROLE_CODEVIEWER, someRole);
    assertThat(permissionsOfGroup(idRoot, someGroupId)).containsOnly(ROLE_USER, ROLE_CODEVIEWER, someRole);
    assertThat(permissionsOfUser(idRoot, someUserId)).containsOnly(ROLE_USER, ROLE_CODEVIEWER, someRole);

    underTest.execute();

    assertThat(isPrivate("root1")).isFalse();
    assertThat(permissionsOfGroupAnyone(idRoot)).containsOnly(someRole);
    assertThat(permissionsOfGroup(idRoot, someGroupId)).containsOnly(someRole);
    assertThat(permissionsOfUser(idRoot, someUserId)).containsOnly(someRole);
  }

  private long insertRootComponent(String uuid, boolean isPrivate) {
    db.executeInsert(
      "PROJECTS",
      "ORGANIZATION_UUID", "org_" + uuid,
      "SCOPE", "PRJ",
      "QUALIFIER", randomQualifier,
      "UUID", uuid,
      "UUID_PATH", "path_" + uuid,
      "ROOT_UUID", "root_" + uuid,
      "PROJECT_UUID", uuid,
      "PRIVATE", valueOf(isPrivate));
    return (long) db.selectFirst("select id as \"ID\" from projects where uuid='" + uuid + "'").get("ID");
  }

  private long insertComponent(String uuid, String projectUuid, boolean isPrivate) {
    db.executeInsert(
      "PROJECTS",
      "ORGANIZATION_UUID", "org_" + uuid,
      "UUID", uuid,
      "UUID_PATH", "path_" + uuid,
      "ROOT_UUID", "root_" + uuid,
      "PROJECT_UUID", projectUuid,
      "PRIVATE", valueOf(isPrivate));
    return (long) db.selectFirst("select id as \"ID\" from projects where uuid='" + uuid + "'").get("ID");
  }

  private void insertGroupPermission(String role, @Nullable Long resourceId, @Nullable Integer groupId) {
    db.executeInsert(
      "GROUP_ROLES",
      "ORGANIZATION_UUID", "org" + random.nextInt(50),
      "GROUP_ID", groupId == null ? null : valueOf(groupId),
      "RESOURCE_ID", resourceId == null ? null : valueOf(resourceId),
      "ROLE", role);
  }

  private int groupCount = Math.abs(random.nextInt(22));

  private int insertGroup() {
    String name = "group" + groupCount++;
    db.executeInsert(
      "GROUPS",
      "ORGANIZATION_UUID", "org" + random.nextInt(12),
      "NAME", name);
    return ((Long) db.selectFirst("select id as \"ID\" from groups where name='" + name + "'").get("ID")).intValue();
  }

  private void insertUserPermission(String role, @Nullable Long resourceId, int userId) {
    db.executeInsert(
      "USER_ROLES",
      "ORGANIZATION_UUID", "org_" + random.nextInt(66),
      "USER_ID", valueOf(userId),
      "RESOURCE_ID", resourceId == null ? null : valueOf(resourceId),
      "ROLE", role);
  }

  private int userCount = Math.abs(random.nextInt(22));

  private int insertUser() {
    String login = "user" + userCount++;
    db.executeInsert(
      "USERS",
      "LOGIN", login,
      "IS_ROOT", String.valueOf(false));
    return ((Long) db.selectFirst("select id as \"ID\" from users where login='" + login + "'").get("ID")).intValue();
  }

  private boolean isPrivate(String uuid) {
    Map<String, Object> row = db.selectFirst("select private as \"PRIVATE\" from projects where uuid = '" + uuid + "'");
    return (boolean) row.get("PRIVATE");
  }

  private Set<String> permissionsOfGroupAnyone(long resourceId) {
    return db.select("select role from group_roles where group_id is null and resource_id = " + resourceId)
      .stream()
      .flatMap(map -> map.entrySet().stream())
      .map(entry -> (String) entry.getValue())
      .collect(MoreCollectors.toSet());
  }

  private Set<String> permissionsOfGroup(long resourceId, int groupId) {
    return db.select("select role from group_roles where group_id = " + groupId + " and resource_id = " + resourceId)
      .stream()
      .flatMap(map -> map.entrySet().stream())
      .map(entry -> (String) entry.getValue())
      .collect(MoreCollectors.toSet());
  }

  private Set<String> permissionsOfUser(long resourceId, int userId) {
    return db.select("select role from user_roles where resource_id = " + resourceId + " and user_id = " + userId)
      .stream()
      .flatMap(map -> map.entrySet().stream())
      .map(entry -> (String) entry.getValue())
      .collect(MoreCollectors.toSet());
  }
}
