/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateQProfileEditUsersUserUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateQProfileEditUsersUserUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateQProfileEditUsersUserUuid(db.database());

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

    String qprofileEditUserUuid_1 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_1, userId_1);
    String qprofileEditUserUuid_2 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_2, userId_2);
    String qprofileEditUserUuid_3 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_3, userId_3);
    String qprofileEditUserUuid_4 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_4, userId_4);
    String qprofileEditUserUuid_5 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_5, userId_1);

    // orphan FK
    String qprofileEditUserUuid_6 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_6, 100L);

    underTest.execute();

    assertThat(db.countRowsOfTable("qprofile_edit_users")).isEqualTo(5);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_1, userUuid_1);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_2, userUuid_2);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_3, userUuid_3);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_4, userUuid_4);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_5, userUuid_1);
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

    String qprofileEditUserUuid_1 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_1, userId_1);
    String qprofileEditUserUuid_2 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_2, userId_2);
    String qprofileEditUserUuid_3 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_3, userId_3);

    underTest.execute();

    String qprofileEditUserUuid_4 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_4, userId_4);
    String qprofileEditUserUuid_5 = Uuids.createFast();
    insertQProfileEditUser(qprofileEditUserUuid_5, userId_1);

    // re-entrant
    underTest.execute();

    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_1, userUuid_1);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_2, userUuid_2);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_3, userUuid_3);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_4, userUuid_4);
    assertThatQProfileEditUserUserUuidIsEqualTo(qprofileEditUserUuid_5, userUuid_1);
  }

  private void assertThatQProfileEditUserUserUuidIsEqualTo(String qprofileEditUserUuid, String expectedUuid) {
    assertThat(db.select(String.format("select user_uuid from qprofile_edit_users where uuid = '%s'", qprofileEditUserUuid))
      .stream()
      .map(row -> row.get("USER_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertQProfileEditUser(String uuid, long userId) {
    db.executeInsert("qprofile_edit_users",
      "uuid", uuid,
      "user_id", userId,
      "qprofile_uuid", Uuids.createFast(),
      "created_at", System.currentTimeMillis());
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
