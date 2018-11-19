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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CleanOrphanRowsInGroupsUsersTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanOrphanRowsInGroupsUsersTest.class, "initial.sql");

  private System2 system2 = mock(System2.class);

  private CleanOrphanRowsInGroupsUsers underTest = new CleanOrphanRowsInGroupsUsers(db.database());

  @Test
  public void remove_orphans_when_users_id_does_not_exist() throws Exception {
    insertUserGroups(1L, 10L);
    insertUserGroups(1L, 11L);
    insertUserGroups(2L, 12L);

    underTest.execute();

    assertThat(db.countRowsOfTable("groups_users")).isZero();
  }

  @Test
  public void remove_orphans_when_user_is_disabled() throws Exception {
    long user1 = insertUser("user1", false);
    insertUserGroups(user1, 10L);
    insertUserGroups(user1, 11L);

    underTest.execute();

    assertThat(db.countRowsOfTable("groups_users")).isZero();
  }

  @Test
  public void does_not_remove_group_membership_on_active_users() throws Exception {
    long user1 = insertUser("user1", true);
    insertUserGroups(user1, 10L);
    insertUserGroups(user1, 11L);
    long user2 = insertUser("user2", true);
    insertUserGroups(user2, 10L);

    underTest.execute();

    checkUserGroups(user1, 10L, 11L);
    checkUserGroups(user2, 10L);
  }

  @Test
  public void migration_is_reentrant() throws Exception {
    insertUserGroups(1L, 10L);
    insertUserGroups(2L, 10L);

    underTest.execute();
    assertThat(db.countRowsOfTable("groups_users")).isZero();

    underTest.execute();
    assertThat(db.countRowsOfTable("groups_users")).isZero();
  }

  private void checkUserGroups(long userId, Long... expectedGroupIds) {
    List<Long> groups = db.select(format("select gu.group_id from groups_users gu where gu.user_id=%s", userId)).stream()
      .map(map -> (Long) map.get("GROUP_ID"))
      .collect(Collectors.toList());
    assertThat(groups).containsOnly(expectedGroupIds);
  }

  private void insertUserGroups(long userId, Long... groupIds) {
    Arrays.stream(groupIds).forEach(groupId -> db.executeInsert(
      "GROUPS_USERS",
      "USER_ID", userId,
      "GROUP_ID", groupId));
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
}
