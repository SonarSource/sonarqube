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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class SetAllUsersIntoSonarUsersGroupTest {

  private static final String SONAR_USERS_NAME = "sonar-users";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetAllUsersIntoSonarUsersGroupTest.class, "initial.sql");

  private SetAllUsersIntoSonarUsersGroup underTest = new SetAllUsersIntoSonarUsersGroup(db.database());

  @Test
  public void set_users_into_sonar_users_group() throws Exception {
    long sonarUsersGroupId = insertGroup(SONAR_USERS_NAME);
    long userId = insertUser("user", true);

    underTest.execute();

    checkUserGroups(userId, sonarUsersGroupId);
  }

  @Test
  public void does_not_remove_existing_sonar_users_group_membership() throws Exception {
    long sonarUsersGroupId = insertGroup(SONAR_USERS_NAME);
    long userId = insertUser("user", true);
    insertUserGroups(userId, sonarUsersGroupId);

    underTest.execute();

    checkUserGroups(userId, sonarUsersGroupId);
  }

  @Test
  public void does_not_remove_existing_group_membership() throws Exception {
    long sonarUsersGroupId = insertGroup(SONAR_USERS_NAME);
    long anotherGroupId = insertGroup("another-group");
    long userId = insertUser("user", true);
    insertUserGroups(userId, anotherGroupId);

    underTest.execute();

    checkUserGroups(userId, sonarUsersGroupId, anotherGroupId);
  }

  @Test
  public void ignore_disabled_users() throws Exception {
    insertGroup(SONAR_USERS_NAME);
    long userId = insertUser("user", false);

    underTest.execute();

    checkUserGroups(userId);
  }

  @Test
  public void migration_is_renentrant() throws Exception {
    long sonarUsersGroupId = insertGroup(SONAR_USERS_NAME);
    long userId = insertUser("user", true);

    underTest.execute();
    checkUserGroups(userId, sonarUsersGroupId);

    underTest.execute();
    checkUserGroups(userId, sonarUsersGroupId);
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

  private long insertGroup(String name) {
    db.executeInsert(
      "GROUPS",
      "NAME", name,
      "DESCRIPTION", name,
      "ORGANIZATION_UUID", "ORGANIZATION_UUID",
      "CREATED_AT", new Date(),
      "UPDATED_AT", new Date());
    return (Long) db.selectFirst(format("select id from groups where name='%s'", name)).get("ID");
  }

  private void insertUserGroups(long userId, Long... groupIds) {
    Arrays.stream(groupIds).forEach(groupId -> db.executeInsert(
      "GROUPS_USERS",
      "USER_ID", userId,
      "GROUP_ID", groupId));
  }
}
