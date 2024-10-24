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
package org.sonar.core.metric;

import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static org.sonar.api.measures.CoreMetrics.DOMAIN_ISSUES;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_MAINTAINABILITY;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_RELIABILITY;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_SECURITY;

public class SoftwareQualitiesMetrics implements Metrics {

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY = "software_quality_maintainability_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, "Maintainability Issues", Metric.ValueType.INT)
    .setDescription("Maintainability Issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY = "new_software_quality_maintainability_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, "New Maintainability Issues", Metric.ValueType.INT)
      .setDescription("New Maintainability Issues")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY = "software_quality_maintainability_rating";
  public static final Metric<Integer> SOFTWARE_QUALITY_MAINTAINABILITY_RATING =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, "Maintainability Rating", Metric.ValueType.RATING)
      .setDescription("Maintainability rating")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(1.0)
      .setWorstValue(5.0)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY = "new_software_quality_maintainability_rating";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, "Maintainability Rating on New Code",
      Metric.ValueType.RATING)
      .setDescription("Maintainability rating on new code")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setDeleteHistoricalData(true)
      .setOptimizedBestValue(true)
      .setQualitative(true)
      .setBestValue(1.0)
      .setWorstValue(5.0)
      .create();

  public static final String SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY = "software_quality_reliability_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_RELIABILITY_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, "Reliability Issues", Metric.ValueType.INT)
      .setDescription("Reliability Issues")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_RELIABILITY)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY = "new_software_quality_reliability_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, "New Reliability Issues", Metric.ValueType.INT)
      .setDescription("New Reliability Issues")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RELIABILITY)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_RELIABILITY_RATING_KEY = "software_quality_reliability_rating";
  public static final Metric<Integer> SOFTWARE_QUALITY_RELIABILITY_RATING = new Metric.Builder(SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    "Reliability Rating", Metric.ValueType.RATING)
    .setDescription("Reliability rating")
    .setDomain(DOMAIN_RELIABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY = "new_software_quality_reliability_rating";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_RELIABILITY_RATING =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, "Reliability Rating on New Code", Metric.ValueType.RATING)
      .setDescription("Reliability rating on new code")
    .setDomain(DOMAIN_RELIABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  public static final String SOFTWARE_QUALITY_SECURITY_ISSUES_KEY = "software_quality_security_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_SECURITY_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, "Security Issues", Metric.ValueType.INT)
      .setDescription("Security Issues")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SECURITY)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY = "new_software_quality_security_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_SECURITY_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, "New Security Issues", Metric.ValueType.INT)
      .setDescription("New Security Issues")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_SECURITY)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_SECURITY_RATING_KEY = "software_quality_security_rating";
  public static final Metric<Integer> SOFTWARE_QUALITY_SECURITY_RATING = new Metric.Builder(SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    "Security Rating", Metric.ValueType.RATING)
    .setDescription("Security rating")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY = "new_software_quality_security_rating";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_SECURITY_RATING = new Metric.Builder(NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    "Security Rating on New Code", Metric.ValueType.RATING)
    .setDescription("Security rating on new code")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  public static final String EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY =
    "effort_to_reach_software_quality_maintainability_rating_a";
  public static final Metric<Long> EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A =
    new Metric.Builder(EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY,
      "Effort to Reach Maintainability Rating A", Metric.ValueType.WORK_DUR)
      .setDescription("Effort to reach maintainability rating A")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY =
    "software_quality_maintainability_remediation_effort";
  public static final Metric<Long> SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, "Technical Debt",
      Metric.ValueType.WORK_DUR)
      .setDescription("Total effort (in minutes) to fix all the maintainability issues on the component and therefore to comply to all " +
        "the requirements.")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY =
    "new_software_quality_maintainability_remediation_effort";
  public static final Metric<Long> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, "Added Technical Debt",
      Metric.ValueType.WORK_DUR)
      .setDescription("Total effort (in minutes) to fix all the maintainability issues on new code on the component and therefore to " +
        "comply to all the requirements.")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();


  public static final String SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY = "software_quality_security_remediation_effort";
  public static final Metric<Long> SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT =
    new Metric.Builder(SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, "Security Remediation Effort",
      Metric.ValueType.WORK_DUR)
      .setDescription("Security remediation effort")
      .setDomain(DOMAIN_SECURITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY = "new_software_quality_security_remediation_effort";
  public static final Metric<Long> NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, "Security Remediation Effort on New Code",
      Metric.ValueType.WORK_DUR)
      .setDescription("Security remediation effort on new code")
      .setDomain(DOMAIN_SECURITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY = "software_quality_reliability_remediation_effort";
  public static final Metric<Long> SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, "Reliability Remediation Effort",
      Metric.ValueType.WORK_DUR)
      .setDescription("Reliability remediation effort")
      .setDomain(DOMAIN_RELIABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY =
    "new_software_quality_reliability_remediation_effort";
  public static final Metric<Long> NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, "Reliability Remediation Effort on New Code",
      Metric.ValueType.WORK_DUR)
      .setDescription("Reliability remediation effort on new code")
      .setDomain(DOMAIN_RELIABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY = "software_quality_maintainability_debt_ratio";
  public static final Metric<Double> SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY, "Technical Debt Ratio", Metric.ValueType.PERCENT)
      .setDescription("Ratio of the actual technical debt compared to the estimated cost to develop the whole source code from scratch")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY = "new_software_quality_maintainability_debt_ratio";
  public static final Metric<Double> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY, "Technical Debt Ratio on New Code",
      Metric.ValueType.PERCENT)
      .setDescription("Technical Debt Ratio on New Code")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY = "software_quality_blocker_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_BLOCKER_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY, "Blocker Severity Issues", Metric.ValueType.INT)
      .setDescription("Blocker Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY = "new_software_quality_blocker_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY, "New Blocker Severity Issues", Metric.ValueType.INT)
    .setDescription("New Blocker Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String SOFTWARE_QUALITY_HIGH_ISSUES_KEY = "software_quality_high_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_HIGH_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_HIGH_ISSUES_KEY, "High Severity Issues", Metric.ValueType.INT)
      .setDescription("High Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY = "new_software_quality_high_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_HIGH_ISSUES = new Metric.Builder(NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY,
    "New High Severity Issues", Metric.ValueType.INT)
    .setDescription("New High Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY = "software_quality_medium_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_MEDIUM_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY, "Medium Severity Issues", Metric.ValueType.INT)
      .setDescription("Medium Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY = "new_software_quality_medium_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY, "New Medium Severity Issues", Metric.ValueType.INT)
    .setDescription("New Medium Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String SOFTWARE_QUALITY_LOW_ISSUES_KEY = "software_quality_low_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_LOW_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_LOW_ISSUES_KEY, "Low Severity Issues", Metric.ValueType.INT)
      .setDescription("Low Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY = "new_software_quality_low_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_LOW_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY,
    "New Low Severity Issues", Metric.ValueType.INT)
    .setDescription("New Low Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String SOFTWARE_QUALITY_INFO_ISSUES_KEY = "software_quality_info_issues";
  public static final Metric<Integer> SOFTWARE_QUALITY_INFO_ISSUES =
    new Metric.Builder(SOFTWARE_QUALITY_INFO_ISSUES_KEY, "Info Severity Issues", Metric.ValueType.INT)
      .setDescription("Info Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY = "new_software_quality_info_issues";
  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_INFO_ISSUES =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY, "New Info Severity Issues", Metric.ValueType.INT)
    .setDescription("New Info Severity issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();


  @Override
  public List<Metric> getMetrics() {
    return List.of(
      SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES,
      SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
      SOFTWARE_QUALITY_RELIABILITY_ISSUES,
      NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES,
      SOFTWARE_QUALITY_RELIABILITY_RATING,
      NEW_SOFTWARE_QUALITY_RELIABILITY_RATING,
      SOFTWARE_QUALITY_SECURITY_ISSUES,
      NEW_SOFTWARE_QUALITY_SECURITY_ISSUES,
      SOFTWARE_QUALITY_SECURITY_RATING,
      NEW_SOFTWARE_QUALITY_SECURITY_RATING,
      EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A,
      SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
      SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
      NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
      SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
      NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
      SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
      SOFTWARE_QUALITY_BLOCKER_ISSUES,
      NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES,
      SOFTWARE_QUALITY_HIGH_ISSUES,
      NEW_SOFTWARE_QUALITY_HIGH_ISSUES,
      SOFTWARE_QUALITY_MEDIUM_ISSUES,
      NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES,
      SOFTWARE_QUALITY_LOW_ISSUES,
      NEW_SOFTWARE_QUALITY_LOW_ISSUES,
      SOFTWARE_QUALITY_INFO_ISSUES,
      NEW_SOFTWARE_QUALITY_INFO_ISSUES
    );
  }
}
