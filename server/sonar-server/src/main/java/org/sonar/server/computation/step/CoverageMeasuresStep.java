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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredFormula;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.formula.coverage.SingleWithUncoveredFormula;
import org.sonar.server.computation.formula.coverage.SingleWithUncoveredMetricKeys;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

/**
 * Computes coverage measures on files and then aggregates them on higher components.
 */
public class CoverageMeasuresStep implements ComputationStep {
  private static final ImmutableList<Formula> COVERAGE_FORMULAS = ImmutableList.<Formula>of(
    // code
    new CodeCoverageFormula(),
    new ItCoverageFormula(),
    new OverallCodeCoverageFormula(),
    // branch
    new BranchCoverageFormula(),
    new ItBranchCoverageFormula(),
    new OverallBranchCoverageFormula(),
    // line
    new LineCoverageFormula(),
    new ItLineCoverageFormula(),
    new OverallLineCoverageFormula()
    );

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
    FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(COVERAGE_FORMULAS)
      .visit(treeRootHolder.getRoot());
  }

  private static class CodeCoverageFormula extends LinesAndConditionsWithUncoveredFormula {
    public CodeCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.CONDITIONS_TO_COVER_KEY,
          CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY
        ),
        CoreMetrics.COVERAGE_KEY);
    }
  }

  private static class ItCoverageFormula extends LinesAndConditionsWithUncoveredFormula {
    private ItCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          CoreMetrics.IT_LINES_TO_COVER_KEY, CoreMetrics.IT_CONDITIONS_TO_COVER_KEY,
          CoreMetrics.IT_UNCOVERED_LINES_KEY, CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY
        ),
        CoreMetrics.IT_COVERAGE_KEY);
    }
  }

  private static class OverallCodeCoverageFormula extends LinesAndConditionsWithUncoveredFormula {
    public OverallCodeCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          CoreMetrics.OVERALL_LINES_TO_COVER_KEY, CoreMetrics.OVERALL_CONDITIONS_TO_COVER_KEY,
          CoreMetrics.OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS_KEY
        ),
        CoreMetrics.OVERALL_COVERAGE_KEY);
    }
  }

  private static class BranchCoverageFormula extends SingleWithUncoveredFormula {
    public BranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CoreMetrics.CONDITIONS_TO_COVER_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY
        ),
        CoreMetrics.BRANCH_COVERAGE_KEY);
    }
  }

  private static class ItBranchCoverageFormula extends SingleWithUncoveredFormula {
    public ItBranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CoreMetrics.IT_CONDITIONS_TO_COVER_KEY, CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY
        ),
        CoreMetrics.IT_BRANCH_COVERAGE_KEY);
    }
  }

  private static class OverallBranchCoverageFormula extends SingleWithUncoveredFormula {
    public OverallBranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(CoreMetrics.OVERALL_CONDITIONS_TO_COVER_KEY, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.OVERALL_BRANCH_COVERAGE_KEY);
    }
  }

  private static class LineCoverageFormula extends SingleWithUncoveredFormula {
    public LineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.UNCOVERED_LINES_KEY),
        CoreMetrics.LINE_COVERAGE_KEY);
    }
  }

  private static class ItLineCoverageFormula extends SingleWithUncoveredFormula {
    public ItLineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(CoreMetrics.IT_LINES_TO_COVER_KEY, CoreMetrics.IT_UNCOVERED_LINES_KEY),
        CoreMetrics.IT_LINE_COVERAGE_KEY);
    }
  }

  private static class OverallLineCoverageFormula extends SingleWithUncoveredFormula {
    public OverallLineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CoreMetrics.OVERALL_LINES_TO_COVER_KEY, CoreMetrics.OVERALL_UNCOVERED_LINES_KEY
        ),
        CoreMetrics.OVERALL_LINE_COVERAGE_KEY);
    }
  }

  @Override
  public String getDescription() {
    return "Aggregation of coverage measures";
  }
}
