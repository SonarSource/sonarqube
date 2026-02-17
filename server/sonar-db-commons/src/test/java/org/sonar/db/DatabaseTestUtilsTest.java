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
package org.sonar.db;

import java.util.Set;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseTestUtilsTest {

  @Rule
  public H2DbTester dbTester = H2DbTester.createForSchema(DatabaseTestUtilsTest.class, "schema-with-tables.sql", false);

  @Test
  public void loadTableNames_shouldLoadTablesFromH2Database() {
    Set<String> tableNames = DatabaseTestUtils.loadTableNames(dbTester.getDb().getDatabase().getDataSource());

    // Should contain the test tables from schema
    assertThat(tableNames)
      .isNotEmpty()
      .contains("test_table", "another_table");
  }

  @Test
  public void loadTableNames_shouldFilterFlywayMigrationTables() {
    Set<String> tableNames = DatabaseTestUtils.loadTableNames(dbTester.getDb().getDatabase().getDataSource());

    // Schema contains flyway_schema_history which should be filtered out
    assertThat(tableNames)
      .noneMatch(name -> name.startsWith("flyway_"))
      .doesNotContain("flyway_schema_history");
  }

  @Test
  public void loadTableNames_shouldFilterSchemaMigrationsTable() {
    Set<String> tableNames = DatabaseTestUtils.loadTableNames(dbTester.getDb().getDatabase().getDataSource());

    // Schema contains schema_migrations which should be filtered out
    assertThat(tableNames)
      .isNotEmpty()
      .doesNotContain("schema_migrations");
  }

  @Test
  public void loadTableNames_shouldReturnLowercaseTableNames() {
    Set<String> tableNames = DatabaseTestUtils.loadTableNames(dbTester.getDb().getDatabase().getDataSource());

    // All table names should be lowercase
    assertThat(tableNames)
      .isNotEmpty()
      .allMatch(name -> name.equals(name.toLowerCase()));
  }
}
