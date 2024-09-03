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
package org.sonar.db.metric;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.measures.Metric;

import static com.google.common.base.Strings.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;

class MetricDtoTest {

  private final MetricDto underTest = new MetricDto();

  @Test
  void getters_and_setters() {
    MetricDto metricDto = new MetricDto()
      .setUuid("1")
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Coverage by unit tests")
      .setDomain("Tests")
      .setValueType("PERCENT")
      .setQualitative(true)
      .setWorstValue(0d)
      .setBestValue(100d)
      .setOptimizedBestValue(true)
      .setDirection(1)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setEnabled(true);

    assertThat(metricDto.getUuid()).isEqualTo("1");
    assertThat(metricDto.getKey()).isEqualTo("coverage");
    assertThat(metricDto.getShortName()).isEqualTo("Coverage");
    assertThat(metricDto.getDescription()).isEqualTo("Coverage by unit tests");
    assertThat(metricDto.getDomain()).isEqualTo("Tests");
    assertThat(metricDto.getValueType()).isEqualTo("PERCENT");
    assertThat(metricDto.isQualitative()).isTrue();
    assertThat(metricDto.getWorstValue()).isEqualTo(0d);
    assertThat(metricDto.getBestValue()).isEqualTo(100d);
    assertThat(metricDto.isOptimizedBestValue()).isTrue();
    assertThat(metricDto.getDirection()).isOne();
    assertThat(metricDto.isHidden()).isTrue();
    assertThat(metricDto.isDeleteHistoricalData()).isTrue();
    assertThat(metricDto.isEnabled()).isTrue();
  }

  @Test
  void fail_if_key_longer_than_64_characters() {
    String a65 = repeat("a", 65);

    assertThatThrownBy(() -> underTest.setKey(a65))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric key length (65) is longer than the maximum authorized (64). '" + a65 + "' was provided.");
  }

  @Test
  void fail_if_name_longer_than_64_characters() {
    String a65 = repeat("a", 65);

    assertThatThrownBy(() -> underTest.setShortName(a65))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric name length (65) is longer than the maximum authorized (64). '" + a65 + "' was provided.");
  }

  @Test
  void fail_if_description_longer_than_255_characters() {
    String a256 = repeat("a", 256);

    assertThatThrownBy(() -> underTest.setDescription(a256))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric description length (256) is longer than the maximum authorized (255). '" + a256 + "' was provided.");
  }

  @Test
  void fail_if_domain_longer_than_64_characters() {
    String a65 = repeat("a", 65);

    assertThatThrownBy(() -> underTest.setDomain(a65))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric domain length (65) is longer than the maximum authorized (64). '" + a65 + "' was provided.");
  }

  @ParameterizedTest
  @MethodSource("metric_types")
  void isNumeric_returns_true_for_numeric_types(Metric.ValueType type, boolean expected) {
    assertThat(underTest.setValueType(type.name()).isNumeric()).isEqualTo(expected);
  }

  private static Stream<Arguments> metric_types() {
    return Stream.of(
      Arguments.of(INT, true),
      Arguments.of(FLOAT, true),
      Arguments.of(PERCENT, true),
      Arguments.of(BOOL, true),
      Arguments.of(STRING, false),
      Arguments.of(MILLISEC, true),
      Arguments.of(DATA, false),
      Arguments.of(LEVEL, false),
      Arguments.of(DISTRIB, false),
      Arguments.of(RATING, true),
      Arguments.of(WORK_DUR, true)
    );
  }
}
