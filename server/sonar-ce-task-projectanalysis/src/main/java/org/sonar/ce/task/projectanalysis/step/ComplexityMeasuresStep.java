/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.ce.task.projectanalysis.formula.SumFormula.createIntSumFormula;

/**
 * Computes complexity measures on files and then aggregates them on higher components.
 */
public class ComplexityMeasuresStep implements ComputationStep {
  private static final List<Formula<?>> FORMULAS = List.of(
    createIntSumFormula(COMPLEXITY_KEY),
    createIntSumFormula(COGNITIVE_COMPLEXITY_KEY)
  );

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
