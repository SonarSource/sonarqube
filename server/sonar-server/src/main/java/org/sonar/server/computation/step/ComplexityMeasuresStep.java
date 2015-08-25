/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import com.google.common.collect.ImmutableList;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.AverageFormula;
import org.sonar.server.computation.formula.DistributionFormula;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.server.computation.formula.SumFormula.createIntSumFormula;

/**
 * Computes complexity measures on files and then aggregates them on higher components.
 */
public class ComplexityMeasuresStep implements ComputationStep {

  private static final ImmutableList<Formula> FORMULAS = ImmutableList.<Formula>of(
    createIntSumFormula(COMPLEXITY_KEY),
    createIntSumFormula(COMPLEXITY_IN_CLASSES_KEY),
    createIntSumFormula(COMPLEXITY_IN_FUNCTIONS_KEY),
    new DistributionFormula(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY),
    new DistributionFormula(FILE_COMPLEXITY_DISTRIBUTION_KEY),
    new DistributionFormula(CLASS_COMPLEXITY_DISTRIBUTION_KEY),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(FILE_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_KEY)
      .setByMetricKey(FILES_KEY)
      .build(),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(CLASS_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_CLASSES_KEY)
      .setByMetricKey(CLASSES_KEY)
      .setFallbackMetricKey(COMPLEXITY_KEY)
      .build(),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(FUNCTIONS_KEY)
      .setFallbackMetricKey(COMPLEXITY_KEY)
      .build());

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public ComplexityMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(FORMULAS))
        .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Aggregation of complexity measures";
  }
}
