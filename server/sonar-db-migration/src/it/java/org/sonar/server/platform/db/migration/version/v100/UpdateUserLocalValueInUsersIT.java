/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateUserLocalValueInUsersIT {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(UpdateUserLocalValueInUsers.class);

  private final DataChange underTest = new UpdateUserLocalValueInUsers(db.database());

  @Test
  void migration_updates_user_local_if_null() throws SQLException {
    String userUuid1 = insertUser(true);
    String userUuid2 = insertUser(false);
    String userUuid3 = insertUser(null);

    underTest.execute();

    assertUserLocalIsUpdatedCorrectly(userUuid1, true);
    assertUserLocalIsUpdatedCorrectly(userUuid2, false);
    assertUserLocalIsUpdatedCorrectly(userUuid3, true);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    String userUuid1 = insertUser(true);
    String userUuid2 = insertUser(false);
    String userUuid3 = insertUser(null);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertUserLocalIsUpdatedCorrectly(userUuid1, true);
    assertUserLocalIsUpdatedCorrectly(userUuid2, false);
    assertUserLocalIsUpdatedCorrectly(userUuid3, true);
  }

  private void assertUserLocalIsUpdatedCorrectly(String userUuid, boolean expected) {
    String selectSql = String.format("select user_local from users where uuid='%s'", userUuid);
    assertThat(db.select(selectSql).stream()
      .map(row -> row.get("USER_LOCAL"))
      .toList())
      .containsExactlyInAnyOrder(expected);
  }

  private String insertUser(Boolean userLocal) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("LOGIN", secure().nextAlphabetic(20));
    map.put("EXTERNAL_LOGIN", secure().nextAlphabetic(20));
    map.put("EXTERNAL_IDENTITY_PROVIDER", "sonarqube");
    map.put("EXTERNAL_ID", secure().nextNumeric(5));
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("USER_LOCAL", userLocal);
    map.put("RESET_PASSWORD", false);
    db.executeInsert("users", map);
    return uuid;
  }
}
