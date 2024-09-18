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
package org.sonar.server.measure.index;

import java.util.Map;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.server.measure.Rating;

/**
 * This class defines "default" measures values for the "Software Quality Rating Metrics" when they do not exist.
 * The "default" value is the same as the equivalent "Rule Type Rating Metric"
  * If the "Software Quality Rating Metrics" exists, then no changes are made
 */
public class ProjectMeasuresSoftwareQualityRatingsInitializer {

  private static final Map<String, String> RATING_KEY_TO_SOFTWARE_QUALITY_RATING_KEY = Map.of(
    CoreMetrics.SQALE_RATING_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    CoreMetrics.RELIABILITY_RATING_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    CoreMetrics.SECURITY_RATING_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    CoreMetrics.NEW_SECURITY_RATING_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    CoreMetrics.NEW_RELIABILITY_RATING_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY
  );

  private ProjectMeasuresSoftwareQualityRatingsInitializer() {
  }

  public static Map<String, Double> initializeSoftwareQualityRatings(Map<String, Double> measures) {

    RATING_KEY_TO_SOFTWARE_QUALITY_RATING_KEY.forEach((k, v) -> initializeSoftwareQualityRatingMeasure(measures, k, v));
    return measures;
  }

  private static void initializeSoftwareQualityRatingMeasure(Map<String, Double> measures, String ruleTypeMetric,
    String softwareQualityMetric) {
    if (measures.containsKey(softwareQualityMetric)) {
      return;
    }

    Double value = measures.get(ruleTypeMetric);
    if (value != null) {
      measures.put(softwareQualityMetric, value);
    }
  }
}
