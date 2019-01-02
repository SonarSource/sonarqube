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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateTableLiveMeasuresTest {
  private static final String TABLE = "live_measures";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateTableLiveMeasuresTest.class, "empty.sql");

  private CreateTableLiveMeasures underTest = new CreateTableLiveMeasures(db.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(0);

    db.assertColumnDefinition(TABLE, "uuid", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "project_uuid", Types.VARCHAR, 50, false);
    db.assertColumnDefinition(TABLE, "component_uuid", Types.VARCHAR, 50, false);
    db.assertColumnDefinition(TABLE, "metric_id", Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE, "value", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE, "text_value", Types.VARCHAR, 4_000, true);
    db.assertColumnDefinition(TABLE, "variation", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE, "measure_data", Types.BLOB, null, true);
    db.assertColumnDefinition(TABLE, "update_marker", Types.VARCHAR, 40, true);
    db.assertColumnDefinition(TABLE, "created_at", Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE, "updated_at", Types.BIGINT, null, false);

    db.assertIndex(TABLE, "live_measures_project", "project_uuid");
    db.assertUniqueIndex(TABLE, "live_measures_component", "component_uuid", "metric_id");
  }
}
