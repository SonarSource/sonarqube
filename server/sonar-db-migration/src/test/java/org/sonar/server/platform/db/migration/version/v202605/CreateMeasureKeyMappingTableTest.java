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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.sonar.server.platform.db.migration.version.v202605.CreateMeasureKeyMappingTable.TABLE_NAME;

class CreateMeasureKeyMappingTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateMeasureKeyMappingTable.class);

  private final CreateMeasureKeyMappingTable underTest = new CreateMeasureKeyMappingTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_measure_key_mapping", "id");
    int smallIntType = Oracle.ID.equals(Objects.requireNonNull(db.database().getDialect()).getId()) ? Types.NUMERIC : Types.SMALLINT;
    db.assertColumnDefinition(TABLE_NAME, "id", smallIntType, null, false);
    db.assertColumnDefinition(TABLE_NAME, "metric_name", Types.VARCHAR, 64, false);
    db.assertColumnDefinition(TABLE_NAME, "metric_type", Types.VARCHAR, 64, true);
    db.assertUniqueIndex(TABLE_NAME, "msr_key_mapping_name_uq_idx", "metric_name");
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
