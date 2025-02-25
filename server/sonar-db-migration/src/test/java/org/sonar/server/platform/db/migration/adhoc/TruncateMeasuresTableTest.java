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
package org.sonar.server.platform.db.migration.adhoc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.db.DatabaseUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.MEASURES_TABLE_NAME;

public class TruncateMeasuresTableTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(TruncateMeasuresTableTest.class, "schema.sql");
  private final TruncateMeasuresTable underTest = new TruncateMeasuresTable(db.database());

  @Test
  public void execute_when_data_is_populated() throws SQLException {
    insertMeasure();
    assertThat(db.countRowsOfTable(MEASURES_TABLE_NAME)).isOne();
    underTest.execute();
    assertThat(db.countRowsOfTable(MEASURES_TABLE_NAME)).isZero();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  public void execute_when_table_does_not_exist_should_not_fail() throws SQLException {
    dropMeasuresTable();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  private void insertMeasure() {
    Map<String, Object> columnValuePairs = Map.of(
      "COMPONENT_UUID", "dc9e5eb4-3f29-4340-a891-65bdc98a2dcd",
      "BRANCH_UUID", "74a9e328-be25-4e47-b3cd-efffec865837",
      "JSON_VALUE", "{\"coverage\":\"80\"}",
      "JSON_VALUE_HASH", 841423045768002689L,
      "CREATED_AT", 1L,
      "UPDATED_AT", 1L
    );
    db.executeInsert(MEASURES_TABLE_NAME, columnValuePairs);
  }

  private void dropMeasuresTable() throws SQLException {
    try (Connection connection = db.database().getDataSource().getConnection()) {
      boolean tableExists = DatabaseUtils.tableExists(MEASURES_TABLE_NAME, connection);
      if (tableExists) {
        db.executeDdl("drop table " + MEASURES_TABLE_NAME);
      }
    }
  }
}
