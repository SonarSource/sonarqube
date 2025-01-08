/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.LINE_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS;
import static org.sonar.server.qualitygate.QualityGateCaycChecker.BEST_VALUE_REQUIREMENTS;
import static org.sonar.server.qualitygate.QualityGateCaycChecker.CAYC_METRICS;
import static org.sonar.server.qualitygate.QualityGateCaycChecker.LEGACY_BEST_VALUE_REQUIREMENTS;
import static org.sonar.server.qualitygate.QualityGateCaycChecker.LEGACY_CAYC_METRICS;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.OVER_COMPLIANT;

@RunWith(DataProviderRunner.class)
public class QualityGateCaycCheckerIT {

  @Rule
  public DbTester db = DbTester.create();
  QualityGateCaycChecker underTest = new QualityGateCaycChecker(db.getDbClient());

  @Test
  @UseDataProvider("caycMetrics")
  public void checkCaycCompliant_when_contains_all_and_only_compliant_conditions_should_return_compliant(Set<Metric>  caycMetrics) {
    String qualityGateUuid = "abcd";
    caycMetrics.forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));
    assertEquals(COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  @UseDataProvider("caycMetrics")
  public void checkCaycCompliant_when_extra_conditions_should_return_over_compliant(Set<Metric>  caycMetrics) {
    String qualityGateUuid = "abcd";
    caycMetrics.forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));

    // extra conditions outside of CAYC requirements
    List.of(LINE_COVERAGE, DUPLICATED_LINES).forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid,
      metric.getBestValue()));

    assertEquals(OVER_COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  @UseDataProvider("caycMetricsAndBestValueRequirements")
  public void checkCaycCompliant_when_conditions_have_lesser_threshold_value_should_return_non_compliant(Set<Metric>  caycMetrics, Map<String, Double> bestValueRequirements) {
    var metrics = caycMetrics.stream().map(this::insertMetric).toList();

    String qualityGateUuid = "abcd";
    for (var metric : metrics) {
      if (bestValueRequirements.keySet().contains(metric.getKey())) {
        insertCondition(metric, qualityGateUuid, metric.getBestValue() - 1);
      } else {
        insertCondition(metric, qualityGateUuid, metric.getBestValue());
      }
    }
    assertEquals(NON_COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  public void isCaycCondition_when_check_compliant_condition_should_return_true() {
    CAYC_METRICS.stream().map(this::toMetricDto)
      .forEach(metricDto -> assertTrue(underTest.isCaycCondition(metricDto)));
    LEGACY_CAYC_METRICS.stream().map(this::toMetricDto)
      .forEach(metricDto -> assertTrue(underTest.isCaycCondition(metricDto)));
  }

  @Test
  public void isCaycCondition_when_check_non_compliant_condition_should_return_false() {
    List.of(BLOCKER_VIOLATIONS, FUNCTION_COMPLEXITY)
      .stream().map(this::toMetricDto)
      .forEach(metricDto -> assertFalse(underTest.isCaycCondition(metricDto)));
  }


  @Test
  public void checkCaycCompliant_when_missing_compliant_condition_should_return_non_compliant() {
    String qualityGateUuid = "abcd";
    List.of(NEW_VIOLATIONS, NEW_SECURITY_HOTSPOTS_REVIEWED)
      .forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));
    assertEquals(NON_COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @Test
  public void existency_requirements_check_only_existency() {
    String qualityGateUuid = "abcd";
    List.of(NEW_VIOLATIONS, NEW_SECURITY_HOTSPOTS_REVIEWED)
      .forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getBestValue()));
    List.of(NEW_COVERAGE, NEW_DUPLICATED_LINES_DENSITY)
      .forEach(metric -> insertCondition(insertMetric(metric), qualityGateUuid, metric.getWorstValue()));
    assertEquals(COMPLIANT, underTest.checkCaycCompliant(db.getSession(), qualityGateUuid));
  }

  @DataProvider
  public static Object[][] caycMetrics() {
    return new Object[][]{
      {CAYC_METRICS}, {LEGACY_CAYC_METRICS}
    };
  }

  @DataProvider
  public static Object[][] caycMetricsAndBestValueRequirements() {
    return new Object[][]{
      {CAYC_METRICS, BEST_VALUE_REQUIREMENTS},
      {LEGACY_CAYC_METRICS, LEGACY_BEST_VALUE_REQUIREMENTS}
    };
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
      .setWorstValue(metric.getWorstValue())
      .setDirection(metric.getDirection()));
  }

  private MetricDto toMetricDto(Metric metric) {
    return new MetricDto()
      .setKey(metric.key())
      .setValueType(metric.getType().name())
      .setBestValue(metric.getBestValue())
      .setWorstValue(metric.getWorstValue())
      .setDirection(metric.getDirection());
  }
}
