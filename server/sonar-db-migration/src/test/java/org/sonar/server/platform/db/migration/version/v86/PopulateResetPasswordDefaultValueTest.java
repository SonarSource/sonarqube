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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateResetPasswordDefaultValueTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateResetPasswordDefaultValueTest.class, "schema.sql");

  private final TestSystem2 system2 = new TestSystem2();
  private final long NOW = 1606375781L;

  private MigrationStep underTest = new PopulateResetPasswordDefaultValue(db.database(), system2);

  @Before
  public void before() {
    system2.setNow(NOW);
  }

  @Test
  public void execute_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("users")).isZero();
  }

  @Test
  public void execute_on_db_with_user_rows() throws SQLException {
    insertUser("uuid-1", null, NOW);
    insertUser("uuid-2", null, NOW);
    insertUser("uuid-3", null, NOW);

    long expectedUpdatedRowTime = 2_000_000_000L;
    system2.setNow(expectedUpdatedRowTime);

    underTest.execute();

    assertThatUserResetPasswordFlagIsEqualTo(
      tuple("uuid-1", false, expectedUpdatedRowTime),
      tuple("uuid-2", false, expectedUpdatedRowTime),
      tuple("uuid-3", false, expectedUpdatedRowTime));
  }

  @Test
  public void does_not_update_already_set_flags() throws SQLException {
    insertUser("uuid-1", true, NOW);
    insertUser("uuid-2", false, NOW);
    insertUser("uuid-3", null, NOW);

    long expectedUpdatedRowTime = 2_000_000_000L;
    system2.setNow(expectedUpdatedRowTime);

    underTest.execute();

    insertUser("uuid-4", null, NOW);
    // re-entrant
    underTest.execute();

    assertThatUserResetPasswordFlagIsEqualTo(
      tuple("uuid-1", true, NOW),
      tuple("uuid-2", false, NOW),
      tuple("uuid-3", false, expectedUpdatedRowTime),
      tuple("uuid-4", false, expectedUpdatedRowTime));
  }

  private void assertThatUserResetPasswordFlagIsEqualTo(Tuple... tuples) {
    assertThat(db.select("select uuid, reset_password, updated_at from users")
      .stream()
      .map(r -> tuple(r.get("UUID"), getBooleanValue(r.get("RESET_PASSWORD")), r.get("UPDATED_AT")))
      .collect(toList()))
        .containsExactlyInAnyOrder(tuples);
  }

  private Boolean getBooleanValue(@Nullable Object value) {
    return value == null ? null : Boolean.parseBoolean(value.toString());
  }

  private void insertUser(String uuid, @Nullable Boolean resetPassword, long updatedAt) {
    db.executeInsert("users",
      "UUID", uuid,
      "LOGIN", uuid + "login",
      "EXTERNAL_LOGIN", uuid + "-external-login",
      "EXTERNAL_IDENTITY_PROVIDER", "sonarqube",
      "EXTERNAL_ID", uuid + "-external-id",
      "IS_ROOT", false,
      "RESET_PASSWORD", resetPassword,
      "ONBOARDED", false,
      "UPDATED_AT", updatedAt);
  }
}
