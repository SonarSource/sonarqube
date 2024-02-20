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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class SelectExecutorIT {

  @RegisterExtension
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(SelectExecutorIT.class, "users_table.sql");

  SqlExecutor underTest = new SqlExecutor();

  @Test
  void testExecuteQuery() throws Exception {
    insertUser("him", "Him");
    insertUser("her", "Her");

    try (Connection connection = dbTester.openConnection()) {
      List<String[]> rows = underTest.select(connection, "select login, name from users order by login", new SqlExecutor.StringsConverter(2));
      assertThat(rows).hasSize(2);
      assertThat(rows.get(0)[0]).isEqualTo("her");
      assertThat(rows.get(0)[1]).isEqualTo("Her");
      assertThat(rows.get(1)[0]).isEqualTo("him");
      assertThat(rows.get(1)[1]).isEqualTo("Him");
    }
  }

  private void insertUser(String login, String name) {
    dbTester.executeInsert("users",
      "LOGIN", login,
      "NAME", name);
  }
}
