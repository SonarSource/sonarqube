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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class UpsertUserTokensTypeValueTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpsertUserTokensTypeValueTest.class, "schema.sql");

  private final DataChange underTest = new UpsertUserTokensTypeValue(db.database());

  @Test
  public void migration_populates_user_tokens_type() throws SQLException {
    String userToken1 = insertUserToken();
    String userToken2 = insertUserToken();

    underTest.execute();

    assertUserTokenTypeIsUpsertedCorrectly(userToken1);
    assertUserTokenTypeIsUpsertedCorrectly(userToken2);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String userToken1 = insertUserToken();
    String userToken2 = insertUserToken();

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertUserTokenTypeIsUpsertedCorrectly(userToken1);
    assertUserTokenTypeIsUpsertedCorrectly(userToken2);
  }

  private void assertUserTokenTypeIsUpsertedCorrectly(String userUuid) {
    String selectSql = String.format("select TYPE from user_tokens where uuid='%s'", userUuid);
    assertThat(db.select(selectSql).stream().map(row -> row.get("TYPE")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder("USER_TOKEN");
  }

  private String insertUserToken() {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("USER_UUID", uuid);
    map.put("NAME", randomAlphabetic(20));
    map.put("TOKEN_HASH", randomAlphabetic(20));
    map.put("LAST_CONNECTION_DATE", System.currentTimeMillis());
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("TYPE", "USER_TOKEN");
    map.put("UUID", uuid);
    db.executeInsert("user_tokens", map);

    return uuid;
  }
}
