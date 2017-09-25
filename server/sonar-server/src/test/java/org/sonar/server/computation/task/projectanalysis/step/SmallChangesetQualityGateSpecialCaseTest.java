/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.qualitygate.Condition;
import org.sonar.server.computation.task.projectanalysis.qualitygate.EvaluationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.ERROR;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.OK;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.WARN;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class SmallChangesetQualityGateSpecialCaseTest {

  public static final int PROJECT_REF = 1234;
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.NEW_LINES)
    .add(CoreMetrics.NEW_COVERAGE)
    .add(CoreMetrics.NEW_BUGS);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  private final SmallChangesetQualityGateSpecialCase underTest = new SmallChangesetQualityGateSpecialCase(measureRepository, metricRepository);

  @Test
  public void ignore_errors_about_new_coverage_for_small_changesets() throws Exception {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().setVariation(19).create(1000));

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(project, metricEvaluationResult);

    assertThat(result.evaluationResult.getLevel()).isSameAs(OK);
  }

  @Test
  public void ignore_warnings_about_new_coverage_for_small_changesets() throws Exception {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, WARN);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().setVariation(19).create(1000));

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(project, metricEvaluationResult);

    assertThat(result.evaluationResult.getLevel()).isSameAs(OK);
  }

  @Test
  public void should_not_change_for_bigger_changesets() throws Exception {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().setVariation(20).create(1000));

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(project, metricEvaluationResult);

    assertThat(result.evaluationResult.getLevel()).isSameAs(ERROR);
  }

  @Test
  public void should_not_change_issue_related_metrics() throws Exception {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_BUGS_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().setVariation(19).create(1000));

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(project, metricEvaluationResult);

    assertThat(result.evaluationResult.getLevel()).isSameAs(ERROR);
  }

  @Test
  public void should_not_change_green_conditions() throws Exception {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_BUGS_KEY, OK);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().setVariation(19).create(1000));

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(project, metricEvaluationResult);

    assertThat(result.evaluationResult.getLevel()).isSameAs(OK);
  }

  @Test
  public void should_not_change_quality_gate_if_new_lines_is_not_defined() throws Exception {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(project, metricEvaluationResult);

    assertThat(result.evaluationResult.getLevel()).isSameAs(ERROR);
  }

  @Test
  public void should_silently_ignore_null_values() throws Exception {

    QualityGateMeasuresStep.MetricEvaluationResult result = underTest.applyIfNeeded(mock(Component.class), null);

    assertThat(result).isNull();
  }

  private Component generateNewRootProject() {
    treeRootHolder.setRoot(builder(Component.Type.PROJECT, PROJECT_REF).build());
    return treeRootHolder.getRoot();
  }

  private QualityGateMeasuresStep.MetricEvaluationResult generateEvaluationResult(String metric, Measure.Level level) {
    Metric newCoverageMetric = metricRepository.getByKey(metric);
    Condition condition = new Condition(newCoverageMetric, "LT", "80", "90", true);
    EvaluationResult evaluationResult = new EvaluationResult(level, mock(Comparable.class));
    return new QualityGateMeasuresStep.MetricEvaluationResult(evaluationResult, condition);
  }
}
