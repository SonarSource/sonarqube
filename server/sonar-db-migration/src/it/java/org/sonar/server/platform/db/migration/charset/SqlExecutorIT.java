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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class SqlExecutorIT {

  private static final String LOGIN_DB_COLUMN = "login";
  private static final String NAME_DB_COLUMN = "name";
  private static final String USERS_DB_TABLE = "users";
  private static final String IS_ROOT_DB_COLUMN = "is_root";

  private SqlExecutor underTest = new SqlExecutor();

  @RegisterExtension
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(SqlExecutorIT.class, "users_table.sql");

  @Test
  void executeSelect_executes_PreparedStatement() throws Exception {
    dbTester.executeInsert(USERS_DB_TABLE, LOGIN_DB_COLUMN, "login1", NAME_DB_COLUMN, "name one", IS_ROOT_DB_COLUMN, false);
    dbTester.executeInsert(USERS_DB_TABLE, LOGIN_DB_COLUMN, "login2", NAME_DB_COLUMN, "name two", IS_ROOT_DB_COLUMN, false);

    try (Connection connection = dbTester.openConnection()) {
      List<String[]> users = underTest.select(connection, "select " + LOGIN_DB_COLUMN + ", " + NAME_DB_COLUMN + " from users order by login", new SqlExecutor.StringsConverter(
        2));
      assertThat(users).hasSize(2);
      assertThat(users.get(0)[0]).isEqualTo("login1");
      assertThat(users.get(0)[1]).isEqualTo("name one");
      assertThat(users.get(1)[0]).isEqualTo("login2");
      assertThat(users.get(1)[1]).isEqualTo("name two");
    }
  }

  @Test
  void executeUpdate_executes_PreparedStatement() throws Exception {
    dbTester.executeInsert(USERS_DB_TABLE, LOGIN_DB_COLUMN, "the_login", NAME_DB_COLUMN, "the name", IS_ROOT_DB_COLUMN, false);

    try (Connection connection = dbTester.openConnection()) {
      underTest.executeDdl(connection, "update users set " + NAME_DB_COLUMN + "='new name' where " + LOGIN_DB_COLUMN + "='the_login'");
    }
    Map<String, Object> row = dbTester.selectFirst("select " + NAME_DB_COLUMN + " from users where " + LOGIN_DB_COLUMN + "='the_login'");
    assertThat(row)
      .isNotEmpty()
      .containsEntry("NAME", "new name");
  }

}
