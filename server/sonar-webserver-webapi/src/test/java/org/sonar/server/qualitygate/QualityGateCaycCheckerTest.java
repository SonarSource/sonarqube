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
package org.sonar.server.qualitygate;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;

import static org.junit.Assert.assertEquals;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES;
import static org.sonar.api.measures.CoreMetrics.LINE_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING;
import static org.sonar.server.qualitygate.QualityGateCaycChecker.CAYC_METRICS;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.OVER_COMPLIANT;

public class QualityGateCaycCheckerTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  QualityGateCaycChecker underTest = new QualityGateCaycChecker(db.getDbClient());

  @Test
  public void checkCaycCompliant() {
    String qualityGateUuid = "abcd";
    CAYC_METRICS.forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));
    assertEquals(COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  public void check_Cayc_non_compliant_with_extra_conditions() {
    String qualityGateUuid = "abcd";
    CAYC_METRICS.forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));

    // extra conditions outside of CAYC requirements
    List.of(LINE_COVERAGE, DUPLICATED_LINES).forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));

    assertEquals(OVER_COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  public void check_Cayc_NonCompliant_with_lesser_threshold_value() {
    var metrics = CAYC_METRICS.stream().map(this::insertMetric).toList();

    IntStream.range(0, 4).forEach(idx -> {
      String qualityGateUuid = "abcd" + idx;
      for (int i = 0; i < metrics.size(); i++) {
        var metric = metrics.get(i);
        insertCondition(metric, qualityGateUuid, idx == i ? metric.getWorstValue() : metric.getBestValue());
      }
      assertEquals(NON_COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
    });
  }

  @Test
  public void check_Cayc_NonCompliant_with_missing_metric() {
    String qualityGateUuid = "abcd";
    List.of(NEW_MAINTAINABILITY_RATING, NEW_RELIABILITY_RATING, NEW_SECURITY_HOTSPOTS_REVIEWED, NEW_DUPLICATED_LINES_DENSITY)
      .forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));
    assertEquals(NON_COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  public void existency_requirements_check_only_existency() {
    String qualityGateUuid = "abcd";
    List.of(NEW_MAINTAINABILITY_RATING, NEW_RELIABILITY_RATING, NEW_SECURITY_HOTSPOTS_REVIEWED, NEW_SECURITY_RATING)
      .forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));
    List.of(NEW_COVERAGE, NEW_DUPLICATED_LINES_DENSITY)
      .forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getWorstValue()));
    assertEquals(COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  private void insertCondition(MetricDto metricDto, String qualityGateUuid, Double threshold) {
    QualityGateConditionDto newCondition = new QualityGateConditionDto().setQualityGateUuid(qualityGateUuid)
      .setUuid(Uuids.create())
      .setMetricUuid(metricDto.getUuid())
      .setOperator("LT")
      .setErrorThreshold(threshold.toString());
    db.getDbClient().gateConditionDao().insert(newCondition, db.getSession());
    db.commit();
  }

  private MetricDto insertMetric(Metric metric) {
    return db.measures().insertMetric(m -> m
      .setKey(metric.key())
      .setValueType(metric.getType().name())
      .setHidden(false)
      .setBestValue(metric.getBestValue())
      .setBestValue(metric.getWorstValue())
      .setDirection(metric.getDirection()));
  }
}
