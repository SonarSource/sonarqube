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

public class prMeasureFixTest {

  @Test
  public void should_add_replacement_metrics() {
    List<String> metricList = new ArrayList<>(Arrays.asList(NEW_BUGS_KEY, NEW_MINOR_VIOLATIONS_KEY));
    PrMeasureFix.addReplacementMetricKeys(metricList);
    assertThat(metricList).contains(BUGS_KEY, NEW_BUGS_KEY, MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY);
  }

  @Test
  public void should_remove_metrics_not_initially_requested() {
    Set<String> originalMetricList = new HashSet<>(Arrays.asList(NEW_BUGS_KEY, MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY));
    MetricDto dto1 = new MetricDto().setKey(BUGS_KEY).setUuid("1");
    MetricDto dto2 = new MetricDto().setKey(NEW_BUGS_KEY).setUuid("2");
    MetricDto dto3 = new MetricDto().setKey(MINOR_VIOLATIONS_KEY).setUuid("3");
    MetricDto dto4 = new MetricDto().setKey(NEW_MINOR_VIOLATIONS_KEY).setUuid("4");

    List<MetricDto> metricList = new ArrayList<>(Arrays.asList(dto1, dto2, dto3, dto4));

    PrMeasureFix.removeMetricsNotRequested(metricList, originalMetricList);
    assertThat(metricList).containsOnly(dto2, dto3, dto4);
  }

  @Test
  public void should_transform_measures() {
    Set<String> requestedKeys = new HashSet<>(Arrays.asList(NEW_BUGS_KEY, MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY));

    MetricDto bugsMetric = new MetricDto().setKey(BUGS_KEY).setUuid("1");
    MetricDto newBugsMetric = new MetricDto().setKey(NEW_BUGS_KEY).setUuid("2");
    MetricDto violationsMetric = new MetricDto().setKey(MINOR_VIOLATIONS_KEY).setUuid("3");
    MetricDto newViolationsMetric = new MetricDto().setKey(NEW_MINOR_VIOLATIONS_KEY).setUuid("4");

    List<MetricDto> metricList = Arrays.asList(bugsMetric, newBugsMetric, violationsMetric, newViolationsMetric);

    LiveMeasureDto bugs = createLiveMeasure(bugsMetric.getUuid(), 10.0);
    LiveMeasureDto newBugs = createLiveMeasure(newBugsMetric.getUuid(), 5.0);
    LiveMeasureDto violations = createLiveMeasure(violationsMetric.getUuid(), 20.0);
    LiveMeasureDto newViolations = createLiveMeasure(newViolationsMetric.getUuid(), 3.0);

    Map<MetricDto, LiveMeasureDto> measureByMetric = new HashMap<>();
    measureByMetric.put(bugsMetric, bugs);
    measureByMetric.put(newBugsMetric, newBugs);
    measureByMetric.put(violationsMetric, violations);
    measureByMetric.put(newViolationsMetric, newViolations);

    PrMeasureFix.createReplacementMeasures(metricList, measureByMetric, requestedKeys);
    assertThat(measureByMetric.entrySet()).extracting(e -> e.getKey().getKey(), e -> e.getValue().getValue())
      .containsOnly(tuple(NEW_BUGS_KEY, 10.0),
        tuple(MINOR_VIOLATIONS_KEY, 20.0),
        tuple(NEW_MINOR_VIOLATIONS_KEY, 20.0));
  }

  private static LiveMeasureDto createLiveMeasure(String metricUuid, @Nullable Double value) {
    return new LiveMeasureDto().setMetricUuid(metricUuid).setValue(value);
  }
}
