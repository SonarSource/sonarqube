/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog.service;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.server.v2.api.dop.jfrog.response.GateCondition;
import org.sonar.server.v2.api.dop.jfrog.response.GateStatus;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubePredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QualityGateDetailsParserTest {

  @Test
  void parse_whenEmpty_shouldReturnNoneStatus() {
    SonarQubePredicate result = QualityGateDetailsParser.parse(null);

    assertThat(result.gates()).hasSize(1);
    assertThat(result.gates().get(0).status()).isEqualTo(GateStatus.NONE);
    assertThat(result.gates().get(0).conditions()).isEmpty();
    assertThat(result.gates().get(0).ignoredConditions()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("statusTestCases")
  void parse_shouldParseStatus(String level, GateStatus expectedStatus) {
    String json = """
      {
        "level": "%s",
        "conditions": [],
        "ignoredConditions": false
      }
      """.formatted(level);

    SonarQubePredicate result = QualityGateDetailsParser.parse(json);

    assertThat(result.gates().get(0).status()).isEqualTo(expectedStatus);
  }

  static Stream<Arguments> statusTestCases() {
    return Stream.of(
      Arguments.of("OK", GateStatus.OK),
      Arguments.of("ERROR", GateStatus.ERROR),
      Arguments.of("WARN", GateStatus.WARN),
      Arguments.of("UNKNOWN", GateStatus.NONE));
  }

  @Test
  void parse_shouldParseIgnoredConditions() {
    String json = """
      {
        "level": "OK",
        "conditions": [],
        "ignoredConditions": true
      }
      """;

    SonarQubePredicate result = QualityGateDetailsParser.parse(json);

    assertThat(result.gates().get(0).ignoredConditions()).isTrue();
  }

  @Test
  void parse_shouldParseConditions() {
    String json = """
      {
        "level": "ERROR",
        "conditions": [
          {
            "metric": "new_coverage",
            "op": "LT",
            "error": "80",
            "actual": "75",
            "level": "ERROR"
          }
        ],
        "ignoredConditions": false
      }
      """;

    SonarQubePredicate result = QualityGateDetailsParser.parse(json);

    assertThat(result.gates().get(0).conditions()).hasSize(1);
    GateCondition condition = result.gates().get(0).conditions().get(0);
    assertThat(condition.metricKey()).isEqualTo("new_coverage");
    assertThat(condition.comparator()).isEqualTo(GateCondition.Comparator.LT);
    assertThat(condition.errorThreshold()).isEqualTo("80");
    assertThat(condition.actualValue()).isEqualTo("75");
    assertThat(condition.status()).isEqualTo(GateStatus.ERROR);
  }

  @ParameterizedTest
  @MethodSource("comparatorTestCases")
  void parse_shouldParseComparators(String op, GateCondition.Comparator expectedComparator) {
    String json = """
      {
        "level": "OK",
        "conditions": [
          {
            "metric": "test",
            "op": "%s",
            "level": "OK"
          }
        ],
        "ignoredConditions": false
      }
      """.formatted(op);

    SonarQubePredicate result = QualityGateDetailsParser.parse(json);

    assertThat(result.gates().get(0).conditions().get(0).comparator()).isEqualTo(expectedComparator);
  }

  static Stream<Arguments> comparatorTestCases() {
    return Stream.of(
      Arguments.of("LT", GateCondition.Comparator.LT),
      Arguments.of("GT", GateCondition.Comparator.GT),
      Arguments.of("EQ", GateCondition.Comparator.EQ),
      Arguments.of("NE", GateCondition.Comparator.NE));
  }

  @Test
  void parse_shouldThrowOnUnknownComparator() {
    String json = """
      {
        "level": "OK",
        "conditions": [
          {
            "metric": "test",
            "op": "UNKNOWN",
            "level": "OK"
          }
        ],
        "ignoredConditions": false
      }
      """;

    assertThatThrownBy(() -> QualityGateDetailsParser.parse(json))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Unknown comparator: UNKNOWN");
  }

  @Test
  void parse_shouldSkipConditionsOnNonLeakPeriod() {
    String json = """
      {
        "level": "OK",
        "conditions": [
          {
            "metric": "coverage",
            "op": "LT",
            "level": "OK",
            "period": 0
          },
          {
            "metric": "new_coverage",
            "op": "LT",
            "level": "OK",
            "period": 1
          }
        ],
        "ignoredConditions": false
      }
      """;

    SonarQubePredicate result = QualityGateDetailsParser.parse(json);

    assertThat(result.gates().get(0).conditions()).hasSize(1);
    assertThat(result.gates().get(0).conditions().get(0).metricKey()).isEqualTo("new_coverage");
  }

  @Test
  void parse_shouldHandleNullConditionsArray() {
    String json = """
      {
        "level": "OK",
        "ignoredConditions": false
      }
      """;

    SonarQubePredicate result = QualityGateDetailsParser.parse(json);

    assertThat(result.gates().get(0).conditions()).isEmpty();
  }

}
