/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScaMetricsTest {
  @Test
  void scaMetricsKeys_thenContainsExpectedKeys() {
    assertThat(ScaMetrics.SCA_METRICS_KEYS).containsExactlyInAnyOrder(
      ScaMetrics.NEW_SCA_COUNT_ANY_ISSUE_KEY,
      ScaMetrics.NEW_SCA_SEVERITY_ANY_ISSUE_KEY,
      ScaMetrics.NEW_SCA_SEVERITY_VULNERABILITY_KEY,
      ScaMetrics.NEW_SCA_SEVERITY_LICENSING_KEY,
      ScaMetrics.NEW_SCA_RATING_ANY_ISSUE_KEY,
      ScaMetrics.NEW_SCA_RATING_VULNERABILITY_KEY,
      ScaMetrics.NEW_SCA_RATING_LICENSING_KEY,
      ScaMetrics.SCA_COUNT_ANY_ISSUE_KEY,
      ScaMetrics.SCA_SEVERITY_ANY_ISSUE_KEY,
      ScaMetrics.SCA_SEVERITY_VULNERABILITY_KEY,
      ScaMetrics.SCA_SEVERITY_LICENSING_KEY,
      ScaMetrics.SCA_RATING_ANY_ISSUE_KEY,
      ScaMetrics.SCA_RATING_VULNERABILITY_KEY,
      ScaMetrics.SCA_RATING_LICENSING_KEY);
  }

  @Test
  void scaMetricKeys_thenHaveCorrectFormat() {
    assertThat(ScaMetrics.NEW_SCA_RATING_LICENSING_KEY).isEqualTo("new_sca_rating_licensing");
    assertThat(ScaMetrics.NEW_SCA_RATING_VULNERABILITY_KEY).isEqualTo("new_sca_rating_vulnerability");
    assertThat(ScaMetrics.NEW_SCA_RATING_ANY_ISSUE_KEY).isEqualTo("new_sca_rating_any_issue");
    assertThat(ScaMetrics.NEW_SCA_SEVERITY_LICENSING_KEY).isEqualTo("new_sca_severity_licensing");
    assertThat(ScaMetrics.NEW_SCA_SEVERITY_VULNERABILITY_KEY).isEqualTo("new_sca_severity_vulnerability");
    assertThat(ScaMetrics.NEW_SCA_SEVERITY_ANY_ISSUE_KEY).isEqualTo("new_sca_severity_any_issue");
    assertThat(ScaMetrics.NEW_SCA_COUNT_ANY_ISSUE_KEY).isEqualTo("new_sca_count_any_issue");
    assertThat(ScaMetrics.SCA_RATING_LICENSING_KEY).isEqualTo("sca_rating_licensing");
    assertThat(ScaMetrics.SCA_RATING_VULNERABILITY_KEY).isEqualTo("sca_rating_vulnerability");
    assertThat(ScaMetrics.SCA_RATING_ANY_ISSUE_KEY).isEqualTo("sca_rating_any_issue");
    assertThat(ScaMetrics.SCA_SEVERITY_LICENSING_KEY).isEqualTo("sca_severity_licensing");
    assertThat(ScaMetrics.SCA_SEVERITY_VULNERABILITY_KEY).isEqualTo("sca_severity_vulnerability");
    assertThat(ScaMetrics.SCA_SEVERITY_ANY_ISSUE_KEY).isEqualTo("sca_severity_any_issue");
    assertThat(ScaMetrics.SCA_COUNT_ANY_ISSUE_KEY).isEqualTo("sca_count_any_issue");
  }
}
