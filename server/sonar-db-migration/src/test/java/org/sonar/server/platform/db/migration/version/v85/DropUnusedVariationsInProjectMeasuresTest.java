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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class DropUnusedVariationsInProjectMeasuresTest {
  private static final String TABLE_NAME = "project_measures";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropUnusedVariationsInProjectMeasuresTest.class, "schema.sql");

  private MigrationStep underTest = new DropUnusedVariationsInProjectMeasures(db.database());

  @Test
  public void drops_table() throws SQLException {
    insertData();
    db.assertColumnDefinition(TABLE_NAME, "variation_value_2", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE_NAME, "variation_value_3", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE_NAME, "variation_value_4", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE_NAME, "variation_value_5", Types.DOUBLE, null, true);

    underTest.execute();
    db.assertColumnDoesNotExist(TABLE_NAME, "variation_value_2");
    db.assertColumnDoesNotExist(TABLE_NAME, "variation_value_3");
    db.assertColumnDoesNotExist(TABLE_NAME, "variation_value_4");
    db.assertColumnDoesNotExist(TABLE_NAME, "variation_value_5");
    assertThat(db.selectFirst("select * from project_measures")).contains(entry("VARIATION_VALUE_1", 1.0));

  }

  private void insertData() {
    db.executeInsert(TABLE_NAME,
      "uuid", "uuid1",
      "analysis_uuid", "analysis1",
      "component_uuid", "component1",
      "metric_uuid", "metric1",
      "variation_value_1", 1.0,
      "variation_value_2", 2.0,
      "variation_value_3", 3.0,
      "variation_value_4", 4.0,
      "variation_value_5", 5.0
    );
  }
}
