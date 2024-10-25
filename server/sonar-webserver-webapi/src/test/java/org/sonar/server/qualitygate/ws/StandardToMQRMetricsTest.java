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
package org.sonar.server.qualitygate.ws;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

class StandardToMQRMetricsTest {

  @Test
  void isStandardMetric_shouldReturnExpectedResult() {
    Assertions.assertThat(StandardToMQRMetrics.isStandardMetric(CoreMetrics.RELIABILITY_RATING_KEY)).isTrue();
    Assertions.assertThat(StandardToMQRMetrics.isMQRMetric(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES_KEY)).isTrue();

    Assertions.assertThat(StandardToMQRMetrics.isMQRMetric(CoreMetrics.SECURITY_RATING_KEY)).isFalse();
    Assertions.assertThat(StandardToMQRMetrics.isStandardMetric(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY)).isFalse();
  }

  @Test
  void getEquivalentMetric_shouldReturnExpectedResult() {
    Assertions.assertThat(StandardToMQRMetrics.getEquivalentMetric(CoreMetrics.COMMENT_LINES_DENSITY_KEY)).isEmpty();
    Assertions.assertThat(StandardToMQRMetrics.getEquivalentMetric(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY)).hasValue(CoreMetrics.RELIABILITY_RATING_KEY);
    Assertions.assertThat(StandardToMQRMetrics.getEquivalentMetric(CoreMetrics.RELIABILITY_RATING_KEY)).hasValue(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY);
  }

}
