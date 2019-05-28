/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FixDuplicationInExternalLoginOnUsersTest {

  private static final long PAST = 5_000_000_000L;
  private static final long NOW = 10_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(FixDuplicationInExternalLoginOnUsersTest.class, "users.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);

  private FixDuplicationInExternalLoginOnUsers underTest = new FixDuplicationInExternalLoginOnUsers(db.database(), system2);

  @Test
  public void fix_duplication() throws SQLException {
    insertUser("USER_1", "EXT_LOGIN_1", "EXT_LOGIN_1");
    insertUser("USER_2", "EXT_LOGIN_1", "EXT_LOGIN_1");
    insertUser("USER_3", "EXT_LOGIN_2", "EXT_LOGIN_2");
    insertUser("USER_4", "EXT_LOGIN_2", "EXT_LOGIN_2");
    insertUser("USER_5", "user5", "user5");

    underTest.execute();

    assertUsers(
      tuple("USER_1", "USER_1", "USER_1", NOW),
      tuple("USER_2", "USER_2", "USER_2", NOW),
      tuple("USER_3", "USER_3", "USER_3", NOW),
      tuple("USER_4", "USER_4", "USER_4", NOW),
      tuple("USER_5", "user5", "user5", PAST));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertUser("USER_1", "EXT_LOGIN", "EXT_LOGIN");
    insertUser("USER_2", "EXT_LOGIN", "EXT_LOGIN");

    underTest.execute();
    underTest.execute();

    assertUsers(
      tuple("USER_1", "USER_1", "USER_1", NOW),
      tuple("USER_2", "USER_2", "USER_2", NOW));
  }

  private void assertUsers(Tuple... expectedTuples) {
    assertThat(db.select("SELECT LOGIN, EXTERNAL_LOGIN, EXTERNAL_ID, UPDATED_AT FROM USERS")
      .stream()
      .map(map -> new Tuple(map.get("LOGIN"), map.get("EXTERNAL_LOGIN"), map.get("EXTERNAL_ID"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertUser(String login, String externalLogin, String externalId) {
    db.executeInsert("USERS",
      "LOGIN", login,
      "EXTERNAL_ID", externalId,
      "EXTERNAL_LOGIN", externalLogin,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST,
      "IS_ROOT", false,
      "ONBOARDED", false);
  }
}
