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
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.formula.Counter;
import org.sonar.server.computation.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.server.computation.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.server.computation.task.projectanalysis.formula.Formula;
import org.sonar.server.computation.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.formula.VariationSumFormula;
import org.sonar.server.computation.task.projectanalysis.formula.counter.IntValue;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredMetricKeys;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.LinesAndConditionsWithUncoveredVariationFormula;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.SingleWithUncoveredMetricKeys;
import org.sonar.server.computation.task.projectanalysis.formula.coverage.SingleWithUncoveredVariationFormula;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

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
  private final PeriodHolder periodHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  @CheckForNull
  private final ScmInfoRepository scmInfoRepository;

  /**
   * Constructor used when processing a Report (ie. a {@link BatchReportReader} instance is available in the container)
   */
  public NewCoverageMeasuresStep(TreeRootHolder treeRootHolder, PeriodHolder periodHolder,
    MeasureRepository measureRepository, final MetricRepository metricRepository, ScmInfoRepository scmInfoRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodHolder = periodHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
  }

  /**
   * Constructor used when processing Views (ie. no {@link BatchReportReader} instance is available in the container)
   */
  public NewCoverageMeasuresStep(TreeRootHolder treeRootHolder, PeriodHolder periodHolder,
    MeasureRepository measureRepository, final MetricRepository metricRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodHolder = periodHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.scmInfoRepository = null;
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .withVariationSupport(periodHolder)
        .buildFor(
          Iterables.concat(NewLinesAndConditionsCoverageFormula.from(scmInfoRepository), FORMULAS)))
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
      return Collections.singleton(new NewLinesAndConditionsCoverageFormula(scmInfoRepository));
    }

    /**
     * Creates a List of {@link org.sonar.server.computation.task.projectanalysis.formula.SumFormula.IntSumFormula} for each
     * metric key of the specified {@link NewCoverageOutputMetricKeys} instance.
     */
    private static Iterable<Formula<?>> variationSumFormulas(NewCoverageOutputMetricKeys outputMetricKeys) {
      return ImmutableList.of(
        new VariationSumFormula(outputMetricKeys.getNewLinesToCover()),
        new VariationSumFormula(outputMetricKeys.getNewUncoveredLines()),
        new VariationSumFormula(outputMetricKeys.getNewConditionsToCover()),
        new VariationSumFormula(outputMetricKeys.getNewUncoveredConditions()));
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
      if (counter.hasNewCode()) {
        int value = computeValueForMetric(counter, context.getMetric());
        return Optional.of(newMeasureBuilder().setVariation(value).createNoValue());
      }
      return Optional.absent();
    }

    private int computeValueForMetric(NewCoverageCounter counter, Metric metric) {
      if (metric.getKey().equals(outputMetricKeys.getNewLinesToCover())) {
        return counter.getNewLines();
      }
      if (metric.getKey().equals(outputMetricKeys.getNewUncoveredLines())) {
        return counter.getNewLines() - counter.getNewCoveredLines();
      }
      if (metric.getKey().equals(outputMetricKeys.getNewConditionsToCover())) {
        return counter.getNewConditions();
      }
      if (metric.getKey().equals(outputMetricKeys.getNewUncoveredConditions())) {
        return counter.getNewConditions() - counter.getNewCoveredConditions();
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

  public static final class NewCoverageCounter implements Counter<NewCoverageCounter> {
    private final IntValue newLines = new IntValue();
    private final IntValue newCoveredLines = new IntValue();
    private final IntValue newConditions = new IntValue();
    private final IntValue newCoveredConditions = new IntValue();
    private final ScmInfoRepository scmInfoRepository;
    private final NewCoverageInputMetricKeys metricKeys;

    public NewCoverageCounter(ScmInfoRepository scmInfoRepository, NewCoverageInputMetricKeys metricKeys) {
      this.scmInfoRepository = scmInfoRepository;
      this.metricKeys = metricKeys;
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
      Component fileComponent = context.getLeaf();
      Optional<ScmInfo> scmInfoOptional = scmInfoRepository.getScmInfo(fileComponent);
      if (!scmInfoOptional.isPresent() || !context.hasPeriod()) {
        return;
      }
      ScmInfo componentScm = scmInfoOptional.get();

      newLines.increment(0);
      newCoveredLines.increment(0);
      newConditions.increment(0);
      newCoveredConditions.increment(0);

      Optional<Measure> hitsByLineMeasure = context.getMeasure(metricKeys.getCoverageLineHitsData());
      Map<Integer, Integer> hitsByLine = parseCountByLine(hitsByLineMeasure);
      Map<Integer, Integer> conditionsByLine = parseCountByLine(context.getMeasure(metricKeys.getConditionsByLine()));
      Map<Integer, Integer> coveredConditionsByLine = parseCountByLine(context.getMeasure(metricKeys.getCoveredConditionsByLine()));

      for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
        int lineId = entry.getKey();
        int hits = entry.getValue();
        int conditions = (Integer) ObjectUtils.defaultIfNull(conditionsByLine.get(lineId), 0);
        int coveredConditions = (Integer) ObjectUtils.defaultIfNull(coveredConditionsByLine.get(lineId), 0);
        if (componentScm.hasChangesetForLine(lineId)) {
          long date = componentScm.getChangesetForLine(lineId).getDate();
          analyze(context.getPeriod(), date, hits, conditions, coveredConditions);
        }
      }
    }

    private static Map<Integer, Integer> parseCountByLine(Optional<Measure> measure) {
      if (measure.isPresent() && measure.get().getValueType() != Measure.ValueType.NO_VALUE) {
        return KeyValueFormat.parseIntInt(measure.get().getStringValue());
      }
      return Collections.emptyMap();
    }

    void analyze(Period period, long lineDate, int hits, int conditions, int coveredConditions) {
      if (isLineInPeriod(lineDate, period)) {
        incrementLines(hits);
        incrementConditions(conditions, coveredConditions);
      }
    }

    /**
     * A line belongs to a Period if its date is older than the SNAPSHOT's date of the period.
     */
    private static boolean isLineInPeriod(long lineDate, Period period) {
      return lineDate > period.getSnapshotDate();
    }

    private void incrementLines(int hits) {
      newLines.increment(1);
      if (hits > 0) {
        newCoveredLines.increment(1);
      }
    }

    private void incrementConditions(int conditions, int coveredConditions) {
      newConditions.increment(conditions);
      if (conditions > 0) {
        newCoveredConditions.increment(coveredConditions);
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
