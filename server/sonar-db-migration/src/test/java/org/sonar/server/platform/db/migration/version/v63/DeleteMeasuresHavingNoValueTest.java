/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v63;

import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteMeasuresHavingNoValueTest {

  private static final String TABLE_MEASURES = "project_measures";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteMeasuresHavingNoValueTest.class, "project_measures.sql");

  private DeleteMeasuresHavingNoValue underTest = new DeleteMeasuresHavingNoValue(db.database());

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isZero();
  }

  @Test
  public void migration_does_not_remove_measures_with_value() throws SQLException {
    insertMeasure(5d, null, null, null);
    insertMeasure(null, "text", null, null);
    insertMeasure(null, null, "data", null);
    insertMeasure(null, null, null, 50d);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(4);
  }

  @Test
  public void migration_removes_measures_with_no_values() throws SQLException {
    insertMeasure(null, null, null, null);
    insertMeasure(null, null, null, null);
    insertMeasure(null, null, null, null);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isZero();
  }

  @Test
  public void migration_does_not_remove_measures_having_variation_on_leak_period() throws SQLException {
    insertMeasureOnlyOnVariations(10d, null, null, null, null);
    insertMeasureOnlyOnVariations(11d, 2d, null, null, null);
    insertMeasureOnlyOnVariations(12d, null, 3d, 4d, 5d);
    insertMeasureOnlyOnVariations(12d, 2d, 3d, 4d, 5d);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(4);
  }

  @Test
  public void migration_removes_measures_having_only_variation_on_periods2_to_5() throws SQLException {
    insertMeasureOnlyOnVariations(null, 2d, null, null, null);
    insertMeasureOnlyOnVariations(null, null, 3d, null, null);
    insertMeasureOnlyOnVariations(null, null, null, 4d, null);
    insertMeasureOnlyOnVariations(null, null, null, null, 5d);
    insertMeasureOnlyOnVariations(null, 2d, 3d, 4d, 5d);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMeasure(5d, null, null, null);
    insertMeasure(null, "text", null, null);
    insertMeasure(null, null, null, null);
    insertMeasure(null, null, null, null);

    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(2);

    underTest.execute();
    assertThat(db.countRowsOfTable(TABLE_MEASURES)).isEqualTo(2);
  }

  private void insertMeasure(@Nullable Double value, @Nullable String textValue, @Nullable String data, @Nullable Double variation) {
    Map<String, Object> values = new HashMap<>(ImmutableMap.<String, Object>builder()
      .put("METRIC_ID", valueOf(10))
      .put("COMPONENT_UUID", randomAlphanumeric(10))
      .put("ANALYSIS_UUID", randomAlphanumeric(10))
      .put("VARIATION_VALUE_2", 2d)
      .put("VARIATION_VALUE_3", 3d)
      .put("VARIATION_VALUE_4", 4d)
      .put("VARIATION_VALUE_5", 5d)
      .build());
    if (value != null) {
      values.put("VALUE", valueOf(value));
    }
    if (textValue != null) {
      values.put("TEXT_VALUE", textValue);
    }
    if (variation != null) {
      values.put("VARIATION_VALUE_1", variation);
    }
    if (data != null) {
      values.put("MEASURE_DATA", data.getBytes(StandardCharsets.UTF_8));
    }
    db.executeInsert(TABLE_MEASURES, values);
  }

  private void insertMeasureOnlyOnVariations(@Nullable Double variation1, @Nullable Double variation2, @Nullable Double variation3, @Nullable Double variation4,
    @Nullable Double variation5) {
    Map<String, Object> values = new HashMap<>(ImmutableMap.of(
      "METRIC_ID", valueOf(20),
      "COMPONENT_UUID", randomAlphanumeric(10),
      "ANALYSIS_UUID", randomAlphanumeric(10)));
    if (variation1 != null) {
      values.put("VARIATION_VALUE_1", valueOf(variation1));
    }
    if (variation2 != null) {
      values.put("VARIATION_VALUE_2", valueOf(variation2));
    }
    if (variation3 != null) {
      values.put("VARIATION_VALUE_3", valueOf(variation3));
    }
    if (variation4 != null) {
      values.put("VARIATION_VALUE_4", valueOf(variation4));
    }
    if (variation5 != null) {
      values.put("VARIATION_VALUE_4", valueOf(variation5));
    }
    db.executeInsert(TABLE_MEASURES, values);
  }

}
