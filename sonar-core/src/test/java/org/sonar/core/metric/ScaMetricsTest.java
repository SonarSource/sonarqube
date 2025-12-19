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
package org.sonar.core.metric;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScaMetricsTest {
  @Test
  void scaMetricsKeys_thenContainsExpectedKeys() {
    // arguments are swapped to appease SonarQube rule
    assertThat(scaMetricKeyProvider()
      .map(args -> (String) args.get()[0])
      .toList()).containsExactlyInAnyOrderElementsOf(
        ScaMetrics.SCA_METRICS_KEYS);
  }

  @ParameterizedTest
  @MethodSource("scaMetricKeyProvider")
  void scaMetricKeys_thenHaveCorrectFormat(String actual, String expected) {
    assertThat(actual).isEqualTo(expected);
  }

  static Stream<Arguments> scaMetricKeyProvider() {
    return Stream.of(
      Arguments.of(ScaMetrics.NEW_SCA_RATING_ANY_ISSUE_KEY, "new_sca_rating_any_issue"),
      Arguments.of(ScaMetrics.NEW_SCA_RATING_VULNERABILITY_KEY, "new_sca_rating_vulnerability"),
      Arguments.of(ScaMetrics.NEW_SCA_RATING_LICENSING_KEY, "new_sca_rating_licensing"),
      Arguments.of(ScaMetrics.NEW_SCA_RATING_MALWARE_KEY, "new_sca_rating_malware"),
      Arguments.of(ScaMetrics.NEW_SCA_SEVERITY_ANY_ISSUE_KEY, "new_sca_severity_any_issue"),
      Arguments.of(ScaMetrics.NEW_SCA_SEVERITY_VULNERABILITY_KEY, "new_sca_severity_vulnerability"),
      Arguments.of(ScaMetrics.NEW_SCA_SEVERITY_LICENSING_KEY, "new_sca_severity_licensing"),
      Arguments.of(ScaMetrics.NEW_SCA_SEVERITY_MALWARE_KEY, "new_sca_severity_malware"),
      Arguments.of(ScaMetrics.NEW_SCA_COUNT_ANY_ISSUE_KEY, "new_sca_count_any_issue"),
      Arguments.of(ScaMetrics.SCA_RATING_ANY_ISSUE_KEY, "sca_rating_any_issue"),
      Arguments.of(ScaMetrics.SCA_RATING_VULNERABILITY_KEY, "sca_rating_vulnerability"),
      Arguments.of(ScaMetrics.SCA_RATING_LICENSING_KEY, "sca_rating_licensing"),
      Arguments.of(ScaMetrics.SCA_RATING_MALWARE_KEY, "sca_rating_malware"),
      Arguments.of(ScaMetrics.SCA_SEVERITY_ANY_ISSUE_KEY, "sca_severity_any_issue"),
      Arguments.of(ScaMetrics.SCA_SEVERITY_VULNERABILITY_KEY, "sca_severity_vulnerability"),
      Arguments.of(ScaMetrics.SCA_SEVERITY_LICENSING_KEY, "sca_severity_licensing"),
      Arguments.of(ScaMetrics.SCA_SEVERITY_MALWARE_KEY, "sca_severity_malware"),
      Arguments.of(ScaMetrics.SCA_COUNT_ANY_ISSUE_KEY, "sca_count_any_issue"));
  }
}
