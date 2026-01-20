/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIndexOnComponentsLowerKee.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIndexOnComponentsLowerKee.TABLE_NAME;

class CreateIndexOnComponentsLowerKeeIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIndexOnComponentsLowerKee.class);
  private final DdlChange underTest = new CreateIndexOnComponentsLowerKee(db.database());

  @Test
  void execute_shouldCreateIndex() throws SQLException {
    assumeIndexSupported();
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();

    assertIndexCreated();
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    assumeIndexSupported();
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();
    underTest.execute();

    assertIndexCreated();
  }

  private void assumeIndexSupported() {
    String dialectId = db.database().getDialect().getId();
    assumeTrue(PostgreSql.ID.equals(dialectId) || Oracle.ID.equals(dialectId),
      "This migration only supports PostgreSQL and Oracle (MSSQL not implemented yet)");
  }

  /**
   * For PostgreSQL and Oracle, we only verify the index exists without checking column names.
   * Function-based indexes use database-specific representations:
   * - PostgreSQL: Reports the full expression like "lower((kee)::text)"
   * - Oracle: Creates auto-generated virtual columns like "sys_nc00027$"
   * The standard assertIndex method expects plain column names, so we verify existence only.
   */
  private void assertIndexCreated() {
    try (Connection connection = db.openConnection()) {
      try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, db.toVendorCase(TABLE_NAME), false, false)) {
        boolean indexFound = false;
        while (rs.next()) {
          if (INDEX_NAME.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
            indexFound = true;
            assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should be non-unique").isTrue();
            break;
          }
        }
        assertThat(indexFound).as("Index %s should exist", INDEX_NAME).isTrue();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to check index", e);
    }
  }
}
