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
package org.sonar.server.platform.db.migration.version.v84.users.fk.userroles;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateUserRolesUserUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUserRolesUserUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateUserRolesUserUuid(db.database());

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

    String userRoleUuid_1 = Uuids.createFast();
    insertUserRole(userRoleUuid_1, userId_1);
    String userRoleUuid_2 = Uuids.createFast();
    insertUserRole(userRoleUuid_2, userId_2);
    String userRoleUuid_3 = Uuids.createFast();
    insertUserRole(userRoleUuid_3, userId_3);
    String userRoleUuid_4 = Uuids.createFast();
    insertUserRole(userRoleUuid_4, userId_4);
    String userRoleUuid_5 = Uuids.createFast();
    insertUserRole(userRoleUuid_5, userId_1);

    underTest.execute();

    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_1, userUuid_1);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_2, userUuid_2);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_3, userUuid_3);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_4, userUuid_4);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_5, userUuid_1);
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

    String userRoleUuid_1 = Uuids.createFast();
    insertUserRole(userRoleUuid_1, userId_1);
    String userRoleUuid_2 = Uuids.createFast();
    insertUserRole(userRoleUuid_2, userId_2);
    String userRoleUuid_3 = Uuids.createFast();
    insertUserRole(userRoleUuid_3, userId_3);

    underTest.execute();

    String userRoleUuid_4 = Uuids.createFast();
    insertUserRole(userRoleUuid_4, userId_4);
    String userRoleUuid_5 = Uuids.createFast();
    insertUserRole(userRoleUuid_5, userId_1);

    // re-entrant
    underTest.execute();

    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_1, userUuid_1);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_2, userUuid_2);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_3, userUuid_3);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_4, userUuid_4);
    assertThatUserRoleUserUuidIsEqualTo(userRoleUuid_5, userUuid_1);
  }

  private void assertThatUserRoleUserUuidIsEqualTo(String userRoleUuid, String expectedUuid) {
    assertThat(db.select(String.format("select user_uuid from user_roles where uuid = '%s'", userRoleUuid))
      .stream()
      .map(row -> row.get("USER_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertUserRole(String uuid, Long userId) {
    db.executeInsert("user_roles",
      "uuid", uuid,
      "organization_uuid", Uuids.createFast(),
      "user_id", userId,
      "role", Uuids.createFast());
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
