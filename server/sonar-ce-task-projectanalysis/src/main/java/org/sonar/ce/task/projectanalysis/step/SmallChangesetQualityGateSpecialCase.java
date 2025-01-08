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
package org.sonar.ce.task.projectanalysis.step;

import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResult;
import org.sonar.ce.task.projectanalysis.step.QualityGateMeasuresStep.MetricEvaluationResult;

import static org.sonar.server.qualitygate.QualityGateEvaluatorImpl.MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS;
import static org.sonar.server.qualitygate.QualityGateEvaluatorImpl.METRICS_TO_IGNORE_ON_SMALL_CHANGESETS;

public class SmallChangesetQualityGateSpecialCase {
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;
  private final ConfigurationRepository config;

  public SmallChangesetQualityGateSpecialCase(MeasureRepository measureRepository, MetricRepository metricRepository, ConfigurationRepository config) {
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.config = config;
  }

  public boolean appliesTo(Component project, @Nullable MetricEvaluationResult metricEvaluationResult) {
    return metricEvaluationResult != null
      && metricEvaluationResult.evaluationResult.level() != Measure.Level.OK
      && METRICS_TO_IGNORE_ON_SMALL_CHANGESETS.contains(metricEvaluationResult.condition.getMetric().getKey())
      && config.getConfiguration().getBoolean(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES).orElse(true)
      && isSmallChangeset(project);
  }

  MetricEvaluationResult apply(MetricEvaluationResult metricEvaluationResult) {
    return new MetricEvaluationResult(
      new EvaluationResult(Measure.Level.OK, metricEvaluationResult.evaluationResult.value()), metricEvaluationResult.condition);
  }

  private boolean isSmallChangeset(Component project) {
    return measureRepository.getRawMeasure(project, metricRepository.getByKey(CoreMetrics.NEW_LINES_KEY))
      .map(newLines -> newLines.getIntValue() < MAXIMUM_NEW_LINES_FOR_SMALL_CHANGESETS)
      .orElse(false);
  }
}
