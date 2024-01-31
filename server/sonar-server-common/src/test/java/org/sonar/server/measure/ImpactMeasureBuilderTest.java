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

import java.util.Map;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImpactMeasureBuilderTest {

  @Test
  public void createEmptyMeasure_shouldReturnMeasureWithAllFields() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.createEmpty();
    assertThat(builder.buildAsMap())
      .containsAllEntriesOf(getImpactMap(0L, 0L, 0L, 0L));
  }

  private static Map<String, Long> getImpactMap(Long total, Long high, Long medium, Long low) {
    return Map.of("total", total, Severity.HIGH.name(), high, Severity.MEDIUM.name(), medium, Severity.LOW.name(), low);
  }

  @Test
  public void fromMap_shouldInitializeCorrectlyTheBuilder() {
    Map<String, Long> map = getImpactMap(6L, 3L, 2L, 1L);
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromMap(map);
    assertThat(builder.buildAsMap())
      .isEqualTo(map);
  }

  @Test
  public void fromMap_whenMissingField_shouldThrowException() {
    Map<String, Long> map = Map.of();
    assertThatThrownBy(() -> ImpactMeasureBuilder.fromMap(map))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a total key");
  }

  @Test
  public void toString_shouldInitializeCorrectlyTheBuilder() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromString("""
      {
        total: 6,
        HIGH: 3,
        MEDIUM: 2,
        LOW: 1
      }
      """);
    assertThat(builder.buildAsMap())
      .isEqualTo(getImpactMap(6L, 3L, 2L, 1L));
  }

  @Test
  public void buildAsMap_whenIsEmpty_shouldThrowException() {
    ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.newInstance();
    assertThatThrownBy(impactMeasureBuilder::buildAsMap)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a total key");
  }

  @Test
  public void buildAsMap_whenMissingSeverity_shouldThrowException() {
    ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.newInstance()
      .setTotal(1L)
      .setSeverity(Severity.HIGH, 1L)
      .setSeverity(Severity.MEDIUM, 1L);
    assertThatThrownBy(impactMeasureBuilder::buildAsMap)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a key for severity LOW");
  }

  @Test
  public void buildAsString_whenMissingSeverity_shouldThrowException() {
    ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.newInstance()
      .setTotal(1L)
      .setSeverity(Severity.HIGH, 1L)
      .setSeverity(Severity.MEDIUM, 1L);
    assertThatThrownBy(impactMeasureBuilder::buildAsString)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a key for severity LOW");
  }

  @Test
  public void setSeverity_shouldInitializeSeverityValues() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.newInstance()
      .setSeverity(Severity.HIGH, 3L)
      .setSeverity(Severity.MEDIUM, 2L)
      .setSeverity(Severity.LOW, 1L)
      .setTotal(6L);
    assertThat(builder.buildAsMap())
      .isEqualTo(getImpactMap(6L, 3L, 2L, 1L));
  }

  @Test
  public void add_shouldSumImpactsAndTotal() {
    ImpactMeasureBuilder builder = ImpactMeasureBuilder.fromMap(getImpactMap(6L, 3L, 2L, 1L))
      .add(ImpactMeasureBuilder.newInstance().setTotal(6L).setSeverity(Severity.HIGH, 3L).setSeverity(Severity.MEDIUM, 2L).setSeverity(Severity.LOW, 1L));
    assertThat(builder.buildAsMap())
      .isEqualTo(getImpactMap(12L, 6L, 4L, 2L));
  }

  @Test
  public void add_whenOtherMapHasMissingField_shouldThrowException() {
    ImpactMeasureBuilder impactMeasureBuilder = ImpactMeasureBuilder.newInstance();
    ImpactMeasureBuilder otherBuilder = ImpactMeasureBuilder.newInstance();
    assertThatThrownBy(() -> impactMeasureBuilder.add(otherBuilder))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Map must contain a total key");
  }

}
