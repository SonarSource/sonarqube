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
package org.sonar.server.measure.index;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMeasuresSoftwareQualityRatingsInitializerTest {

  @Test
  void initializeSoftwareQualityRatings_whenNoRating_thenNoSoftwareQualityRatingMetric() {
    Map<String, Double> initialMeasures = new HashMap<>();
    initialMeasures.put(CoreMetrics.BRANCH_COVERAGE_KEY, null);
    initialMeasures.put(CoreMetrics.ACCEPTED_ISSUES_KEY, 12.0);
    initialMeasures.put(CoreMetrics.SQALE_RATING_KEY, null);

    Map<String, Double> measures = new HashMap<>(initialMeasures);
    ProjectMeasuresSoftwareQualityRatingsInitializer.initializeSoftwareQualityRatings(measures);

    assertThat(measures).containsExactlyInAnyOrderEntriesOf(initialMeasures);
  }

  @Test
  void initializeSoftwareQualityRatings_whenRatingAndNoSoftwareQualityRating_thenSoftwareQualityRatingMetricAreCreated() {
    Map<String, Double> initialMeasures = new HashMap<>();
    initialMeasures.put(CoreMetrics.SQALE_RATING_KEY, 1.0);
    initialMeasures.put(CoreMetrics.RELIABILITY_RATING_KEY, 2.0);
    initialMeasures.put(CoreMetrics.SECURITY_RATING_KEY, 3.0);
    initialMeasures.put(CoreMetrics.NEW_SECURITY_RATING_KEY, 4.0);
    initialMeasures.put(CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY, 5.0);
    initialMeasures.put(CoreMetrics.NEW_RELIABILITY_RATING_KEY, 1.0);

    Map<String, Double> measures = new HashMap<>(initialMeasures);

    ProjectMeasuresSoftwareQualityRatingsInitializer.initializeSoftwareQualityRatings(measures);

    assertThat(measures).hasSize(initialMeasures.size() * 2)
      .containsAllEntriesOf(initialMeasures)
      .containsEntry(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, 1.0)
      .containsEntry(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, 2.0)
      .containsEntry(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY, 3.0)
      .containsEntry(SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, 4.0)
      .containsEntry(SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, 5.0)
      .containsEntry(SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, 1.0);
  }
}
