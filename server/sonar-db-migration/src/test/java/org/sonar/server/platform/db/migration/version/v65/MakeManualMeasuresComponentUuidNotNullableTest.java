/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

public class MakeManualMeasuresComponentUuidNotNullableTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeManualMeasuresComponentUuidNotNullableTest.class, "manual_measures.sql");

  private final Random random = new Random();
  private MakeManualMeasuresComponentUuidNotNullable underTest = new MakeManualMeasuresComponentUuidNotNullable(db.database());

  @Test
  public void execute_makes_column_component_uuid_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumn();
  }

  @Test
  public void execute_makes_column_component_uuid_not_nullable_on_populated_table() throws SQLException {
    insertManualMeasure();
    insertManualMeasure();
    insertManualMeasure();

    underTest.execute();

    verifyColumn();
  }

  private void verifyColumn() {
    db.assertColumnDefinition("manual_measures", "component_uuid", Types.VARCHAR, 50, false);
  }

  private void insertManualMeasure() {
    db.executeInsert(
      "manual_measures",
      "METRIC_ID", random.nextInt(),
      "COMPONENT_UUID", randomAlphabetic(5));
  }
}
