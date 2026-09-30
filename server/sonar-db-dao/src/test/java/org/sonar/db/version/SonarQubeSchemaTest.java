/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.version;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.dialect.H2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SonarQubeSchemaTest {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  @Test
  void tables_match_h2_schema() throws SQLException {
    assumeTrue(H2.ID.equals(db.getDbClient().getDatabase().getDialect().getId()));

    Set<String> schemaTables = new HashSet<>();
    Connection connection = db.getSession().getConnection();
    try (ResultSet rs = connection.getMetaData().getTables(null, connection.getSchema(), "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        schemaTables.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
      }
    }

    assertThat(schemaTables)
      .as("Every table in the schema must be registered in SonarQubeSchema.TABLES so that DbTester truncates it between tests")
      .containsExactlyInAnyOrderElementsOf(SonarQubeSchema.TABLES);
  }
}
