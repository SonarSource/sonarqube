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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
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
import org.sonar.server.computation.task.projectanalysis.formula.VariationSumFormula;
import org.sonar.server.computation.task.projectanalysis.formula.counter.IntVariationValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_DUPLICATED_KEY;
import static org.sonar.server.computation.task.projectanalysis.period.PeriodPredicates.viewsRestrictedPeriods;

/**
 * Computes new duplication measures on files and then aggregates them on higher components.
 *
 */
public class NewDuplicationMeasuresStep implements ComputationStep {

  private final ImmutableList<Formula> formulas;

  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public NewDuplicationMeasuresStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    ScmInfoRepository scmInfoRepository, @Nullable DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.formulas = ImmutableList.of(NewDuplicationFormula.from(scmInfoRepository, duplicationRepository));
  }

  /**
   * Constructor used by Pico in Governance where no DuplicationRepository is available.
   */
  public NewDuplicationMeasuresStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this(treeRootHolder, periodsHolder, metricRepository, measureRepository, null, null);
  }

  @Override
  public String getDescription() {
    return "Compute new duplication measures";
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .withVariationSupport(periodsHolder)
        .buildFor(formulas))
          .visit(treeRootHolder.getRoot());
  }

  private static class NewDuplicationCounter implements Counter<NewDuplicationCounter> {
    @CheckForNull
    private final DuplicationRepository duplicationRepository;
    private final ScmInfoRepository scmInfoRepository;
    private final IntVariationValue.Array newLines = IntVariationValue.newArray();

    private NewDuplicationCounter(@Nullable DuplicationRepository duplicationRepository, ScmInfoRepository scmInfoRepository) {
      this.duplicationRepository = duplicationRepository;
      this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public void aggregate(NewDuplicationCounter counter) {
      this.newLines.incrementAll(counter.newLines);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component leaf = context.getLeaf();
      Iterable<Duplication> duplications = requireNonNull(this.duplicationRepository, "DuplicationRepository missing").getDuplications(leaf);
      Optional<ScmInfo> scmInfo = scmInfoRepository.getScmInfo(leaf);

      if (!scmInfo.isPresent()) {
        return;
      }

      NewLinesDuplicatedAccumulator newLinesDuplicatedAccumulator = new NewLinesDuplicatedAccumulator(scmInfo.get(), context.getPeriods());

      for (Duplication duplication : duplications) {
        newLinesDuplicatedAccumulator.addBlock(duplication.getOriginal());
        duplication.getDuplicates().stream()
          .filter(InnerDuplicate.class::isInstance)
          .map(duplicate -> (InnerDuplicate) duplicate)
          .forEach(duplicate -> newLinesDuplicatedAccumulator.addBlock(duplicate.getTextBlock()));
      }

      Map<Period, Integer> newLinesDuplicatedByPeriod = newLinesDuplicatedAccumulator.getNewLinesDuplicated();
      context.getPeriods().forEach(period -> newLines.increment(period, newLinesDuplicatedByPeriod.getOrDefault(period, 0)));
    }
  }

  private static class NewLinesDuplicatedAccumulator {
    private final ScmInfo scmInfo;
    private final List<Period> periods;
    private final SetMultimap<Period, Integer> counts;

    private NewLinesDuplicatedAccumulator(ScmInfo scmInfo, List<Period> periods) {
      this.scmInfo = scmInfo;
      this.periods = periods;
      this.counts = LinkedHashMultimap.create(periods.size(), Iterables.size(scmInfo.getAllChangesets()));
    }

    void addBlock(TextBlock textBlock) {
      IntStream.rangeClosed(textBlock.getStart(), textBlock.getEnd())
        .forEach(line -> periods.stream()
          .filter(period -> isLineInPeriod(line, period))
          .forEach(period -> counts.put(period, line)));
    }

    Map<Period, Integer> getNewLinesDuplicated() {
      return ImmutableMap.copyOf(counts.keySet().stream().collect(toMap(Function.identity(), period -> counts.get(period).size())));
    }

    private boolean isLineInPeriod(int lineNumber, Period period) {
      return scmInfo.getChangesetForLine(lineNumber).getDate() > period.getSnapshotDate();
    }
  }

  private static final class NewDuplicationFormula implements Formula<NewDuplicationCounter> {
    private static final Formula VIEW_FORMULA = new VariationSumFormula(NEW_LINES_DUPLICATED_KEY, viewsRestrictedPeriods(), 0.0d);

    private final DuplicationRepository duplicationRepository;
    private final ScmInfoRepository scmInfoRepository;

    private NewDuplicationFormula(ScmInfoRepository scmInfoRepository, @Nullable DuplicationRepository duplicationRepository) {
      this.duplicationRepository = duplicationRepository;
      this.scmInfoRepository = scmInfoRepository;
    }

    public static Formula<?> from(@Nullable ScmInfoRepository scmInfoRepository, @Nullable DuplicationRepository duplicationRepository) {
      return scmInfoRepository == null
        ? VIEW_FORMULA
        : new NewDuplicationFormula(scmInfoRepository, duplicationRepository);
    }

    @Override
    public NewDuplicationCounter createNewCounter() {
      return new NewDuplicationCounter(duplicationRepository, scmInfoRepository);
    }

    @Override
    public Optional<Measure> createMeasure(NewDuplicationCounter counter, CreateMeasureContext context) {
      String metricKey = context.getMetric().getKey();
      if (NEW_LINES_DUPLICATED_KEY.equals(metricKey)) {
        Optional<MeasureVariations> variations = counter.newLines.toMeasureVariations();
        return variations.isPresent()
          ? Optional.of(Measure.newMeasureBuilder().setVariations(variations.get()).createNoValue())
          : Optional.absent();
      }

      throw new IllegalArgumentException("Unsupported metric " + context.getMetric());
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_DUPLICATED_KEY};
    }
  }
}
