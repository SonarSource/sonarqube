/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;

public class DropPeriodAndValueWarningColumnsFromQualityGateConditionsTableTest {

  private static final String TABLE_NAME = "quality_gate_conditions";
  private static final String COLUMN_NAME_PERIOD = "period";
  private static final String COLUMN_NAME_VALUE_WARNING = "value_warning";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(DropPeriodAndValueWarningColumnsFromQualityGateConditionsTableTest.class, "schema.sql");

  private DropPeriodAndValueWarningColumnsFromQualityGateConditionsTable underTest = new DropPeriodAndValueWarningColumnsFromQualityGateConditionsTable(dbTester.database());

  @Test
  public void column_has_been_dropped() throws SQLException {
    dbTester.assertColumnDefinition(TABLE_NAME, COLUMN_NAME_PERIOD, INTEGER, null, true);
    dbTester.assertColumnDefinition(TABLE_NAME, COLUMN_NAME_VALUE_WARNING, VARCHAR, null, true);
    underTest.execute();
    dbTester.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME_PERIOD);
    dbTester.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME_VALUE_WARNING);
  }

}
