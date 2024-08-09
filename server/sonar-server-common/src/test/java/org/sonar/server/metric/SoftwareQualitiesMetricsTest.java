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
package org.sonar.server.metric;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoftwareQualitiesMetricsTest {
  @Test
  void getMetrics() {
    assertThat(new SoftwareQualitiesMetrics().getMetrics())
      .containsExactlyInAnyOrder(
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REVIEW_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING,
        SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO);
  }
}
