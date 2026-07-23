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
package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class RenameTableChangeImplTest {

  private static final String OLD_TABLE_NAME = "old_table";
  private static final String NEW_TABLE_NAME = "new_table";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createEmpty();

  @Test
  void execute_shouldRenameTableAndKeepExistingData() throws SQLException {
    db.executeDdl("CREATE TABLE " + OLD_TABLE_NAME + " (id VARCHAR(50) NOT NULL)");
    db.executeInsert(OLD_TABLE_NAME, "id", "row-1");

    underTest(OLD_TABLE_NAME, NEW_TABLE_NAME).execute();

    db.assertTableDoesNotExist(OLD_TABLE_NAME);
    db.assertTableExists(NEW_TABLE_NAME);
    assertThatRowExists("row-1");
  }

  @Test
  void execute_whenOldTableDoesNotExist_shouldDoNothing() throws SQLException {
    db.assertTableDoesNotExist(OLD_TABLE_NAME);
    db.assertTableDoesNotExist(NEW_TABLE_NAME);

    underTest(OLD_TABLE_NAME, NEW_TABLE_NAME).execute();

    db.assertTableDoesNotExist(OLD_TABLE_NAME);
    db.assertTableDoesNotExist(NEW_TABLE_NAME);
  }

  @Test
  void execute_whenNewTableAlreadyExists_shouldNotRenameAndKeepOldTableUntouched() throws SQLException {
    db.executeDdl("CREATE TABLE " + OLD_TABLE_NAME + " (id VARCHAR(50) NOT NULL)");
    db.executeInsert(OLD_TABLE_NAME, "id", "row-1");
    db.executeDdl("CREATE TABLE " + NEW_TABLE_NAME + " (id VARCHAR(50) NOT NULL)");

    underTest(OLD_TABLE_NAME, NEW_TABLE_NAME).execute();

    db.assertTableExists(OLD_TABLE_NAME);
    db.assertTableExists(NEW_TABLE_NAME);
    assertThat(db.select("SELECT id FROM " + OLD_TABLE_NAME))
      .extracting(row -> row.get("ID"))
      .containsExactly("row-1");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.executeDdl("CREATE TABLE " + OLD_TABLE_NAME + " (id VARCHAR(50) NOT NULL)");

    RenameTableChange underTest = underTest(OLD_TABLE_NAME, NEW_TABLE_NAME);
    underTest.execute();
    underTest.execute();

    db.assertTableDoesNotExist(OLD_TABLE_NAME);
    db.assertTableExists(NEW_TABLE_NAME);
  }

  private void assertThatRowExists(String id) {
    assertThat(db.select("SELECT id FROM " + NEW_TABLE_NAME))
      .extracting(row -> row.get("ID"))
      .containsExactly(id);
  }

  private RenameTableChange underTest(String oldTableName, String newTableName) {
    return new RenameTableChange(db.database(), oldTableName, newTableName) {
    };
  }
}
