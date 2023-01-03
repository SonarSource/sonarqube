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
package org.sonar.server.platform.db.migration.version.v84.users.fk.permtemplatesusers;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulatePermTemplatesUsersUserUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulatePermTemplatesUsersUserUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulatePermTemplatesUsersUserUuid(db.database());

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

    String permTemplatesUserUuid_1 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_1, userId_1);
    String permTemplatesUserUuid_2 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_2, userId_2);
    String permTemplatesUserUuid_3 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_3, userId_3);
    String permTemplatesUserUuid_4 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_4, userId_4);
    String permTemplatesUserUuid_5 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_5, userId_1);

    // orphan FK
    String permTemplatesUserUuid_6 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_6, 100L);

    underTest.execute();

    assertThat(db.countRowsOfTable("perm_templates_users")).isEqualTo(5);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_1, userUuid_1);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_2, userUuid_2);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_3, userUuid_3);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_4, userUuid_4);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_5, userUuid_1);
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

    String permTemplatesUserUuid_1 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_1, userId_1);
    String permTemplatesUserUuid_2 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_2, userId_2);
    String permTemplatesUserUuid_3 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_3, userId_3);

    underTest.execute();

    String permTemplatesUserUuid_4 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_4, userId_4);
    String permTemplatesUserUuid_5 = Uuids.createFast();
    insertPermTemplatesUser(permTemplatesUserUuid_5, userId_1);

    // re-entrant
    underTest.execute();

    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_1, userUuid_1);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_2, userUuid_2);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_3, userUuid_3);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_4, userUuid_4);
    assertThatPermTemplatesUsersUserUuidIsEqualTo(permTemplatesUserUuid_5, userUuid_1);
  }

  private void assertThatPermTemplatesUsersUserUuidIsEqualTo(String permTemplatesUserUuid, String expectedUuid) {
    assertThat(db.select(String.format("select user_uuid from perm_templates_users where uuid = '%s'", permTemplatesUserUuid))
      .stream()
      .map(row -> row.get("USER_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertPermTemplatesUser(String uuid, long userId) {
    db.executeInsert("perm_templates_users",
      "uuid", uuid,
      "user_id", userId,
      "template_id", userId + 100,
      "permission_reference", Uuids.createFast());
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
