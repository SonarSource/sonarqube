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

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_BRANCH_UUID;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_COMPONENT_UUID;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_JSON_VALUE;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_JSON_VALUE_HASH;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable.MEASURES_TABLE_NAME;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class CreateMeasuresTableTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createEmpty();

  private final DdlChange underTest = new CreateMeasuresTable(db.database());

  @Test
  public void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(MEASURES_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(MEASURES_TABLE_NAME);
    db.assertPrimaryKey(MEASURES_TABLE_NAME, "pk_measures", "component_uuid");
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_COMPONENT_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_BRANCH_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_JSON_VALUE, Types.CLOB, null, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_JSON_VALUE_HASH, Types.BIGINT, null, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(MEASURES_TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
  }

  @Test
  public void execute_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(MEASURES_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(MEASURES_TABLE_NAME);
  }
}
