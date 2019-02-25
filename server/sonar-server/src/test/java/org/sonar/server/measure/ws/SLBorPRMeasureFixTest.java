/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS_KEY;

public class SLBorPRMeasureFixTest {

  @Test
  public void should_add_replacement_metrics() {
    List<String> metricList = new ArrayList<>(Arrays.asList(NEW_BUGS_KEY, NEW_MINOR_VIOLATIONS_KEY));
    SLBorPRMeasureFix.addReplacementMetricKeys(metricList);
    assertThat(metricList).contains(BUGS_KEY, NEW_BUGS_KEY, MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY);
  }

  @Test
  public void should_remove_metrics_not_initially_requested() {
    Set<String> originalMetricList = new HashSet<>(Arrays.asList(NEW_BUGS_KEY, MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY));
    MetricDto dto1 = new MetricDto().setKey(BUGS_KEY).setId(1);
    MetricDto dto2 = new MetricDto().setKey(NEW_BUGS_KEY).setId(2);
    MetricDto dto3 = new MetricDto().setKey(MINOR_VIOLATIONS_KEY).setId(3);
    MetricDto dto4 = new MetricDto().setKey(NEW_MINOR_VIOLATIONS_KEY).setId(4);

    List<MetricDto> metricList = new ArrayList<>(Arrays.asList(dto1, dto2, dto3, dto4));

    SLBorPRMeasureFix.removeMetricsNotRequested(metricList, originalMetricList);
    assertThat(metricList).containsOnly(dto2, dto3, dto4);
  }

  @Test
  public void should_transform_measures() {
    Set<String> requestedKeys = new HashSet<>(Arrays.asList(NEW_BUGS_KEY, MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY));

    MetricDto bugsMetric = new MetricDto().setKey(BUGS_KEY).setId(1);
    MetricDto newBugsMetric = new MetricDto().setKey(NEW_BUGS_KEY).setId(2);
    MetricDto violationsMetric = new MetricDto().setKey(MINOR_VIOLATIONS_KEY).setId(3);
    MetricDto newViolationsMetric = new MetricDto().setKey(NEW_MINOR_VIOLATIONS_KEY).setId(4);

    List<MetricDto> metricList = Arrays.asList(bugsMetric, newBugsMetric, violationsMetric, newViolationsMetric);

    LiveMeasureDto bugs = createLiveMeasure(bugsMetric.getId(), 10.0, null);
    LiveMeasureDto newBugs = createLiveMeasure(newBugsMetric.getId(), null, 5.0);
    LiveMeasureDto violations = createLiveMeasure(violationsMetric.getId(), 20.0, null);
    LiveMeasureDto newViolations = createLiveMeasure(newViolationsMetric.getId(), null, 3.0);

    Map<MetricDto, LiveMeasureDto> measureByMetric = new HashMap<>();
    measureByMetric.put(bugsMetric, bugs);
    measureByMetric.put(newBugsMetric, newBugs);
    measureByMetric.put(violationsMetric, violations);
    measureByMetric.put(newViolationsMetric, newViolations);

    SLBorPRMeasureFix.createReplacementMeasures(metricList, measureByMetric, requestedKeys);
    assertThat(measureByMetric.entrySet()).extracting(e -> e.getKey().getKey(), e -> e.getValue().getValue(), e -> e.getValue().getVariation())
      .containsOnly(tuple(NEW_BUGS_KEY, null, 10.0),
        tuple(MINOR_VIOLATIONS_KEY, 20.0, null),
        tuple(NEW_MINOR_VIOLATIONS_KEY, null, 20.0));
  }

  private static LiveMeasureDto createLiveMeasure(int metricId, @Nullable Double value, @Nullable Double variation) {
    return new LiveMeasureDto().setMetricId(metricId).setVariation(variation).setValue(value);
  }
}
