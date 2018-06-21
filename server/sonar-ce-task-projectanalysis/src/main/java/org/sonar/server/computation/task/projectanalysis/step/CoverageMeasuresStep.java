/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.formula.Formula;
import org.sonar.server.computation.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredFormula;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.SingleWithUncoveredFormula;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.SingleWithUncoveredMetricKeys;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.BRANCH_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.LINE_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES_KEY;
import static org.sonar.server.computation.task.projectanalysis.formula.SumFormula.createIntSumFormula;

/**
 * Computes coverage measures on files and then aggregates them on higher components.
 */
public class CoverageMeasuresStep implements ComputationStep {
  private static final ImmutableList<Formula> COVERAGE_FORMULAS = ImmutableList.of(
    createIntSumFormula(LINES_TO_COVER_KEY),
    createIntSumFormula(UNCOVERED_LINES_KEY),
    createIntSumFormula(CONDITIONS_TO_COVER_KEY),
    createIntSumFormula(UNCOVERED_CONDITIONS_KEY),
    new CodeCoverageFormula(),
    new BranchCoverageFormula(),
    new LineCoverageFormula());

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public CoverageMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(COVERAGE_FORMULAS))
        .visit(treeRootHolder.getRoot());
  }

  private static class CodeCoverageFormula extends LinesAndConditionsWithUncoveredFormula {
    public CodeCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          LINES_TO_COVER_KEY, CONDITIONS_TO_COVER_KEY,
          UNCOVERED_LINES_KEY, UNCOVERED_CONDITIONS_KEY),
        COVERAGE_KEY);
    }
  }

  private static class BranchCoverageFormula extends SingleWithUncoveredFormula {
    public BranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CONDITIONS_TO_COVER_KEY, UNCOVERED_CONDITIONS_KEY),
        BRANCH_COVERAGE_KEY);
    }
  }

  private static class LineCoverageFormula extends SingleWithUncoveredFormula {
    public LineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(LINES_TO_COVER_KEY, UNCOVERED_LINES_KEY),
        LINE_COVERAGE_KEY);
    }
  }

  @Override
  public String getDescription() {
    return "Compute coverage measures";
  }
}
