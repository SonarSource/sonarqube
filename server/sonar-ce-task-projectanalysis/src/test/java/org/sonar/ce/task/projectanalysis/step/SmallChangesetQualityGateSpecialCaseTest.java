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
package org.sonar.ce.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TestSettingsRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.ERROR;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.OK;

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

  private final MapSettings mapSettings = new MapSettings();
  private final TestSettingsRepository settings = new TestSettingsRepository(new ConfigurationBridge(mapSettings));
  private final SmallChangesetQualityGateSpecialCase underTest = new SmallChangesetQualityGateSpecialCase(measureRepository, metricRepository, settings);

  @Test
  public void ignore_errors_about_new_coverage_for_small_changesets() {
    mapSettings.setProperty(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES, true);
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().create(19));

    boolean result = underTest.appliesTo(project, metricEvaluationResult);

    assertThat(result).isTrue();
  }

  @Test
  public void dont_ignore_errors_about_new_coverage_for_small_changesets_if_disabled() {
    mapSettings.setProperty(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES, false);
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().create(19));

    boolean result = underTest.appliesTo(project, metricEvaluationResult);

    assertThat(result).isFalse();
  }

  @Test
  public void should_not_change_for_bigger_changesets() {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().create(20));

    boolean result = underTest.appliesTo(project, metricEvaluationResult);

    assertThat(result).isFalse();
  }

  @Test
  public void should_not_change_issue_related_metrics() {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_BUGS_KEY, ERROR);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().create(19));

    boolean result = underTest.appliesTo(project, metricEvaluationResult);

    assertThat(result).isFalse();
  }

  @Test
  public void should_not_change_green_conditions() {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_BUGS_KEY, OK);
    Component project = generateNewRootProject();
    measureRepository.addRawMeasure(PROJECT_REF, CoreMetrics.NEW_LINES_KEY, newMeasureBuilder().create(19));

    boolean result = underTest.appliesTo(project, metricEvaluationResult);

    assertThat(result).isFalse();
  }

  @Test
  public void should_not_change_quality_gate_if_new_lines_is_not_defined() {
    QualityGateMeasuresStep.MetricEvaluationResult metricEvaluationResult = generateEvaluationResult(NEW_COVERAGE_KEY, ERROR);
    Component project = generateNewRootProject();

    boolean result = underTest.appliesTo(project, metricEvaluationResult);

    assertThat(result).isFalse();
  }

  @Test
  public void should_silently_ignore_null_values() {

    boolean result = underTest.appliesTo(mock(Component.class), null);

    assertThat(result).isFalse();
  }

  @Test
  public void apply() {
    Comparable<?> value = mock(Comparable.class);
    Condition condition = mock(Condition.class);
    QualityGateMeasuresStep.MetricEvaluationResult original = new QualityGateMeasuresStep.MetricEvaluationResult(
      new EvaluationResult(Measure.Level.ERROR, value), condition);

    QualityGateMeasuresStep.MetricEvaluationResult modified = underTest.apply(original);

    assertThat(modified.evaluationResult.level()).isSameAs(OK);
    assertThat(modified.evaluationResult.value()).isSameAs(value);
    assertThat(modified.condition).isSameAs(condition);
  }

  private Component generateNewRootProject() {
    treeRootHolder.setRoot(builder(Component.Type.PROJECT, PROJECT_REF).build());
    return treeRootHolder.getRoot();
  }

  private QualityGateMeasuresStep.MetricEvaluationResult generateEvaluationResult(String metric, Measure.Level level) {
    Metric newCoverageMetric = metricRepository.getByKey(metric);
    Condition condition = new Condition(newCoverageMetric, "LT", "80");
    EvaluationResult evaluationResult = new EvaluationResult(level, mock(Comparable.class));
    return new QualityGateMeasuresStep.MetricEvaluationResult(evaluationResult, condition);
  }
}
