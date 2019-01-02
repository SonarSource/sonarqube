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
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateExternalIdOnUsersTest {

  private static final long PAST = 5_000_000_000L;
  private static final long NOW = 10_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateExternalIdOnUsersTest.class, "users.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateExternalIdOnUsers underTest = new PopulateExternalIdOnUsers(db.database(), system2);

  @Test
  public void update_users() throws SQLException {
    insertUser("USER_1", "user1", null);
    insertUser("USER_2", "user2", "1234");

    underTest.execute();

    assertUsers(
      tuple("USER_1", "user1", NOW),
      tuple("USER_2", "1234", PAST));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertUser("USER_1", "user1", null);
    insertUser("USER_2", "user2", "1234");

    underTest.execute();
    underTest.execute();

    assertUsers(
      tuple("USER_1", "user1", NOW),
      tuple("USER_2", "1234", PAST));
  }

  private void assertUsers(Tuple... expectedTuples) {
    assertThat(db.select("SELECT LOGIN, EXTERNAL_ID, UPDATED_AT FROM USERS")
      .stream()
      .map(map -> new Tuple(map.get("LOGIN"), map.get("EXTERNAL_ID"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertUser(String login, String externalLogin, @Nullable String externalId) {
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
