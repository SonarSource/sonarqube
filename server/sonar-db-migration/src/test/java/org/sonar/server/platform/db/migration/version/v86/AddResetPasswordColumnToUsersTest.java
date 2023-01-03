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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import java.util.Objects;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static java.sql.Types.BOOLEAN;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class AddResetPasswordColumnToUsersTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddResetPasswordColumnToUsersTest.class, "schema.sql");

  private MigrationStep underTest = new AddResetPasswordColumnToUsers(db.database());

  @Test
  public void execute_on_empty_db() throws SQLException {
    db.assertColumnDoesNotExist("users", "reset_password");
    underTest.execute();
    db.assertColumnDefinition("users", "reset_password", BOOLEAN, null, true);
  }

  @Test
  public void execute_on_db_with_user_rows() throws SQLException {
    insertUser("uuid-1");
    insertUser("uuid-2");
    insertUser("uuid-3");
    db.assertColumnDoesNotExist("users", "reset_password");
    underTest.execute();
    db.assertColumnDefinition("users", "reset_password", BOOLEAN, null, true);

    assertThatAllUsersResetPasswordFlagAreNotSet();
  }

  private void assertThatAllUsersResetPasswordFlagAreNotSet() {
    assertThat(db.select("select reset_password from users")
      .stream()
      .map(r -> r.get("RESET_PASSWORD"))
      .map(this::getBooleanValue)
      .filter(Objects::nonNull)
      .collect(toList())).isEmpty();
  }

  private Boolean getBooleanValue(@Nullable Object value) {
    return value == null ? null : Boolean.parseBoolean(value.toString());
  }

  private void insertUser(String uuid) {
    db.executeInsert("users",
      "UUID", uuid,
      "LOGIN", uuid + "login",
      "EXTERNAL_LOGIN", uuid + "-external-login",
      "EXTERNAL_IDENTITY_PROVIDER", "sonarqube",
      "EXTERNAL_ID", uuid + "-external-id",
      "IS_ROOT", false,
      "ONBOARDED", false);
  }
}
