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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class PopulateUsersOnboardedTest {

  private final static long PAST = 100_000_000_000L;
  private final static long NOW = 500_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUsersOnboardedTest.class, "users_with_onboarded_column.sql");

  public PopulateUsersOnboarded underTest = new PopulateUsersOnboarded(db.database(), system2);

  @Test
  public void set_onboarded_to_true() throws SQLException {
    insertUser("admin");
    insertUser("user");
    assertUsers(tuple("admin", false, PAST), tuple("user", false, PAST));

    underTest.execute();

    assertUsers(tuple("admin", true, NOW), tuple("user", true, NOW));
  }

  private void insertUser(String login) {
    db.executeInsert("USERS", "LOGIN", login, "ONBOARDED", false, "IS_ROOT", true, "CREATED_AT", PAST, "UPDATED_AT", PAST);
  }

  private void assertUsers(Tuple... expectedTuples) {
    assertThat(db.select("SELECT LOGIN, ONBOARDED, UPDATED_AT FROM USERS")
      .stream()
      .map(map -> new Tuple(map.get("LOGIN"), map.get("ONBOARDED"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }
}
