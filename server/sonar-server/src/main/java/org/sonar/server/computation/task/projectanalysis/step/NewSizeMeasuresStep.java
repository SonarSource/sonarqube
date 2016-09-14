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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplication;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.server.computation.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.TextBlock;
import org.sonar.server.computation.task.projectanalysis.formula.Counter;
import org.sonar.server.computation.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.server.computation.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.server.computation.task.projectanalysis.formula.Formula;
import org.sonar.server.computation.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.formula.counter.DoubleVariationValue;
import org.sonar.server.computation.task.projectanalysis.formula.counter.IntVariationValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;

/**
 * Computes measures on new code related to the size
 */
public class NewSizeMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final NewDuplicationFormula duplicationFormula;

  public NewSizeMeasuresStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    ScmInfoRepository scmInfoRepository, DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.duplicationFormula = new NewDuplicationFormula(scmInfoRepository, duplicationRepository);
  }

  @Override
  public String getDescription() {
    return "Compute size measures on new code";
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .withVariationSupport(periodsHolder)
        .buildFor(ImmutableList.of(duplicationFormula)))
          .visit(treeRootHolder.getRoot());
  }

  private static class NewSizeCounter implements Counter<NewSizeCounter> {
    private final DuplicationRepository duplicationRepository;
    private final ScmInfoRepository scmInfoRepository;
    private final IntVariationValue.Array newLines = IntVariationValue.newArray();
    private final IntVariationValue.Array newDuplicatedLines = IntVariationValue.newArray();
    private final IntVariationValue.Array newDuplicatedBlocks = IntVariationValue.newArray();

    private NewSizeCounter(DuplicationRepository duplicationRepository,
      ScmInfoRepository scmInfoRepository) {
      this.duplicationRepository = duplicationRepository;
      this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public void aggregate(NewSizeCounter counter) {
      this.newDuplicatedLines.incrementAll(counter.newDuplicatedLines);
      this.newDuplicatedBlocks.incrementAll(counter.newDuplicatedBlocks);
      this.newLines.incrementAll(counter.newLines);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component leaf = context.getLeaf();
      Optional<ScmInfo> scmInfo = scmInfoRepository.getScmInfo(leaf);
      if (!scmInfo.isPresent()) {
        return;
      }

      context.getPeriods().forEach(period -> newLines.increment(period, 0));
      if (leaf.getType() != Component.Type.FILE) {
        context.getPeriods().forEach(period -> {
          newDuplicatedLines.increment(period, 0);
          newDuplicatedBlocks.increment(period, 0);
        });
        return;
      }

      initNewLines(scmInfo.get(), context.getPeriods());
      initNewDuplicated(leaf, scmInfo.get(), context.getPeriods());
    }

    private void initNewLines(ScmInfo scmInfo, List<Period> periods) {
      scmInfo.getAllChangesets().forEach(changeset -> periods.stream()
        .filter(period -> isLineInPeriod(changeset, period))
        .forEach(period -> newLines.increment(period, 1)));
    }

    private void initNewDuplicated(Component component, ScmInfo scmInfo, List<Period> periods) {
      DuplicationCounters duplicationCounters = new DuplicationCounters(scmInfo, periods);
      Iterable<Duplication> duplications = duplicationRepository.getDuplications(component);
      for (Duplication duplication : duplications) {
        duplicationCounters.addBlock(duplication.getOriginal());
        duplication.getDuplicates().stream()
          .filter(InnerDuplicate.class::isInstance)
          .map(duplicate -> (InnerDuplicate) duplicate)
          .forEach(duplicate -> duplicationCounters.addBlock(duplicate.getTextBlock()));
      }

      Map<Period, Integer> newLinesDuplicatedByPeriod = duplicationCounters.getNewLinesDuplicated();
      periods.forEach(period -> {
        newDuplicatedLines.increment(period, newLinesDuplicatedByPeriod.getOrDefault(period, 0));
        newDuplicatedBlocks.increment(period, duplicationCounters.getNewBlocksDuplicated().getOrDefault(period, 0));
      });
    }

    private static boolean isLineInPeriod(Changeset changeset, Period period) {
      return changeset.getDate() > period.getSnapshotDate();
    }
  }

  private static class DuplicationCounters {
    private final ScmInfo scmInfo;
    private final List<Period> periods;
    private final SetMultimap<Period, Integer> lineCounts;
    private final Multiset<Period> blockCounts;

    private DuplicationCounters(ScmInfo scmInfo, List<Period> periods) {
      this.scmInfo = scmInfo;
      this.periods = periods;
      this.lineCounts = LinkedHashMultimap.create(periods.size(), Iterables.size(scmInfo.getAllChangesets()));
      this.blockCounts = HashMultiset.create();
    }

    void addBlock(TextBlock textBlock) {
      Set<Period> periodWithNewCode = new HashSet<>();
      IntStream.rangeClosed(textBlock.getStart(), textBlock.getEnd())
        .forEach(line -> periods.stream()
          .filter(period -> isLineInPeriod(line, period))
          .forEach(period -> {
            lineCounts.put(period, line);
            periodWithNewCode.add(period);
          }));
      blockCounts.addAll(periodWithNewCode);
    }

    Map<Period, Integer> getNewLinesDuplicated() {
      return ImmutableMap.copyOf(lineCounts.keySet().stream().collect(toMap(Function.identity(), period -> lineCounts.get(period).size())));
    }

    Map<Period, Integer> getNewBlocksDuplicated() {
      return blockCounts.entrySet().stream().collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
    }

    private boolean isLineInPeriod(int lineNumber, Period period) {
      return scmInfo.getChangesetForLine(lineNumber).getDate() > period.getSnapshotDate();
    }
  }

  private static final class NewDuplicationFormula implements Formula<NewSizeCounter> {
    private final DuplicationRepository duplicationRepository;
    private final ScmInfoRepository scmInfoRepository;

    private NewDuplicationFormula(ScmInfoRepository scmInfoRepository,
      DuplicationRepository duplicationRepository) {
      this.duplicationRepository = duplicationRepository;
      this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public NewSizeCounter createNewCounter() {
      return new NewSizeCounter(duplicationRepository, scmInfoRepository);
    }

    @Override
    public Optional<Measure> createMeasure(NewSizeCounter counter, CreateMeasureContext context) {
      String metricKey = context.getMetric().getKey();
      switch (metricKey) {
        case NEW_LINES_KEY:
          return createMeasure(counter.newLines.toMeasureVariations());
        case NEW_DUPLICATED_LINES_KEY:
          return createMeasure(counter.newDuplicatedLines.toMeasureVariations());
        case NEW_DUPLICATED_LINES_DENSITY_KEY:
          return createNewDuplicatedLinesDensityMeasure(counter, context);
        case NEW_BLOCKS_DUPLICATED_KEY:
          return createMeasure(counter.newDuplicatedBlocks.toMeasureVariations());
        default:
          throw new IllegalArgumentException("Unsupported metric " + context.getMetric());
      }
    }

    private static Optional<Measure> createMeasure(Optional<MeasureVariations> measure) {
      return measure.isPresent()
        ? Optional.of(Measure.newMeasureBuilder().setVariations(measure.get()).createNoValue())
        : Optional.absent();
    }

    private static Optional<Measure> createNewDuplicatedLinesDensityMeasure(NewSizeCounter counter, CreateMeasureContext context) {
      Optional<MeasureVariations> newLines = counter.newLines.toMeasureVariations();
      Optional<MeasureVariations> newDuplicatedLines = counter.newDuplicatedLines.toMeasureVariations();
      DoubleVariationValue.Array newDuplicatedLinesDensity = DoubleVariationValue.newArray();
      if (newLines.isPresent() && newDuplicatedLines.isPresent()) {
        MeasureVariations newLinesVariations = newLines.get();
        MeasureVariations newDuplicatedLinesVariations = newDuplicatedLines.get();
        for (Period period : context.getPeriods()) {
          double currentNewLines = newLinesVariations.getVariation(period.getIndex());
          if (currentNewLines > 0d) {
            double density = Math.min(100d, 100d * newDuplicatedLinesVariations.getVariation(period.getIndex()) / currentNewLines);
            newDuplicatedLinesDensity.increment(period, density);
          }
        }
      }
      return createMeasure(newDuplicatedLinesDensity.toMeasureVariations());
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_KEY, NEW_DUPLICATED_LINES_KEY, NEW_DUPLICATED_LINES_DENSITY_KEY, NEW_BLOCKS_DUPLICATED_KEY};
    }
  }
}
