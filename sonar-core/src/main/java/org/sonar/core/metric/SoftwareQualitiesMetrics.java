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

import static org.sonar.api.measures.CoreMetrics.DOMAIN_MAINTAINABILITY;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_RELIABILITY;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_SECURITY;
import static org.sonar.api.measures.CoreMetrics.DOMAIN_SECURITY_REVIEW;

public class SoftwareQualitiesMetrics implements Metrics {

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY = "software_quality_maintainability_rating";

  public static final Metric<Integer> SOFTWARE_QUALITY_MAINTAINABILITY_RATING =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, "Software Quality Maintainability Rating", Metric.ValueType.RATING)
      .setDescription("Software quality maintainability rating")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(1.0)
      .setWorstValue(4.0)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY = "new_software_quality_maintainability_rating";

  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, "Software Quality Maintainability Rating on New Code",
      Metric.ValueType.RATING)
      .setDescription("Software quality maintainability rating on new code")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setDeleteHistoricalData(true)
      .setOptimizedBestValue(true)
      .setQualitative(true)
      .setBestValue(1.0)
      .setWorstValue(4.0)
      .create();

  public static final String SOFTWARE_QUALITY_RELIABILITY_RATING_KEY = "software_quality_reliability_rating";

  public static final Metric<Integer> SOFTWARE_QUALITY_RELIABILITY_RATING = new Metric.Builder(SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    "Software Quality Reliability Rating", Metric.ValueType.RATING)
    .setDescription("Software quality reliability rating")
    .setDomain(DOMAIN_RELIABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(4.0)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY = "new_software_quality_reliability_rating";

  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_RELIABILITY_RATING =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    "Software Quality Reliability Rating on New Code", Metric.ValueType.RATING)
    .setDescription("Software quality reliability rating on new code")
    .setDomain(DOMAIN_RELIABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(4.0)
    .create();

  public static final String SOFTWARE_QUALITY_SECURITY_RATING_KEY = "software_quality_security_rating";

  public static final Metric<Integer> SOFTWARE_QUALITY_SECURITY_RATING = new Metric.Builder(SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    "Software Quality Security Rating", Metric.ValueType.RATING)
    .setDescription("Software quality security rating")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(4.0)
    .create();

  public static final String NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY = "new_software_quality_security_rating";

  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_SECURITY_RATING = new Metric.Builder(NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    "Software Quality Security Rating on New Code", Metric.ValueType.RATING)
    .setDescription("Software quality security rating on new code")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(4.0)
    .create();

  public static final String SOFTWARE_QUALITY_SECURITY_REVIEW_RATING_KEY = "software_quality_security_review_rating";

  public static final Metric<Integer> SOFTWARE_QUALITY_SECURITY_REVIEW_RATING =
    new Metric.Builder(SOFTWARE_QUALITY_SECURITY_REVIEW_RATING_KEY, "Software Quality Security Review Rating", Metric.ValueType.RATING)
      .setDescription("Software quality security review rating")
      .setDomain(DOMAIN_SECURITY_REVIEW)
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(1.0)
      .setWorstValue(4.0)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING_KEY = "new_software_quality_security_review_rating";

  public static final Metric<Integer> NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING_KEY, "Software Quality Security Review Rating on New Code",
      Metric.ValueType.RATING)
      .setDescription("Software quality security review rating on new code")
      .setDomain(DOMAIN_SECURITY_REVIEW)
      .setDirection(Metric.DIRECTION_WORST)
      .setDeleteHistoricalData(true)
      .setOptimizedBestValue(true)
      .setQualitative(true)
      .setBestValue(1.0)
      .setWorstValue(4.0)
      .create();

  public static final String EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY =
    "effort_to_reach_software_quality_maintainability_rating_a";

  public static final Metric<Long> EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A =
    new Metric.Builder(EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY,
      "Software Quality Effort to Reach Maintainability Rating A", Metric.ValueType.WORK_DUR)
      .setDescription("Software quality effort to reach maintainability rating A")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY =
    "software_quality_maintainability_remediation_effort";

  public static final Metric<Long> SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, "Software Quality Maintainability Remediation Effort",
      Metric.ValueType.WORK_DUR)
      .setDescription("Software quality total effort (in minutes) to fix all the maintainability issues on the component and therefore to comply to all the requirements.")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY =
    "new_software_quality_maintainability_remediation_effort";

  public static final Metric<Long> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, "Software Quality Maintainability Remediation Effort on new code",
      Metric.ValueType.WORK_DUR)
      .setDescription("Software quality total effort (in minutes) to fix all the maintainability issues on new code on the component and therefore to comply to all the requirements.")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();


  public static final String SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY = "software_quality_security_remediation_effort";

  public static final Metric<Long> SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT =
    new Metric.Builder(SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, "Software Quality Security Remediation Effort",
      Metric.ValueType.WORK_DUR)
      .setDescription("Software quality security remediation effort")
      .setDomain(DOMAIN_SECURITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY = "new_software_quality_security_remediation_effort";

  public static final Metric<Long> NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, "Software Quality Security Remediation Effort on New Code",
      Metric.ValueType.WORK_DUR)
      .setDescription("Software quality security remediation effort on new code")
      .setDomain(DOMAIN_SECURITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY = "software_quality_reliability_remediation_effort";

  public static final Metric<Long> SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, "Software Quality Reliability Remediation Effort",
      Metric.ValueType.WORK_DUR)
      .setDescription("Software quality reliability remediation effort")
      .setDomain(DOMAIN_RELIABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY =
    "new_software_quality_reliability_remediation_effort";

  public static final Metric<Long> NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, "Software Quality Reliability Remediation Effort on New Code",
      Metric.ValueType.WORK_DUR)
      .setDescription("Software quality reliability remediation effort on new code")
      .setDomain(DOMAIN_RELIABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY = "software_quality_maintainability_debt_ratio";

  public static final Metric<Double> SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO =
    new Metric.Builder(SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY, "Software Quality Technical Debt Ratio", Metric.ValueType.PERCENT)
      .setDescription("Software quality ratio of the actual technical debt compared to the estimated cost to develop the whole source code from scratch")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  public static final String NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY = "new_software_quality_maintainability_debt_ratio";

  public static final Metric<Double> NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO =
    new Metric.Builder(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY, "Software Quality Technical Debt Ratio on New Code",
      Metric.ValueType.PERCENT)
      .setDescription("Software quality technical debt ratio software quality of new/changed code.")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  @Override
  public List<Metric> getMetrics() {
    return List.of(
      SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING,
      SOFTWARE_QUALITY_RELIABILITY_RATING,
      NEW_SOFTWARE_QUALITY_RELIABILITY_RATING,
      SOFTWARE_QUALITY_SECURITY_RATING,
      NEW_SOFTWARE_QUALITY_SECURITY_RATING,
      SOFTWARE_QUALITY_SECURITY_REVIEW_RATING,
      NEW_SOFTWARE_QUALITY_SECURITY_REVIEW_RATING,
      EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A,
      SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT,
      SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
      NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT,
      SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
      NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT,
      SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO,
      NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO
    );
  }
}
