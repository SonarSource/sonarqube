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
package org.sonar.server.qualitygate;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.db.metric.MetricDto;

class QualityGateModeCheckerTest {

  QualityGateModeChecker underTest = new QualityGateModeChecker();

  @Test
  void getUsageOfModeMetrics_shouldReturnNoResult_whenMetricsIsEmpty() {
    QualityGateModeChecker.QualityModeResult qualityModeResult = underTest.getUsageOfModeMetrics(List.of());
    Assertions.assertThat(qualityModeResult.hasMQRConditions()).isFalse();
    Assertions.assertThat(qualityModeResult.hasStandardConditions()).isFalse();
  }

  @Test
  void getUsageOfModeMetrics_shouldReturnExpectedResult_whenMetricsContainsStandardAndMQR() {
    QualityGateModeChecker.QualityModeResult qualityModeResult = underTest.getUsageOfModeMetrics(List.of(newMetric(CoreMetrics.RELIABILITY_RATING_KEY)));
    Assertions.assertThat(qualityModeResult.hasMQRConditions()).isFalse();
    Assertions.assertThat(qualityModeResult.hasStandardConditions()).isTrue();

    qualityModeResult = underTest.getUsageOfModeMetrics(List.of(newMetric(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES_KEY)));
    Assertions.assertThat(qualityModeResult.hasMQRConditions()).isTrue();
    Assertions.assertThat(qualityModeResult.hasStandardConditions()).isFalse();

    qualityModeResult = underTest.getUsageOfModeMetrics(List.of(newMetric(CoreMetrics.RELIABILITY_RATING_KEY),
      newMetric(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES_KEY)));
    Assertions.assertThat(qualityModeResult.hasMQRConditions()).isTrue();
    Assertions.assertThat(qualityModeResult.hasStandardConditions()).isTrue();
  }

  private static MetricDto newMetric(String metricKey) {
    return new MetricDto().setKey(metricKey);
  }

}
