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
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.formula.counter.IntValue;
import org.sonar.ce.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.ce.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredVariationFormula;
import org.sonar.ce.task.projectanalysis.formula.coverage.SingleWithUncoveredMetricKeys;
import org.sonar.ce.task.projectanalysis.formula.coverage.SingleWithUncoveredVariationFormula;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

import static java.lang.Math.min;
import static org.sonar.api.measures.CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_UNCOVERED_LINES_KEY;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * Computes measures related to the New Coverage. These measures do not have values, only variations.
 */
public class NewCoverageMeasuresStep implements ComputationStep {

  private static final List<Formula> FORMULAS = ImmutableList.of(
    // UT coverage
    new NewCoverageFormula(),
    new NewBranchCoverageFormula(),
    new NewLineCoverageFormula());

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final NewLinesRepository newLinesRepository;
  private final BatchReportReader reportReader;

  public NewCoverageMeasuresStep(TreeRootHolder treeRootHolder,
    MeasureRepository measureRepository, MetricRepository metricRepository, NewLinesRepository newLinesRepository, BatchReportReader reportReader) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.newLinesRepository = newLinesRepository;
    this.reportReader = reportReader;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .buildFor(
          Iterables.concat(NewLinesAndConditionsCoverageFormula.from(newLinesRepository, reportReader), FORMULAS)))
            .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Compute new coverage";
  }

  private static class NewCoverageFormula extends LinesAndConditionsWithUncoveredVariationFormula {
    NewCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          NEW_LINES_TO_COVER_KEY, NEW_CONDITIONS_TO_COVER_KEY,
          NEW_UNCOVERED_LINES_KEY, NEW_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_COVERAGE_KEY);
    }
  }

  private static class NewBranchCoverageFormula extends SingleWithUncoveredVariationFormula {
    NewBranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(NEW_CONDITIONS_TO_COVER_KEY, NEW_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_BRANCH_COVERAGE_KEY);
    }
  }

  private static class NewLineCoverageFormula extends SingleWithUncoveredVariationFormula {
    NewLineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(NEW_LINES_TO_COVER_KEY, NEW_UNCOVERED_LINES_KEY),
        CoreMetrics.NEW_LINE_COVERAGE_KEY);
    }
  }

  public static class NewLinesAndConditionsCoverageFormula implements Formula<NewCoverageCounter> {
    private final NewLinesRepository newLinesRepository;
    private final BatchReportReader reportReader;

    private NewLinesAndConditionsCoverageFormula(NewLinesRepository newLinesRepository, BatchReportReader reportReader) {
      this.newLinesRepository = newLinesRepository;
      this.reportReader = reportReader;
    }

    public static Iterable<Formula<NewCoverageCounter>> from(NewLinesRepository newLinesRepository, BatchReportReader reportReader) {
      return Collections.singleton(new NewLinesAndConditionsCoverageFormula(newLinesRepository, reportReader));
    }

    @Override
    public NewCoverageCounter createNewCounter() {
      return new NewCoverageCounter(newLinesRepository, reportReader);
    }

    @Override
    public Optional<Measure> createMeasure(NewCoverageCounter counter, CreateMeasureContext context) {
      if (counter.hasNewCode()) {
        int value = computeValueForMetric(counter, context.getMetric());
        return Optional.of(newMeasureBuilder().setVariation(value).createNoValue());
      }
      return Optional.empty();
    }

    private static int computeValueForMetric(NewCoverageCounter counter, Metric metric) {
      if (metric.getKey().equals(NEW_LINES_TO_COVER_KEY)) {
        return counter.getNewLines();
      }
      if (metric.getKey().equals(NEW_UNCOVERED_LINES_KEY)) {
        return counter.getNewLines() - counter.getNewCoveredLines();
      }
      if (metric.getKey().equals(NEW_CONDITIONS_TO_COVER_KEY)) {
        return counter.getNewConditions();
      }
      if (metric.getKey().equals(NEW_UNCOVERED_CONDITIONS_KEY)) {
        return counter.getNewConditions() - counter.getNewCoveredConditions();
      }
      throw new IllegalArgumentException("Unsupported metric " + metric.getKey());
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {
        NEW_LINES_TO_COVER_KEY,
        NEW_UNCOVERED_LINES_KEY,
        NEW_CONDITIONS_TO_COVER_KEY,
        NEW_UNCOVERED_CONDITIONS_KEY
      };
    }
  }

  public static final class NewCoverageCounter implements Counter<NewCoverageCounter> {
    private final IntValue newLines = new IntValue();
    private final IntValue newCoveredLines = new IntValue();
    private final IntValue newConditions = new IntValue();
    private final IntValue newCoveredConditions = new IntValue();
    private final NewLinesRepository newLinesRepository;
    private final BatchReportReader reportReader;

    NewCoverageCounter(NewLinesRepository newLinesRepository, BatchReportReader reportReader) {
      this.newLinesRepository = newLinesRepository;
      this.reportReader = reportReader;
    }

    @Override
    public void aggregate(NewCoverageCounter counter) {
      newLines.increment(counter.newLines);
      newCoveredLines.increment(counter.newCoveredLines);
      newConditions.increment(counter.newConditions);
      newCoveredConditions.increment(counter.newCoveredConditions);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component component = context.getLeaf();
      if (component.getType() != Component.Type.FILE) {
        return;
      }
      Optional<Set<Integer>> newLinesSet = newLinesRepository.getNewLines(component);
      if (!newLinesSet.isPresent()) {
        return;
      }

      newLines.increment(0);
      newCoveredLines.increment(0);
      newConditions.increment(0);
      newCoveredConditions.increment(0);

      try (CloseableIterator<ScannerReport.LineCoverage> lineCoverage = reportReader.readComponentCoverage(component.getReportAttributes().getRef())) {
        while (lineCoverage.hasNext()) {
          final ScannerReport.LineCoverage line = lineCoverage.next();
          int lineId = line.getLine();
          if (newLinesSet.get().contains(lineId)) {
            if (line.getHasHitsCase() == ScannerReport.LineCoverage.HasHitsCase.HITS) {
              newLines.increment(1);
              if (line.getHits()) {
                newCoveredLines.increment(1);
              }
            }
            if (line.getHasCoveredConditionsCase() == ScannerReport.LineCoverage.HasCoveredConditionsCase.COVERED_CONDITIONS) {
              newConditions.increment(line.getConditions());
              newCoveredConditions.increment(min(line.getCoveredConditions(), line.getConditions()));
            }
          }
        }
      }

    }

    boolean hasNewCode() {
      return newLines.isSet();
    }

    int getNewLines() {
      return newLines.getValue();
    }

    int getNewCoveredLines() {
      return newCoveredLines.getValue();
    }

    int getNewConditions() {
      return newConditions.getValue();
    }

    int getNewCoveredConditions() {
      return newCoveredConditions.getValue();
    }
  }

}
