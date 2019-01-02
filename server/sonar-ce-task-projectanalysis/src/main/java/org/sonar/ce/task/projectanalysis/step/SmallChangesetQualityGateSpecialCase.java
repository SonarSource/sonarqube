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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResult;
import org.sonar.ce.task.projectanalysis.step.QualityGateMeasuresStep.MetricEvaluationResult;

import static java.util.Arrays.asList;

public class SmallChangesetQualityGateSpecialCase {

  /**
   * Some metrics will be ignored on very small change sets.
   */
  private static final Collection<String> METRICS_TO_IGNORE_ON_SMALL_CHANGESETS = asList(
    CoreMetrics.NEW_COVERAGE_KEY,
    CoreMetrics.NEW_LINE_COVERAGE_KEY,
    CoreMetrics.NEW_BRANCH_COVERAGE_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_KEY,
    CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY);
  private static final int MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS = 19;

  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;

  public SmallChangesetQualityGateSpecialCase(MeasureRepository measureRepository, MetricRepository metricRepository) {
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
  }

  public boolean appliesTo(Component project, @Nullable MetricEvaluationResult metricEvaluationResult) {
    return metricEvaluationResult != null
      && metricEvaluationResult.evaluationResult.getLevel() != Measure.Level.OK
      && METRICS_TO_IGNORE_ON_SMALL_CHANGESETS.contains(metricEvaluationResult.condition.getMetric().getKey())
      && isSmallChangeset(project);
  }

  MetricEvaluationResult apply(MetricEvaluationResult metricEvaluationResult) {
    return new MetricEvaluationResult(
      new EvaluationResult(Measure.Level.OK, metricEvaluationResult.evaluationResult.getValue()), metricEvaluationResult.condition);
  }

  private boolean isSmallChangeset(Component project) {
    return measureRepository.getRawMeasure(project, metricRepository.getByKey(CoreMetrics.NEW_LINES_KEY))
      .map(newLines -> newLines.hasVariation() && newLines.getVariation() <= MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS)
      .orElse(false);
  }
}
