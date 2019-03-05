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

import com.google.common.collect.ImmutableList;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.AverageFormula;
import org.sonar.ce.task.projectanalysis.formula.DistributionFormula;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.ce.task.projectanalysis.formula.SumFormula.createIntSumFormula;

/**
 * Computes complexity measures on files and then aggregates them on higher components.
 */
public class ComplexityMeasuresStep implements ComputationStep {

  private static final ImmutableList<Formula> FORMULAS = ImmutableList.of(
    createIntSumFormula(COMPLEXITY_KEY),
    createIntSumFormula(COMPLEXITY_IN_CLASSES_KEY),
    createIntSumFormula(COMPLEXITY_IN_FUNCTIONS_KEY),
    createIntSumFormula(COGNITIVE_COMPLEXITY_KEY),
    new DistributionFormula(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY),
    new DistributionFormula(FILE_COMPLEXITY_DISTRIBUTION_KEY),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(FILE_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_KEY)
      .setByMetricKey(FILES_KEY)
      .build(),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(CLASS_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_CLASSES_KEY)
      .setByMetricKey(CLASSES_KEY)
      .build(),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(FUNCTIONS_KEY)
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
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(FORMULAS))
      .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Compute complexity measures";
  }
}
