/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers;

import java.sql.SQLException;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateGroupsUsersUserUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateGroupsUsersUserUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateGroupsUsersUserUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    long userId_1 = 1L;
    String userUuid_1 = "uuid-1";
    insertUser(userId_1, userUuid_1);

    long userId_2 = 2L;
    String userUuid_2 = "uuid-2";
    insertUser(userId_2, userUuid_2);

    long userId_3 = 3L;
    String userUuid_3 = "uuid-3";
    insertUser(userId_3, userUuid_3);

    long userId_4 = 4L;
    String userUuid_4 = "uuid-4";
    insertUser(userId_4, userUuid_4);

    String groupUuid_1 = Uuids.createFast();
    insertGroupUser(userId_1, groupUuid_1);
    String groupUuid_2 = Uuids.createFast();
    insertGroupUser(userId_2, groupUuid_2);
    String groupUuid_3 = Uuids.createFast();
    insertGroupUser(userId_3, groupUuid_3);
    String groupUuid_4 = Uuids.createFast();
    insertGroupUser(userId_1, groupUuid_4);
    // orphan FK
    String groupUuid_5 = Uuids.createFast();
    insertGroupUser(100L, groupUuid_5);

    underTest.execute();

    assertThat(db.countRowsOfTable("groups_users")).isEqualTo(4);
    assertThatGroupsUserUserUuidIsEqualTo(userId_1, groupUuid_1, userUuid_1);
    assertThatGroupsUserUserUuidIsEqualTo(userId_2, groupUuid_2, userUuid_2);
    assertThatGroupsUserUserUuidIsEqualTo(userId_3, groupUuid_3, userUuid_3);
    assertThatGroupsUserUserUuidIsEqualTo(userId_1, groupUuid_4, userUuid_1);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long userId_1 = 1L;
    String userUuid_1 = "uuid-1";
    insertUser(userId_1, userUuid_1);

    long userId_2 = 2L;
    String userUuid_2 = "uuid-2";
    insertUser(userId_2, userUuid_2);

    long userId_3 = 3L;
    String userUuid_3 = "uuid-3";
    insertUser(userId_3, userUuid_3);

    long userId_4 = 4L;
    String userUuid_4 = "uuid-4";
    insertUser(userId_4, userUuid_4);

    String groupUuid_1 = Uuids.createFast();
    insertGroupUser(userId_1, groupUuid_1);
    String groupUuid_2 = Uuids.createFast();
    insertGroupUser(userId_2, groupUuid_2);
    String groupUuid_3 = Uuids.createFast();
    insertGroupUser(userId_3, groupUuid_3);

    underTest.execute();

    String groupUuid_4 = Uuids.createFast();
    insertGroupUser(userId_1, groupUuid_4);

    // re-entrant
    underTest.execute();

    assertThatGroupsUserUserUuidIsEqualTo(userId_1, groupUuid_1, userUuid_1);
    assertThatGroupsUserUserUuidIsEqualTo(userId_2, groupUuid_2, userUuid_2);
    assertThatGroupsUserUserUuidIsEqualTo(userId_3, groupUuid_3, userUuid_3);
    assertThatGroupsUserUserUuidIsEqualTo(userId_1, groupUuid_4, userUuid_1);
  }

  private void assertThatGroupsUserUserUuidIsEqualTo(Long userId, String groupUuid, String expectedUuid) {
    assertThat(db.select(String.format("select user_uuid from groups_users where user_id = %d and group_uuid = '%s'", userId, groupUuid))
      .stream()
      .map(row -> row.get("USER_UUID"))
      .filter(Objects::nonNull)
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertGroupUser(Long userId, String groupUuid) {
    db.executeInsert("groups_users",
      "user_id", userId,
      "group_uuid", groupUuid);
  }

  private void insertUser(Long id, String uuid) {
    db.executeInsert("users",
      "id", id,
      "uuid", uuid,
      "login", "login" + id,
      "external_login", "ex-login" + id,
      "external_identity_provider", "ex-provider" + id,
      "external_id", id + 1,
      "is_root", false,
      "onboarded", false);
  }
}
