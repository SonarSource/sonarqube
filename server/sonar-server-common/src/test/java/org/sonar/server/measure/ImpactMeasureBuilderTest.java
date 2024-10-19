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
package org.sonar.server.measure;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;

class ImpactMeasureBuilderTest {

  @Test
  void createEmptyMeasure_shouldReturnMeasureWithAllFields() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.createEmpty();
    assertThat(builder.buildAsMap())
      .containsAllEntriesOf(getImpactMap(0L, 0L, 0L, 0L, 0L, 0L));
  }

  @Test
  void fromMap_shouldInitializeCorrectlyTheBuilder() {
    Map<String, Long> map = getImpactMap(6L, 3L, 2L, 1L);
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromMap(map);

    map.put(INFO.name(), 0L);
    map.put(BLOCKER.name(), 0L);

    assertThat(builder.buildAsMap())
      .isEqualTo(map);
  }

  @Test
  void fromMap_whenMissingField_shouldThrowException() {
    Map<String, Long> map = Map.of();
    assertThatThrownBy(() -> ImpactMeasureBuilder.fromMap(map))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a total key");
  }

  @Test
  void toString_shouldInitializeCorrectlyTheBuilder() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromString("""
      {
        total: 6,
        HIGH: 3,
        MEDIUM: 2,
        LOW: 1
      }
      """);
    Map<String, Long> expectedMap = getImpactMap(6L, 3L, 2L, 1L, 0L, 0L);
    assertThat(builder.buildAsMap())
      .isEqualTo(expectedMap);
  }

  @Test
  void buildAsMap_whenIsEmpty_shouldThrowException() {
    ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.newInstance();
    assertThatThrownBy(impactMeasureBuilder::buildAsMap)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a total key");
  }

  @Test
  void setSeverity_shouldInitializeSeverityValues() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.newInstance()
      .setSeverity(HIGH, 3L)
      .setSeverity(MEDIUM, 2L)
      .setSeverity(LOW, 1L)
      .setSeverity(INFO, 4L)
      .setSeverity(BLOCKER, 5L)
      .setTotal(15L);
    assertThat(builder.buildAsMap())
      .isEqualTo(getImpactMap(15L, 3L, 2L, 1L, 4L, 5L));
  }

  @Test
  void add_shouldSumImpactsAndTotal() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromMap(getImpactMap(11L, 3L, 2L, 1L, 1L, 4L))
      .add(ImpactMeasureBuilder.newInstance()
        .setTotal(14L)
        .setSeverity(HIGH, 3L)
        .setSeverity(MEDIUM, 2L)
        .setSeverity(LOW, 1L)
        .setSeverity(INFO, 5L)
        .setSeverity(BLOCKER, 3L));
    assertThat(builder.buildAsMap())
      .isEqualTo(getImpactMap(25L, 6L, 4L, 2L, 6L, 7L));
  }

  @Test
  void add_whenOtherMapHasMissingField_shouldThrowException() {
    ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.newInstance();
    ImpactMeasureBuilder otherBuilder = ImpactMeasureBuilder.newInstance();
    assertThatThrownBy(() -> impactMeasureBuilder.add(otherBuilder))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a total key");
  }

  @Test
  void getTotal_shouldReturnExpectedTotal() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromMap(getImpactMap(6L, 3L, 2L, 1L));

    assertThat(builder.getTotal()).isEqualTo(6L);
  }

  private static Map<String, Long> getImpactMap(Long total, Long high, Long medium, Long low) {
    return new HashMap<>() {
      {
        put("total", total);
        put(HIGH.name(), high);
        put(MEDIUM.name(), medium);
        put(LOW.name(), low);
      }
    };
  }

  private static Map<String, Long> getImpactMap(Long total, Long high, Long medium, Long low, Long info, Long blocker) {
    Map<String, Long> impactMap = getImpactMap(total, high, medium, low);
    impactMap.put(INFO.name(), info);
    impactMap.put(BLOCKER.name(), blocker);
    return impactMap;
  }

}
