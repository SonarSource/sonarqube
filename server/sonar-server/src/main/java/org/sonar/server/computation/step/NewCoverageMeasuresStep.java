/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.CounterInitializationContext;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.formula.VariationSumFormula;
import org.sonar.server.computation.formula.counter.IntVariationValue;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.formula.coverage.LinesAndConditionsWithUncoveredVariationFormula;
import org.sonar.server.computation.formula.coverage.SingleWithUncoveredMetricKeys;
import org.sonar.server.computation.formula.coverage.SingleWithUncoveredVariationFormula;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;
import org.sonar.server.computation.scm.ScmInfo;
import org.sonar.server.computation.scm.ScmInfoRepository;

import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.period.PeriodPredicates.viewsRestrictedPeriods;

/**
 * Computes measures related to the New Coverage. These measures do not have values, only variations.
 */
public class NewCoverageMeasuresStep implements ComputationStep {

  private static final List<Formula> FORMULAS = ImmutableList.<Formula>of(
    // UT coverage
    new NewCoverageFormula(),
    new NewBranchCoverageFormula(),
    new NewLineCoverageFormula(),
    // IT File coverage
    new NewItCoverageFormula(),
    new NewItBranchCoverageFormula(),
    new NewItLinesCoverageFormula(),
    // Overall coverage
    new NewOverallCodeCoverageFormula(),
    new NewOverallBranchCoverageFormula(),
    new NewOverallLineCoverageFormula());

  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  @CheckForNull
  private final ScmInfoRepository scmInfoRepository;

