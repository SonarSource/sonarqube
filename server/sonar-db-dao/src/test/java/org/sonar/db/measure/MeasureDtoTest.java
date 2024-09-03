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
package org.sonar.db.measure;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

class MeasureDtoTest {

  @ParameterizedTest
  @MethodSource("valuesOfDifferentTypes")
  void getString_returns_string_value(Object value) {
    String metricKey = randomAlphabetic(7);
    MeasureDto measureDto = new MeasureDto().addValue(metricKey, value);
    assertThat(measureDto.getString(metricKey)).isEqualTo(String.valueOf(value));
  }

  private static List<Object> valuesOfDifferentTypes() {
    return List.of(2, 3.14, "foo");
  }

  @Test
  void getString_returns_null_for_nonexistent_metric() {
    String metricKey = randomAlphabetic(7);
    MeasureDto measureDto = new MeasureDto();
    assertThat(measureDto.getString(metricKey)).isNull();
  }

  @Test
  void compute_json_value_hash() {
    MeasureDto measureDto = new MeasureDto();
    measureDto.setJsonValue("{\"key\":\"value\"}");
    assertThat(measureDto.getJsonValueHash()).isNull();
    assertThat(measureDto.computeJsonValueHash()).isEqualTo(2887272982314571750L);
    assertThat(measureDto.getJsonValueHash()).isEqualTo(2887272982314571750L);
  }

  @Test
  void getMetricValues_returns_all_values_ordered() {
    MeasureDto measureDto = new MeasureDto()
      .addValue("string-metric", "value")
      .addValue("int-metric", 1)
      .addValue("long-metric", 2L);
    assertThat(measureDto.getMetricValues()).containsExactlyEntriesOf(new TreeMap<>(Map.of(
      "int-metric", 1,
      "long-metric", 2L,
      "string-metric", "value"
    )));
  }

  @Test
  void toString_prints_all_fields() {
    MeasureDto measureDto = new MeasureDto()
      .setBranchUuid("branch-uuid")
      .setComponentUuid("component-uuid")
      .addValue("int-metric", 12)
      .addValue("double-metric", 34.5)
      .addValue("boolean-metric", true)
      .addValue("string-metric", "value");
    measureDto.computeJsonValueHash();

    assertThat(measureDto).hasToString("MeasureDto{componentUuid='component-uuid'" +
      ", branchUuid='branch-uuid'" +
      ", metricValues={boolean-metric=true, double-metric=34.5, int-metric=12, string-metric=value}" +
      ", jsonValueHash=-1071134275520515337}");
  }
}
