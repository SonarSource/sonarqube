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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class SetOrganizationMembersIntoMembersGroupTest {

  private static final String MEMBERS_NAME = "Members";
  private static final String ORGANIZATION_1 = "ORGANIZATION_1";
  private static final String ORGANIZATION_2 = "ORGANIZATION_2";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetOrganizationMembersIntoMembersGroupTest.class, "initial.sql");

  private SetOrganizationMembersIntoMembersGroup underTest = new SetOrganizationMembersIntoMembersGroup(db.database());

  @Test
  public void set_users_into_group_members() throws Exception {
    long user1 = insertUser("user1", true);
    long user2 = insertUser("user2", true);
    long user3 = insertUser("user3", true);
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganizationMember(user1, ORGANIZATION_1);
    insertOrganizationMember(user2, ORGANIZATION_2);
    insertOrganizationMember(user3, ORGANIZATION_1);

    underTest.execute();

    checkUserGroups(user1, group1);
    checkUserGroups(user2, group2);
    checkUserGroups(user3, group1);
  }

  @Test
  public void set_users_into_group_members_when_some_users_already_belongs_to_group() throws Exception {
    long user1 = insertUser("user1", true);
    long user2 = insertUser("user2", true);
    long user3 = insertUser("user3", true);
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganizationMember(user1, ORGANIZATION_1);
    insertOrganizationMember(user2, ORGANIZATION_2);
    insertOrganizationMember(user3, ORGANIZATION_1);
    insertUserGroups(user1, group1);

    underTest.execute();

    checkUserGroups(user1, group1);
    checkUserGroups(user2, group2);
    checkUserGroups(user3, group1);
  }

  @Test
  public void does_nothing_if_members_group_does_not_exist() throws Exception {
    long user1 = insertUser("user1", true);
    insertGroup(ORGANIZATION_1, "other");
    insertGroup(ORGANIZATION_2, "other");
    insertOrganizationMember(user1, ORGANIZATION_1);

    underTest.execute();

    checkUserGroups(user1);
  }

  @Test
  public void does_not_fail_when_users_already_belongs_to_group_members() throws Exception {
    long user1 = insertUser("user1", true);
    long user2 = insertUser("user2", true);
    long group1 = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    long group2 = insertGroup(ORGANIZATION_2, MEMBERS_NAME);
    insertOrganizationMember(user1, ORGANIZATION_1);
    insertOrganizationMember(user2, ORGANIZATION_2);
    insertUserGroups(user1, group1);
    insertUserGroups(user2, group2);

    underTest.execute();

    checkUserGroups(user1, group1);
    checkUserGroups(user2, group2);
  }

  @Test
  public void ignore_disabled_users() throws Exception {
    long user = insertUser("user", false);
    insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    insertOrganizationMember(user, ORGANIZATION_1);

    underTest.execute();

    checkUserGroups(user);
  }

  @Test
  public void migration_is_renentrant() throws Exception {
    long user = insertUser("user1", true);
    long group = insertGroup(ORGANIZATION_1, MEMBERS_NAME);
    insertOrganizationMember(user, ORGANIZATION_1);

    underTest.execute();
    checkUserGroups(user, group);

    underTest.execute();
    checkUserGroups(user, group);
  }

  private void checkUserGroups(long userId, Long... expectedGroupIds) {
    List<Long> groups = db.select(format("select gu.group_id from groups_users gu where gu.user_id=%s", userId)).stream()
      .map(map -> (Long) map.get("GROUP_ID"))
      .collect(Collectors.toList());
    assertThat(groups).containsOnly(expectedGroupIds);
  }

  private long insertUser(String login, boolean enabled) {
    db.executeInsert(
      "USERS",
      "LOGIN", login,
      "ACTIVE", Boolean.toString(enabled),
      "IS_ROOT", "false",
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
    return (Long) db.selectFirst(format("select id from users where login='%s'", login)).get("ID");
  }

  private void insertOrganizationMember(long userId, String organizationUuid) {
    db.executeInsert("ORGANIZATION_MEMBERS", "USER_ID", userId, "ORGANIZATION_UUID", organizationUuid);
  }

  private long insertGroup(String organization, String name) {
    db.executeInsert(
      "GROUPS",
      "NAME", name,
      "DESCRIPTION", name,
      "ORGANIZATION_UUID", organization,
      "CREATED_AT", new Date(),
      "UPDATED_AT", new Date());
    return (Long) db.selectFirst(format("select id from groups where name='%s' and organization_uuid='%s'", name, organization)).get("ID");
  }

  private void insertUserGroups(long userId, Long... groupIds) {
    Arrays.stream(groupIds).forEach(groupId -> db.executeInsert(
      "GROUPS_USERS",
      "USER_ID", userId,
      "GROUP_ID", groupId));
  }
}
