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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.measure.newcoverage.NewCoverageMetricKeys;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

/**
 * Computes measures related to the New Coverage. These measures do not have values, only variations.
 */
public class NewCoverageMeasuresStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final BatchReportReader batchReportReader;
  private final MeasureRepository measureRepository;

  private final List<NewCoverageMetrics> newCoverageMetricsList;

  public NewCoverageMeasuresStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder, BatchReportReader batchReportReader,
    MeasureRepository measureRepository, final MetricRepository metricRepository, NewCoverageMetricKeys... newCoverageMetricsList) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.batchReportReader = batchReportReader;
    this.measureRepository = measureRepository;
    this.newCoverageMetricsList = from(asList(newCoverageMetricsList))
      .transform(new Function<NewCoverageMetricKeys, NewCoverageMetrics>() {
        @Nullable
        @Override
        public NewCoverageMetrics apply(@Nullable NewCoverageMetricKeys input) {
          return new NewCoverageMetrics(metricRepository, input);
        }
      })
      .toList();
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareVisitor(FILE, POST_ORDER) {
      @Override
      public void visitFile(Component file) {
        if (!file.getFileAttributes().isUnitTest()) {
          processFileComponent(file);
        }
      }
    }.visit(treeRootHolder.getRoot());
  }

  private void processFileComponent(Component file) {
    for (NewCoverageMetrics newCoverageMetrics : newCoverageMetricsList) {
      Counters counters = new Counters(file, periodsHolder);
      if (populateCounters(newCoverageMetrics, counters)) {
        compute(newCoverageMetrics, counters);
      }
    }
  }

  private boolean populateCounters(NewCoverageMetrics newCoverageMetrics, Counters counters) {
    Component fileComponent = counters.getComponent();
    BatchReport.Changesets componentScm = batchReportReader.readChangesets(fileComponent.getRef());
    if (componentScm == null) {
      return false;
    }

    Optional<Measure> hitsByLineMeasure = measureRepository.getRawMeasure(fileComponent, newCoverageMetrics.coverageLineHitsData);
    if (!hitsByLineMeasure.isPresent() || hitsByLineMeasure.get().getValueType() == Measure.ValueType.NO_VALUE) {
      return false;
    }

    Map<Integer, Integer> hitsByLine = parseCountByLine(hitsByLineMeasure);
    Map<Integer, Integer> conditionsByLine = parseCountByLine(measureRepository.getRawMeasure(fileComponent, newCoverageMetrics.conditionsByLine));
    Map<Integer, Integer> coveredConditionsByLine = parseCountByLine(measureRepository.getRawMeasure(fileComponent, newCoverageMetrics.coveredConditionsByLine));

    for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
      int lineId = entry.getKey();
      int hits = entry.getValue();
      int conditions = (Integer) ObjectUtils.defaultIfNull(conditionsByLine.get(lineId), 0);
      int coveredConditions = (Integer) ObjectUtils.defaultIfNull(coveredConditionsByLine.get(lineId), 0);
      BatchReport.Changesets.Changeset changeset = componentScm.getChangeset(componentScm.getChangesetIndexByLine(lineId - 1));
      Date date = changeset.hasDate() ? new Date(changeset.getDate()) : null;

      counters.analyze(date, hits, conditions, coveredConditions);
    }

    return true;
  }

  private static Map<Integer, Integer> parseCountByLine(Optional<Measure> measure) {
    if (measure.isPresent() && measure.get().getValueType() != Measure.ValueType.NO_VALUE) {
      return KeyValueFormat.parseIntInt(measure.get().getStringValue());
    }
    return Collections.emptyMap();
  }

  private void compute(NewCoverageMetrics newCoverageMetrics, Counters counters) {
    computeMeasure(counters, newCoverageMetrics.newLinesToCover, NewLinesToCoverComputer.INSTANCE);
    computeMeasure(counters, newCoverageMetrics.newUncoveredLines, NewUncoveredLinesComputer.INSTANCE);
    computeMeasure(counters, newCoverageMetrics.newConditionsToCover, NewConditionsToCoverComputer.INSTANCE);
    computeMeasure(counters, newCoverageMetrics.newUncoveredConditions, NewUncoveredConditionsComputer.INSTANCE);
  }

  private void computeMeasure(final Counters counters, Metric metric, NewCoverageMeasureComputer measureComputer) {
    List<Counter> nonEmptyCounters = from(counters.getCounters()).filter(CounterHasNewCode.INSTANCE).toList();
    if (nonEmptyCounters.isEmpty()) {
      measureRepository.add(counters.getComponent(), metric, newMeasureBuilder().createNoValue());
      return;
    }

    MeasureVariations.Builder variationsBuilder = MeasureVariations.newMeasureVarationsBuilder();
    for (Counter counter : nonEmptyCounters) {
      variationsBuilder.setVariation(counter.getPeriodIndex(), measureComputer.compute(counter));
    }
    measureRepository.add(counters.getComponent(), metric, newMeasureBuilder().setVariations(variationsBuilder.build()).createNoValue());
  }

  @Override
  public String getDescription() {
    return "Computation of New Coverage measures";
  }

  /**
   * Internal class storing the Metric objects for each property of a {@link NewCoverageMetricKeys} instance
   */
  private static final class NewCoverageMetrics {
    private final Metric coverageLineHitsData;
    private final Metric conditionsByLine;
    private final Metric coveredConditionsByLine;
    private final Metric newLinesToCover;
    private final Metric newUncoveredLines;
    private final Metric newConditionsToCover;
    private final Metric newUncoveredConditions;

    public NewCoverageMetrics(MetricRepository metricRepository, NewCoverageMetricKeys keys) {
      this.coverageLineHitsData = metricRepository.getByKey(keys.coverageLineHitsData());
      this.conditionsByLine = metricRepository.getByKey(keys.conditionsByLine());
      this.coveredConditionsByLine = metricRepository.getByKey(keys.coveredConditionsByLine());
      this.newLinesToCover = metricRepository.getByKey(keys.newLinesToCover());
      this.newUncoveredLines = metricRepository.getByKey(keys.newUncoveredLines());
      this.newConditionsToCover = metricRepository.getByKey(keys.newConditionsToCover());
      this.newUncoveredConditions = metricRepository.getByKey(keys.newUncoveredConditions());
    }

  }

  /**
   * Holds the {@link Counter}s (one for each Period in a specific {@link PeriodsHolder}) for a specific Component.
   */
  private static final class Counters {
    private final Component component;
    private final List<Counter> counters;

    public Counters(Component component, PeriodsHolder periodsHolder) {
      this.component = component;
      this.counters = from(periodsHolder.getPeriods()).transform(PeriodToCounter.INSTANCE).toList();
    }

    public void analyze(@Nullable Date lineDate, int hits, int conditions, int coveredConditions) {
      if (lineDate == null) {
        return;
      }
      for (Counter counter : getCounters()) {
        counter.analyze(lineDate, hits, conditions, coveredConditions);
      }
    }

    public Component getComponent() {
      return component;
    }

    private Iterable<Counter> getCounters() {
      return this.counters;
    }

  }

  public static final class Counter {
    private final int periodIndex;
    private final Date date;
    private Integer newLines;
    private Integer newCoveredLines;
    private Integer newConditions;
    private Integer newCoveredConditions;

    private Counter(int periodIndex, Date date) {
      this.periodIndex = periodIndex;
      this.date = date;
    }

    public int getPeriodIndex() {
      return periodIndex;
    }

    void analyze(Date lineDate, int hits, int conditions, int coveredConditions) {
      if (lineDate.after(date)) {
        addLine(hits > 0);
        addConditions(conditions, coveredConditions);
      }
    }

    void addLine(boolean covered) {
      if (newLines == null) {
        newLines = 0;
      }
      newLines += 1;
      if (covered) {
        if (newCoveredLines == null) {
          newCoveredLines = 0;
        }
        newCoveredLines += 1;
      }
    }

    void addConditions(int count, int countCovered) {
      if (newConditions == null) {
        newConditions = 0;
      }
      newConditions += count;
      if (count > 0) {
        if (newCoveredConditions == null) {
          newCoveredConditions = 0;
        }
        newCoveredConditions += countCovered;
      }
    }

    boolean hasNewCode() {
      return newLines != null;
    }

    public int getNewLines() {
      return newLines != null ? newLines : 0;
    }

    public int getNewCoveredLines() {
      return newCoveredLines != null ? newCoveredLines : 0;
    }

    public int getNewConditions() {
      return newConditions != null ? newConditions : 0;
    }

    public int getNewCoveredConditions() {
      return newCoveredConditions != null ? newCoveredConditions : 0;
    }
  }

  private enum PeriodToCounter implements Function<Period, Counter> {
    INSTANCE;

    @Override
    @Nonnull
    public Counter apply(@Nonnull Period input) {
      return new Counter(input.getIndex(), new Date(input.getSnapshotDate()));
    }
  }

  private enum CounterHasNewCode implements Predicate<Counter> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Counter input) {
      return input.hasNewCode();
    }
  }

  /**
   * Represents a way of computing a measure value from a given Counter.
   */
  private interface NewCoverageMeasureComputer {
    int compute(Counter counter);
  }

  private enum NewLinesToCoverComputer implements NewCoverageMeasureComputer {
    INSTANCE;

    @Override
    public int compute(Counter counter) {
      return counter.getNewLines();
    }
  }

  private enum NewUncoveredLinesComputer implements NewCoverageMeasureComputer {
    INSTANCE;

    @Override
    public int compute(Counter counter) {
      return counter.getNewLines() - counter.getNewCoveredLines();
    }
  }

  private enum NewConditionsToCoverComputer implements NewCoverageMeasureComputer {
    INSTANCE;

    @Override
    public int compute(Counter counter) {
      return counter.getNewConditions();
    }
  }

  private enum NewUncoveredConditionsComputer implements NewCoverageMeasureComputer {
    INSTANCE;

    @Override
    public int compute(Counter counter) {
      return counter.getNewConditions() - counter.getNewCoveredConditions();
    }
  }
}
