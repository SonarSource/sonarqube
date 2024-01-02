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
package org.sonar.server.platform.db.migration.version.v98;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class MoveLiveMeasureVariationToValueTest {
  private static final String TABLE = "live_measures";
  private int counter = 0;

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MoveLiveMeasureVariationToValueTest.class, "schema.sql");

  private final DataChange underTest = new MoveLiveMeasureVariationToValue(db.database());

  @Test
  public void moves_value_to_value_if_value_is_null() throws SQLException {
    insertMeasure("1", null, 1000D);
    insertMeasure("2", null, 2000D);

    underTest.execute();

    List<Map<String, Object>> select = db.select("select uuid as \"UUID\", value as \"VALUE\", variation as \"VARIATION\" from live_measures");
    assertThat(select)
      .extracting(t -> t.get("UUID"), t -> t.get("VALUE"), t -> t.get("VARIATION"))
      .containsOnly(tuple("1", 1000D, 1000D), tuple("2", 2000D, 2000D));
  }

  @Test
  public void does_not_move_if_row_contains_value() throws SQLException {
    insertMeasure("1", 1100D, 1000D);
    insertMeasure("2", 2100D, 2000D);

    underTest.execute();

    List<Map<String, Object>> select = db.select("select uuid as \"UUID\", value as \"VALUE\", variation as \"VARIATION\" from live_measures");
    assertThat(select)
      .extracting(t -> t.get("UUID"), t -> t.get("VALUE"), t -> t.get("VARIATION"))
      .containsOnly(tuple("1", 1100D, 1000D), tuple("2", 2100D, 2000D));
  }

  private void insertMeasure(String uuid, @Nullable Double value, @Nullable Double variation) {
    db.executeInsert(TABLE,
      "uuid", uuid,
      "project_uuid", "p1",
      "component_uuid", "c1",
      "metric_uuid", counter++,
      "value", value,
      "variation", variation,
      "created_at", counter++,
      "updated_at", counter++);
  }
}