  /**
   * Constructor used when processing a Report (ie. a {@link BatchReportReader} instance is available in the container)
   */
  public NewCoverageMeasuresStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder,
    MeasureRepository measureRepository, final MetricRepository metricRepository, ScmInfoRepository scmInfoRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
  }

  /**
   * Constructor used when processing Views (ie. no {@link BatchReportReader} instance is available in the container)
   */
  public NewCoverageMeasuresStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder,
    MeasureRepository measureRepository, final MetricRepository metricRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.scmInfoRepository = null;
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .withVariationSupport(periodsHolder)
        .buildFor(
          Iterables.concat(
            NewLinesAndConditionsCoverageFormula.from(scmInfoRepository),
            NewItLinesAndConditionsCoverageFormula.from(scmInfoRepository),
            NewOverallLinesAndConditionsCoverageFormula.from(scmInfoRepository),
            FORMULAS)))
      .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Compute new coverage";
  }

  private static class NewLinesAndConditionsCoverageFormula extends NewLinesAndConditionsFormula {

    private static final NewCoverageOutputMetricKeys OUTPUT_METRIC_KEYS = new NewCoverageOutputMetricKeys(
      CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_UNCOVERED_LINES_KEY,
      CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY);
    private static final Iterable<Formula<?>> VIEWS_FORMULAS = variationSumFormulas(OUTPUT_METRIC_KEYS);

    private NewLinesAndConditionsCoverageFormula(ScmInfoRepository scmInfoRepository) {
      super(scmInfoRepository,
        new NewCoverageInputMetricKeys(
          CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY, CoreMetrics.CONDITIONS_BY_LINE_KEY, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY),
        OUTPUT_METRIC_KEYS);
    }

    public static Iterable<Formula<?>> from(@Nullable ScmInfoRepository scmInfoRepository) {
      if (scmInfoRepository == null) {
        return VIEWS_FORMULAS;
      }
      return Collections.<Formula<?>>singleton(new NewLinesAndConditionsCoverageFormula(scmInfoRepository));
    }
  }

  private static class NewCoverageFormula extends LinesAndConditionsWithUncoveredVariationFormula {
    public NewCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
          CoreMetrics.NEW_UNCOVERED_LINES_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_COVERAGE_KEY);
    }
  }

  private static class NewBranchCoverageFormula extends SingleWithUncoveredVariationFormula {
    public NewBranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_BRANCH_COVERAGE_KEY);
    }
  }

  private static class NewLineCoverageFormula extends SingleWithUncoveredVariationFormula {
    public NewLineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_UNCOVERED_LINES_KEY),
        CoreMetrics.NEW_LINE_COVERAGE_KEY);
    }
  }

  private static class NewItLinesAndConditionsCoverageFormula extends NewLinesAndConditionsFormula {

    private static final NewCoverageOutputMetricKeys OUTPUT_METRIC_KEYS = new NewCoverageOutputMetricKeys(
      CoreMetrics.NEW_IT_LINES_TO_COVER_KEY, CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY,
      CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY);
    private static final Iterable<Formula<?>> VIEWS_FORMULAS = variationSumFormulas(OUTPUT_METRIC_KEYS);

    private NewItLinesAndConditionsCoverageFormula(ScmInfoRepository scmInfoRepository) {
      super(scmInfoRepository,
        new NewCoverageInputMetricKeys(
          CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY, CoreMetrics.IT_CONDITIONS_BY_LINE_KEY, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY),
        OUTPUT_METRIC_KEYS);
    }

    public static Iterable<Formula<?>> from(@Nullable ScmInfoRepository scmInfoRepository) {
      if (scmInfoRepository == null) {
        return VIEWS_FORMULAS;
      }
      return Collections.<Formula<?>>singleton(new NewItLinesAndConditionsCoverageFormula(scmInfoRepository));
    }
  }

  private static class NewItCoverageFormula extends LinesAndConditionsWithUncoveredVariationFormula {
    private NewItCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          CoreMetrics.NEW_IT_LINES_TO_COVER_KEY, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY,
          CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_IT_COVERAGE_KEY);
    }
  }

  private static class NewItBranchCoverageFormula extends SingleWithUncoveredVariationFormula {
    public NewItBranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_IT_BRANCH_COVERAGE_KEY);
    }
  }

  private static class NewItLinesCoverageFormula extends SingleWithUncoveredVariationFormula {
    public NewItLinesCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(CoreMetrics.NEW_IT_LINES_TO_COVER_KEY, CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY),
        CoreMetrics.NEW_IT_LINE_COVERAGE_KEY);
    }
  }

  private static class NewOverallLinesAndConditionsCoverageFormula extends NewLinesAndConditionsFormula {

    private static final NewCoverageOutputMetricKeys OUTPUT_METRIC_KEYS = new NewCoverageOutputMetricKeys(
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY,
      CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY);
    private static final Iterable<Formula<?>> VIEWS_FORMULAS = variationSumFormulas(OUTPUT_METRIC_KEYS);

    private NewOverallLinesAndConditionsCoverageFormula(ScmInfoRepository scmInfoRepository) {
      super(scmInfoRepository,
        new NewCoverageInputMetricKeys(
          CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA_KEY, CoreMetrics.OVERALL_CONDITIONS_BY_LINE_KEY, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY),
        OUTPUT_METRIC_KEYS);
    }

    public static Iterable<Formula<?>> from(@Nullable ScmInfoRepository scmInfoRepository) {
      if (scmInfoRepository == null) {
        return VIEWS_FORMULAS;
      }
      return Collections.<Formula<?>>singleton(new NewOverallLinesAndConditionsCoverageFormula(scmInfoRepository));
    }
  }

  private static class NewOverallCodeCoverageFormula extends LinesAndConditionsWithUncoveredVariationFormula {
    public NewOverallCodeCoverageFormula() {
      super(
        new LinesAndConditionsWithUncoveredMetricKeys(
          CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY,
          CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_OVERALL_COVERAGE_KEY);
    }
  }

  private static class NewOverallBranchCoverageFormula extends SingleWithUncoveredVariationFormula {
    public NewOverallBranchCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY),
        CoreMetrics.NEW_OVERALL_BRANCH_COVERAGE_KEY);
    }
  }

  private static class NewOverallLineCoverageFormula extends SingleWithUncoveredVariationFormula {
    public NewOverallLineCoverageFormula() {
      super(
        new SingleWithUncoveredMetricKeys(
          CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY),
        CoreMetrics.NEW_OVERALL_LINE_COVERAGE_KEY);
    }
  }

  /**
   * Creates a List of {@link org.sonar.server.computation.formula.SumFormula.IntSumFormula} for each
   * metric key of the specified {@link NewCoverageOutputMetricKeys} instance.
   */
  private static Iterable<Formula<?>> variationSumFormulas(NewCoverageOutputMetricKeys outputMetricKeys) {
    return ImmutableList.<Formula<?>>of(
      new VariationSumFormula(outputMetricKeys.getNewLinesToCover(), viewsRestrictedPeriods()),
      new VariationSumFormula(outputMetricKeys.getNewUncoveredLines(), viewsRestrictedPeriods()),
      new VariationSumFormula(outputMetricKeys.getNewConditionsToCover(), viewsRestrictedPeriods()),
      new VariationSumFormula(outputMetricKeys.getNewUncoveredConditions(), viewsRestrictedPeriods()));
  }

  public static class NewLinesAndConditionsFormula implements Formula<NewCoverageCounter> {
    private final ScmInfoRepository scmInfoRepository;
    private final NewCoverageInputMetricKeys inputMetricKeys;
    private final NewCoverageOutputMetricKeys outputMetricKeys;

    public NewLinesAndConditionsFormula(ScmInfoRepository scmInfoRepository, NewCoverageInputMetricKeys inputMetricKeys, NewCoverageOutputMetricKeys outputMetricKeys) {
      this.scmInfoRepository = scmInfoRepository;
      this.inputMetricKeys = inputMetricKeys;
      this.outputMetricKeys = outputMetricKeys;
    }

    @Override
    public NewCoverageCounter createNewCounter() {
      return new NewCoverageCounter(scmInfoRepository, inputMetricKeys);
    }

    @Override
    public Optional<Measure> createMeasure(NewCoverageCounter counter, CreateMeasureContext context) {
      MeasureVariations.Builder builder = MeasureVariations.newMeasureVariationsBuilder();
      for (Period period : context.getPeriods()) {
        if (counter.hasNewCode(period)) {
          int value = computeValueForMetric(counter, period, context.getMetric());
          builder.setVariation(period, value);
        }
      }
      if (builder.isEmpty()) {
        return Optional.absent();
      }
      return Optional.of(newMeasureBuilder().setVariations(builder.build()).createNoValue());
    }

    private int computeValueForMetric(NewCoverageCounter counter, Period period, Metric metric) {
      if (metric.getKey().equals(outputMetricKeys.getNewLinesToCover())) {
        return counter.getNewLines(period);
      }
      if (metric.getKey().equals(outputMetricKeys.getNewUncoveredLines())) {
        return counter.getNewLines(period) - counter.getNewCoveredLines(period);
      }
      if (metric.getKey().equals(outputMetricKeys.getNewConditionsToCover())) {
        return counter.getNewConditions(period);
      }
      if (metric.getKey().equals(outputMetricKeys.getNewUncoveredConditions())) {
        return counter.getNewConditions(period) - counter.getNewCoveredConditions(period);
      }
      throw new IllegalArgumentException("Unsupported metric " + metric.getKey());
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {
        outputMetricKeys.getNewLinesToCover(),
        outputMetricKeys.getNewUncoveredLines(),
        outputMetricKeys.getNewConditionsToCover(),
        outputMetricKeys.getNewUncoveredConditions()
      };
    }
  }

  public static final class NewCoverageCounter implements org.sonar.server.computation.formula.Counter<NewCoverageCounter> {
    private final IntVariationValue.Array newLines = IntVariationValue.newArray();
    private final IntVariationValue.Array newCoveredLines = IntVariationValue.newArray();
    private final IntVariationValue.Array newConditions = IntVariationValue.newArray();
    private final IntVariationValue.Array newCoveredConditions = IntVariationValue.newArray();
    private final ScmInfoRepository scmInfoRepository;
    private final NewCoverageInputMetricKeys metricKeys;

    public NewCoverageCounter(ScmInfoRepository scmInfoRepository, NewCoverageInputMetricKeys metricKeys) {
      this.scmInfoRepository = scmInfoRepository;
      this.metricKeys = metricKeys;
    }

    @Override
    public void aggregate(NewCoverageCounter counter) {
      newLines.incrementAll(counter.newLines);
      newCoveredLines.incrementAll(counter.newCoveredLines);
      newConditions.incrementAll(counter.newConditions);
      newCoveredConditions.incrementAll(counter.newCoveredConditions);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component fileComponent = context.getLeaf();
      Optional<ScmInfo> scmInfoOptional = scmInfoRepository.getScmInfo(fileComponent);
      if (!scmInfoOptional.isPresent()) {
        return;
      }
      ScmInfo componentScm = scmInfoOptional.get();

      Optional<Measure> hitsByLineMeasure = context.getMeasure(metricKeys.getCoverageLineHitsData());
      if (!hitsByLineMeasure.isPresent() || hitsByLineMeasure.get().getValueType() == Measure.ValueType.NO_VALUE) {
        return;
      }

      Map<Integer, Integer> hitsByLine = parseCountByLine(hitsByLineMeasure);
      Map<Integer, Integer> conditionsByLine = parseCountByLine(context.getMeasure(metricKeys.getConditionsByLine()));
      Map<Integer, Integer> coveredConditionsByLine = parseCountByLine(context.getMeasure(metricKeys.getCoveredConditionsByLine()));

      for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
        int lineId = entry.getKey();
        int hits = entry.getValue();
        int conditions = (Integer) ObjectUtils.defaultIfNull(conditionsByLine.get(lineId), 0);
        int coveredConditions = (Integer) ObjectUtils.defaultIfNull(coveredConditionsByLine.get(lineId), 0);
        long date = componentScm.getChangesetForLine(lineId).getDate();
        analyze(context.getPeriods(), date, hits, conditions, coveredConditions);
      }
    }

    private static Map<Integer, Integer> parseCountByLine(Optional<Measure> measure) {
      if (measure.isPresent() && measure.get().getValueType() != Measure.ValueType.NO_VALUE) {
        return KeyValueFormat.parseIntInt(measure.get().getStringValue());
      }
      return Collections.emptyMap();
    }

    public void analyze(List<Period> periods, @Nullable Long lineDate, int hits, int conditions, int coveredConditions) {
      if (lineDate == null) {
        return;
      }
      for (Period period : periods) {
        if (isLineInPeriod(lineDate, period)) {
          incrementLines(period, hits);
          incrementConditions(period, conditions, coveredConditions);
        }
      }
    }

    /**
     * A line belongs to a Period if its date is older than the SNAPSHOT's date of the period.
     */
    private static boolean isLineInPeriod(long lineDate, Period period) {
      return lineDate > period.getSnapshotDate();
    }

    private void incrementLines(Period period, int hits) {
      newLines.increment(period, 1);
      if (hits > 0) {
        newCoveredLines.increment(period, 1);
      }
    }

    private void incrementConditions(Period period, int conditions, int coveredConditions) {
      newConditions.increment(period, conditions);
      if (conditions > 0) {
        newCoveredConditions.increment(period, coveredConditions);
      }
    }

    public boolean hasNewCode(Period period) {
      return newLines.get(period).isSet();
    }

    public int getNewLines(Period period) {
      return newLines.get(period).getValue();
    }

    public int getNewCoveredLines(Period period) {
      return newCoveredLines.get(period).getValue();
    }

    public int getNewConditions(Period period) {
      return newConditions.get(period).getValue();
    }

    public int getNewCoveredConditions(Period period) {
      return newCoveredConditions.get(period).getValue();
    }
  }

  @Immutable
  public static final class NewCoverageOutputMetricKeys {
    private final String newLinesToCover;
    private final String newUncoveredLines;
    private final String newConditionsToCover;
    private final String newUncoveredConditions;

    public NewCoverageOutputMetricKeys(String newLinesToCover, String newUncoveredLines, String newConditionsToCover, String newUncoveredConditions) {
      this.newLinesToCover = newLinesToCover;
      this.newUncoveredLines = newUncoveredLines;
      this.newConditionsToCover = newConditionsToCover;
      this.newUncoveredConditions = newUncoveredConditions;
    }

    public String getNewLinesToCover() {
      return newLinesToCover;
    }

    public String getNewUncoveredLines() {
      return newUncoveredLines;
    }

    public String getNewConditionsToCover() {
      return newConditionsToCover;
    }

    public String getNewUncoveredConditions() {
      return newUncoveredConditions;
    }
  }

  @Immutable
  public static class NewCoverageInputMetricKeys {
    private final String coverageLineHitsData;
    private final String conditionsByLine;
    private final String coveredConditionsByLine;

    public NewCoverageInputMetricKeys(String coverageLineHitsData, String conditionsByLine, String coveredConditionsByLine) {
      this.coverageLineHitsData = coverageLineHitsData;
      this.conditionsByLine = conditionsByLine;
      this.coveredConditionsByLine = coveredConditionsByLine;
    }

    public String getCoverageLineHitsData() {
      return coverageLineHitsData;
    }

    public String getConditionsByLine() {
      return conditionsByLine;
    }

    public String getCoveredConditionsByLine() {
      return coveredConditionsByLine;
    }
  }
}
