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
package org.sonar.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleIndexTestUtils {
  private OracleIndexTestUtils() {
    // utils only
  }

  /**
   * For Oracle, we only verify the index exists and is unique, without checking column names.
   * Oracle creates function-based indexes with auto-generated virtual column names (like sys_nc00014$)
   * when using CASE expressions, so we can't verify against the original column name.
   */
  public static void assertIndexExistsForOracle(MigrationDbTester db, boolean unique, String indexName, String tableName) {
    try (var connection = db.openConnection()) {
      try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName.toUpperCase(), false, false)) {
        boolean indexFound = false;
        while (rs.next()) {
          if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
            indexFound = true;
            if (unique) {
              assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should be unique").isFalse();
            } else {
              assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should not be unique").isTrue();
            }
            break;
          }
        }
        assertThat(indexFound).as("Index %s should exist", indexName).isTrue();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to check index", e);
    }
  }
}
