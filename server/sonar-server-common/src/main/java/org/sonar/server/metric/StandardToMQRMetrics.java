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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_INFO_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_LOW_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY;

/**
 * Defines the metrics mapping between the standard mode and the MQR mode.
 * This list all the metrics that are specific for each mode, and the equivalent metric in the other mode.
 */
public class StandardToMQRMetrics {
  private static final BiMap<String, String> STANDARD_TO_MQR_MODE_METRICS;

  private static final BiMap<String, String> MQR_TO_STANDARD_MODE_METRICS;

  static {
    STANDARD_TO_MQR_MODE_METRICS = HashBiMap.create();
    // Severity related metrics
    STANDARD_TO_MQR_MODE_METRICS.put(BLOCKER_VIOLATIONS_KEY, SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(NEW_BLOCKER_VIOLATIONS_KEY, NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CRITICAL_VIOLATIONS_KEY, SOFTWARE_QUALITY_HIGH_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(NEW_CRITICAL_VIOLATIONS_KEY, NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(MAJOR_VIOLATIONS_KEY, SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(NEW_MAJOR_VIOLATIONS_KEY, NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(MINOR_VIOLATIONS_KEY, SOFTWARE_QUALITY_LOW_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(NEW_MINOR_VIOLATIONS_KEY, NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(INFO_VIOLATIONS_KEY, SOFTWARE_QUALITY_INFO_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(NEW_INFO_VIOLATIONS_KEY, NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY);

    // Maintainability related metrics
    STANDARD_TO_MQR_MODE_METRICS.put(CODE_SMELLS_KEY, SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(NEW_CODE_SMELLS_KEY, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(SQALE_RATING_KEY, SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.TECHNICAL_DEBT_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_TECHNICAL_DEBT_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.SQALE_DEBT_RATIO_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY);

    STANDARD_TO_MQR_MODE_METRICS.put(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY, EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY);

    // Security related metrics
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.SECURITY_RATING_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_SECURITY_RATING_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.VULNERABILITIES_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_VULNERABILITIES_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY);

    // Reliability related metrics
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.RELIABILITY_RATING_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_RELIABILITY_RATING_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.BUGS_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY);
    STANDARD_TO_MQR_MODE_METRICS.put(CoreMetrics.NEW_BUGS_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY);

    MQR_TO_STANDARD_MODE_METRICS = STANDARD_TO_MQR_MODE_METRICS.inverse();
  }

  private StandardToMQRMetrics() {
  }

  public static boolean isStandardMetric(String metricKey) {
    return STANDARD_TO_MQR_MODE_METRICS.containsKey(metricKey);
  }

  public static boolean isMQRMetric(String metricKey) {
    return MQR_TO_STANDARD_MODE_METRICS.containsKey(metricKey);
  }

  /**
   * Retrieves equivalent metric in the other mode. Return empty if metric has no equivalence
   */
  public static Optional<String> getEquivalentMetric(String metricKey) {
    return Optional.ofNullable(STANDARD_TO_MQR_MODE_METRICS.get(metricKey))
      .or(() -> Optional.ofNullable(MQR_TO_STANDARD_MODE_METRICS.get(metricKey)));
  }
}
