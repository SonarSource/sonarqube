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
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredFormula;
import org.sonar.ce.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.ce.task.projectanalysis.formula.coverage.SingleWithUncoveredFormula;
import org.sonar.ce.task.projectanalysis.formula.coverage.SingleWithUncoveredMetricKeys;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

import static java.lang.Math.min;
import static org.sonar.api.measures.CoreMetrics.BRANCH_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.LINE_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES_KEY;
import static org.sonar.ce.task.projectanalysis.formula.SumFormula.createIntSumFormula;

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
  private final BatchReportReader reportReader;
  private final Metric linesToCoverMetric;
  private final Metric uncoveredLinesMetric;
  private final Metric conditionsToCoverMetric;
  private final Metric uncoveredConditionsMetric;

  /**
   * Constructor used when processing a Report (ie. a {@link BatchReportReader} instance is available in the container)
   */
  public CoverageMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository, BatchReportReader reportReader) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.reportReader = reportReader;
    this.linesToCoverMetric = metricRepository.getByKey(LINES_TO_COVER_KEY);
    this.uncoveredLinesMetric = metricRepository.getByKey(UNCOVERED_LINES_KEY);
    this.conditionsToCoverMetric = metricRepository.getByKey(CONDITIONS_TO_COVER_KEY);
    this.uncoveredConditionsMetric = metricRepository.getByKey(UNCOVERED_CONDITIONS_KEY);
  }

  /**
   * Constructor used when processing Views (ie. no {@link BatchReportReader} instance is available in the container)
   */
  public CoverageMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.linesToCoverMetric = metricRepository.getByKey(LINES_TO_COVER_KEY);
    this.uncoveredLinesMetric = metricRepository.getByKey(UNCOVERED_LINES_KEY);
    this.conditionsToCoverMetric = metricRepository.getByKey(CONDITIONS_TO_COVER_KEY);
    this.uncoveredConditionsMetric = metricRepository.getByKey(UNCOVERED_CONDITIONS_KEY);
    this.reportReader = null;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (reportReader != null) {
      new DepthTraversalTypeAwareCrawler(new FileCoverageVisitor(reportReader)).visit(treeRootHolder.getReportTreeRoot());
    }
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(COVERAGE_FORMULAS))
        .visit(treeRootHolder.getReportTreeRoot());
  }

  private class FileCoverageVisitor extends TypeAwareVisitorAdapter {

    private final BatchReportReader reportReader;

    private FileCoverageVisitor(BatchReportReader reportReader) {
      super(CrawlerDepthLimit.FILE, Order.POST_ORDER);
      this.reportReader = reportReader;
    }

    @Override
    public void visitFile(Component file) {
      try (CloseableIterator<ScannerReport.LineCoverage> lineCoverage = reportReader.readComponentCoverage(file.getReportAttributes().getRef())) {
        int linesToCover = 0;
        int coveredLines = 0;
        int conditionsToCover = 0;
        int coveredConditions = 0;
        while (lineCoverage.hasNext()) {
          final ScannerReport.LineCoverage line = lineCoverage.next();
          if (line.getHasHitsCase() == ScannerReport.LineCoverage.HasHitsCase.HITS) {
            linesToCover++;
            if (line.getHits()) {
              coveredLines++;
            }
          }
          if (line.getHasCoveredConditionsCase() == ScannerReport.LineCoverage.HasCoveredConditionsCase.COVERED_CONDITIONS) {
            conditionsToCover += line.getConditions();
            coveredConditions += min(line.getCoveredConditions(), line.getConditions());
          }
        }
        if (linesToCover > 0) {
          measureRepository.add(file, linesToCoverMetric, Measure.newMeasureBuilder().create(linesToCover));
          measureRepository.add(file, uncoveredLinesMetric, Measure.newMeasureBuilder().create(linesToCover - coveredLines));
        }
        if (conditionsToCover > 0) {
          measureRepository.add(file, conditionsToCoverMetric, Measure.newMeasureBuilder().create(conditionsToCover));
          measureRepository.add(file, uncoveredConditionsMetric, Measure.newMeasureBuilder().create(conditionsToCover - coveredConditions));
        }
      }
    }
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
