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
package org.sonar.server.platform.db.migration.version.v84.usertokens;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddUuidColumnToUserTokensTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUuidColumnToUserTokensTest.class, "schema.sql");

  private DdlChange underTest = new AddUuidColumnToUserTokens(db.database());

  @Before
  public void setup() {
    insertUserToken(1L, "user1", "name1", "token1");
    insertUserToken(2L, "user2", "name2", "token2");
    insertUserToken(3L, "user3", "name3", "token3");
  }

  @Test
  public void add_uuid_column_to_user_tokens() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("user_tokens", "uuid", Types.VARCHAR, 40, true);

    assertThat(db.countSql("select count(id) from user_tokens"))
      .isEqualTo(3);
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
