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

package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.formula.Counter;
import org.sonar.server.computation.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.server.computation.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.server.computation.task.projectanalysis.formula.Formula;
import org.sonar.server.computation.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.formula.counter.IntVariationValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_NCLOC_KEY;
import static org.sonar.api.utils.KeyValueFormat.newIntegerConverter;

/**
 * Computes new lines of code measure on files and then aggregates them on higher components.
 */
public class NewLinesMeasureStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final ScmInfoRepository scmInfoRepository;

  public NewLinesMeasureStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    ScmInfoRepository scmInfoRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
  }

  @Override
  public String getDescription() {
    return "Compute new lines of code";
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .withVariationSupport(periodsHolder)
        .buildFor(ImmutableList.of(new NewLinesFormula(measureRepository, scmInfoRepository, metricRepository.getByKey(NCLOC_DATA_KEY)))))
          .visit(treeRootHolder.getRoot());
  }

  private static class NewLinesCounter implements Counter<NewLinesCounter> {
    private final MeasureRepository measureRepository;
    private final ScmInfoRepository scmInfoRepository;
    private final Metric nclocDataMetric;

    private final IntVariationValue.Array newLines = IntVariationValue.newArray();

    private NewLinesCounter(MeasureRepository measureRepository, ScmInfoRepository scmInfoRepository, Metric nclocDataMetric) {
      this.measureRepository = measureRepository;
      this.scmInfoRepository = scmInfoRepository;
      this.nclocDataMetric = nclocDataMetric;
    }

    @Override
    public void aggregate(NewLinesCounter counter) {
      this.newLines.incrementAll(counter.newLines);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      context.getPeriods().forEach(period -> newLines.increment(period, 0));

      Component leak = context.getLeaf();
      if (leak.getType() != Component.Type.FILE) {
        return;
      }

      Optional<ScmInfo> optionalScmInfo = scmInfoRepository.getScmInfo(leak);
      Optional<Measure> nclocData = measureRepository.getRawMeasure(leak, nclocDataMetric);

      if (!nclocData.isPresent() || !optionalScmInfo.isPresent()) {
        return;
      }

      ScmInfo scmInfo = optionalScmInfo.get();

      nclocLineNumbers(nclocData.get()).stream()
        .map(scmInfo::getChangesetForLine)
        .forEach(changeset -> context.getPeriods().stream()
          .filter(period -> isLineInPeriod(changeset, period))
          .forEach(period -> newLines.increment(period, 1)));
    }

    /**
     * NCLOC_DATA contains Key-value pairs, where key - is a line number, and value - is an indicator of whether line
     * contains code (1) or not (0).
     *
     * This method parses the value of the NCLOC_DATA measure and return the line numbers which contain code.
     */
    private static List<Integer> nclocLineNumbers(Measure nclocDataMeasure) {
      Map<Integer, Integer> parsedNclocData = KeyValueFormat.parse(nclocDataMeasure.getData(), newIntegerConverter(), newIntegerConverter());
      return parsedNclocData.entrySet()
        .stream()
        .filter(entry -> entry.getValue() == 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    }

    private static boolean isLineInPeriod(Changeset changeset, Period period) {
      return changeset.getDate() > period.getSnapshotDate();
    }
  }

  private static final class NewLinesFormula implements Formula<NewLinesCounter> {
    private final MeasureRepository measureRepository;
    private final ScmInfoRepository scmInfoRepository;
    private final Metric nclocDataMetric;

    private NewLinesFormula(MeasureRepository measureRepository, ScmInfoRepository scmInfoRepository, Metric nclocDataMetric) {
      this.measureRepository = measureRepository;
      this.scmInfoRepository = scmInfoRepository;
      this.nclocDataMetric = nclocDataMetric;
    }

    @Override
    public NewLinesCounter createNewCounter() {
      return new NewLinesCounter(measureRepository, scmInfoRepository, nclocDataMetric);
    }

    @Override
    public Optional<Measure> createMeasure(NewLinesCounter counter, CreateMeasureContext context) {
      String metricKey = context.getMetric().getKey();
      if (NEW_NCLOC_KEY.equals(metricKey)) {
        Optional<MeasureVariations> newLines = counter.newLines.toMeasureVariations();
        return newLines.isPresent()
          ? Optional.of(Measure.newMeasureBuilder().setVariations(newLines.get()).createNoValue())
          : Optional.absent();
      }

      throw new IllegalArgumentException("Unsupported metric " + context.getMetric());
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_NCLOC_KEY};
    }
  }
}
