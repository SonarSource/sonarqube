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
package org.sonar.server.platform.db.migration.version.v84.usertokens;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateUserTokensUuidTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUserTokensUuidTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private DataChange underTest = new PopulateUserTokensUuid(db.database(), uuidFactory);

  @Test
  public void populate_uuids() throws SQLException {
    insertUserToken(1L, "user1", "name1", "token1");
    insertUserToken(2L, "user2", "name2", "token2");
    insertUserToken(3L, "user3", "name3", "token3");

    underTest.execute();

    verifyUuidsAreNotNull();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertUserToken(1L, "user1", "name1", "token1");
    insertUserToken(2L, "user2", "name2", "token2");
    insertUserToken(3L, "user3", "name3", "token3");

    underTest.execute();
    verifyUuidsAreNotNull();
    insertUserToken(4L, "user4", "name4", "token4");
    insertUserToken(5L, "user5", "name5", "token5");

    List<String> uuids = db.select("select uuid from user_tokens where id = 1 or id = 2 or id = 3")
      .stream()
      .map(row -> (String) row.get("UUID"))
      .collect(Collectors.toList());

    // re-entrant
    underTest.execute();
    verifyUuidsAreNotNull();

    // verify that uuid set during the first migration have not been updated during the second migration
    assertThat(db.select("select uuid from user_tokens")
      .stream()
      .map(row -> (String) row.get("UUID"))
      .collect(Collectors.toList()))
      .containsAll(uuids);
  }

  private void verifyUuidsAreNotNull() {
    assertThat(db.select("select uuid from user_tokens")
      .stream()
      .map(row -> row.get("UUID"))
      .filter(Objects::isNull)
      .collect(Collectors.toList())).isEmpty();
  }

  private void insertUserToken(Long id, String userUuid, String name, String tokenHash) {
    db.executeInsert("user_tokens",
      "id", id,
      "USER_UUID", userUuid,
      "NAME", name,
      "TOKEN_HASH", tokenHash,
      "CREATED_AT", 0L);
  }
}
