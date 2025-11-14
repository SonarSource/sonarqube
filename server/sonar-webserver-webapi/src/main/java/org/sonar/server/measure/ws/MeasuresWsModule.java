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
package org.sonar.server.measure.ws;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.ScaMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.core.platform.Module;
import org.sonar.server.telemetry.TelemetryPortfolioActivityGraphTypeProvider;
import org.sonar.server.telemetry.TelemetryPortfolioActivityRequestedMetricProvider;

public class MeasuresWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      MeasuresWs.class,
      ComponentTreeAction.class,
      ComponentAction.class,
      SearchAction.class,
      SearchHistoryAction.class,
      // Telemetry
      TelemetryPortfolioActivityGraphTypeProvider.class,
      TelemetryPortfolioActivityRequestedMetricProvider.class
    );
  }

  public static String getDeprecatedMetricsInSonarQube93() {
    return "'" + String.join("', '", "releasability_effort", "security_rating_effort", "reliability_rating_effort", "security_review_rating_effort",
      "maintainability_rating_effort", "last_change_on_maintainability_rating", "last_change_on_releasability_rating", "last_change_on_reliability_rating",
      "last_change_on_security_rating", "last_change_on_security_review_rating") + "'";
  }

  public static String getDeprecatedMetricsInSonarQube104() {
    return "'" + String.join("', '", "bugs", "new_bugs", "vulnerabilities", "new_vulnerabilities",
      "code_smells", "new_code_smells", "high_impact_accepted_issues") + "'";
  }

  public static String getDeprecatedMetricsInSonarQube105() {
    return "'" + String.join("', '", "new_blocker_violations", "new_critical_violations", "new_major_violations", "new_minor_violations",
      "new_info_violations", "blocker_violations", "critical_violations", "major_violations", "minor_violations", "info_violations") + "'";
  }

  public static String getNewMetricsInSonarQube107() {
    return Stream.of(
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
      SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A,

      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT)
      .map(e -> "'" + e.getKey() + "'")
      .collect(Collectors.joining(", "));
  }

  public static String getNewMetricsInSonarQube108() {
    return Stream.of(
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_BLOCKER_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_INFO_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MEDIUM_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_LOW_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES,
      SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES,

      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_HIGH_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_INFO_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_LOW_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES,
      SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES)
      .map(e -> "'" + e.getKey() + "'")
      .collect(Collectors.joining(", "));
  }

  public static String getDeprecatedMetricsInSonarQube108() {
    return Stream.of(
      CoreMetrics.MAINTAINABILITY_ISSUES_KEY,
      CoreMetrics.RELIABILITY_ISSUES_KEY,
      CoreMetrics.SECURITY_ISSUES_KEY,

      CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY,
      CoreMetrics.NEW_RELIABILITY_ISSUES_KEY,
      CoreMetrics.NEW_SECURITY_ISSUES_KEY)
      .map(e -> "'" + e + "'")
      .collect(Collectors.joining(", "));
  }

  public static String getUndeprecatedMetricsinSonarQube108() {
    return getDeprecatedMetricsInSonarQube104() + ", " + getDeprecatedMetricsInSonarQube105();
  }

  public static String getNewScaMetricsInSonarQube202504() {
    return Stream.of(
      ScaMetrics.SCA_RATING_LICENSING_KEY,
      ScaMetrics.SCA_RATING_VULNERABILITY_KEY,
      ScaMetrics.SCA_RATING_ANY_ISSUE_KEY,
      ScaMetrics.SCA_SEVERITY_LICENSING_KEY,
      ScaMetrics.SCA_SEVERITY_VULNERABILITY_KEY,
      ScaMetrics.SCA_SEVERITY_ANY_ISSUE_KEY,
      ScaMetrics.SCA_COUNT_ANY_ISSUE_KEY,
      ScaMetrics.NEW_SCA_RATING_LICENSING_KEY,
      ScaMetrics.NEW_SCA_RATING_VULNERABILITY_KEY,
      ScaMetrics.NEW_SCA_RATING_ANY_ISSUE_KEY,
      ScaMetrics.NEW_SCA_SEVERITY_LICENSING_KEY,
      ScaMetrics.NEW_SCA_SEVERITY_VULNERABILITY_KEY,
      ScaMetrics.NEW_SCA_SEVERITY_ANY_ISSUE_KEY,
      ScaMetrics.NEW_SCA_COUNT_ANY_ISSUE_KEY)
      .map(e -> "'" + e + "'")
      .collect(Collectors.joining(", "));
  }
}
