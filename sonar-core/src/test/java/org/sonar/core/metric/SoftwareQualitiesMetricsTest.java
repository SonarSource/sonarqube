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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoftwareQualitiesMetricsTest {
  @Test
  void getMetrics() {
    assertThat(new SoftwareQualitiesMetrics().getMetrics())
      .containsExactlyInAnyOrder(
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING,
        SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_BLOCKER_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_HIGH_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MEDIUM_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_LOW_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_LOW_ISSUES,
        SoftwareQualitiesMetrics.SOFTWARE_QUALITY_INFO_ISSUES,
        SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_INFO_ISSUES
      );
  }
}
