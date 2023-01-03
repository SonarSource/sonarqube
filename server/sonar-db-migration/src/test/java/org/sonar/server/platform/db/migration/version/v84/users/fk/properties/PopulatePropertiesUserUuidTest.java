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
package org.sonar.server.platform.db.migration.version.v84.users.fk.properties;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulatePropertiesUserUuidTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulatePropertiesUserUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulatePropertiesUserUuid(db.database());

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

    String propertyUuid_1 = Uuids.createFast();
    insertProperty(propertyUuid_1, userId_1);
    String propertyUuid_2 = Uuids.createFast();
    insertProperty(propertyUuid_2, userId_2);
    String propertyUuid_3 = Uuids.createFast();
    insertProperty(propertyUuid_3, userId_3);
    String propertyUuid_4 = Uuids.createFast();
    insertProperty(propertyUuid_4, userId_4);
    String propertyUuid_5 = Uuids.createFast();
    insertProperty(propertyUuid_5, null);

    underTest.execute();

    assertThatPropertyUserUuidIsEqualTo(propertyUuid_1, userUuid_1);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_2, userUuid_2);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_3, userUuid_3);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_4, userUuid_4);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_5, null);
  }

  @Test
  public void should_remove_property_for_non_existent_user() throws SQLException {
    long userId_1 = 1L;
    String userUuid_1 = "uuid-1";
    insertUser(userId_1, userUuid_1);

    long userId_2 = 2L;

    long userId_3 = 3L;

    String propertyUuid_1 = Uuids.createFast();
    insertProperty(propertyUuid_1, userId_1);
    String propertyUuid_2 = Uuids.createFast();
    insertProperty(propertyUuid_2, userId_2);
    String propertyUuid_3 = Uuids.createFast();
    insertProperty(propertyUuid_3, userId_3);

    underTest.execute();

    assertThatPropertyUserUuidIsEqualTo(propertyUuid_1, userUuid_1);
    assertPropertyIsRemoved(propertyUuid_2);
    assertPropertyIsRemoved(propertyUuid_3);
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

    String propertyUuid_1 = Uuids.createFast();
    insertProperty(propertyUuid_1, userId_1);
    String propertyUuid_2 = Uuids.createFast();
    insertProperty(propertyUuid_2, userId_2);
    String propertyUuid_3 = Uuids.createFast();
    insertProperty(propertyUuid_3, userId_3);

    underTest.execute();

    String propertyUuid_4 = Uuids.createFast();
    insertProperty(propertyUuid_4, userId_4);
    String propertyUuid_5 = Uuids.createFast();
    insertProperty(propertyUuid_5, null);

    // re-entrant
    underTest.execute();

    assertThatPropertyUserUuidIsEqualTo(propertyUuid_1, userUuid_1);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_2, userUuid_2);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_3, userUuid_3);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_4, userUuid_4);
    assertThatPropertyUserUuidIsEqualTo(propertyUuid_5, null);
  }

  private void assertThatPropertyUserUuidIsEqualTo(String propertyUuid, String expectedUuid) {
    Optional<Object> optional = db.select(String.format("select user_uuid from properties where uuid = '%s'", propertyUuid))
      .stream()
      .map(row -> row.get("USER_UUID"))
      .filter(Objects::nonNull)
      .findFirst();

    if (expectedUuid != null) {
      assertThat(optional).hasValue(expectedUuid);
    } else {
      assertThat(optional).isEmpty();
    }

  }

  private void assertPropertyIsRemoved(String propertyUuid){
    assertThat(db.select(String.format("select 1 from properties where uuid = '%s'", propertyUuid))).isEmpty();
  }

  private void insertProperty(String uuid, Long userId) {
    db.executeInsert("properties",
      "uuid", uuid,
      "user_id", userId,
      "prop_key", "kee-" + uuid,
      "is_empty", false,
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
