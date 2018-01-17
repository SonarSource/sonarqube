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
import java.util.HashSet;
import java.util.Set;
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
import org.sonar.server.computation.task.projectanalysis.formula.counter.IntValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;

/**
 * Computes measures on new code related to the size
 */
public class NewSizeMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final PeriodHolder periodHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final NewDuplicationFormula duplicationFormula;

  public NewSizeMeasuresStep(TreeRootHolder treeRootHolder, PeriodHolder periodHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    ScmInfoRepository scmInfoRepository, DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodHolder = periodHolder;
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
        .withVariationSupport(periodHolder)
        .buildFor(ImmutableList.of(duplicationFormula)))
          .visit(treeRootHolder.getRoot());
  }

  private static class NewSizeCounter implements Counter<NewSizeCounter> {
    private final DuplicationRepository duplicationRepository;
    private final ScmInfoRepository scmInfoRepository;
    private final IntValue newLines = new IntValue();
    private final IntValue newDuplicatedLines = new IntValue();
    private final IntValue newDuplicatedBlocks = new IntValue();

    private NewSizeCounter(DuplicationRepository duplicationRepository,
      ScmInfoRepository scmInfoRepository) {
      this.duplicationRepository = duplicationRepository;
      this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public void aggregate(NewSizeCounter counter) {
      this.newDuplicatedLines.increment(counter.newDuplicatedLines);
      this.newDuplicatedBlocks.increment(counter.newDuplicatedBlocks);
      this.newLines.increment(counter.newLines);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component leaf = context.getLeaf();
      Optional<ScmInfo> scmInfo = scmInfoRepository.getScmInfo(leaf);
      if (!scmInfo.isPresent() || !context.hasPeriod()) {
        return;
      }

      newLines.increment(0);
      if (leaf.getType() != Component.Type.FILE) {
        newDuplicatedLines.increment(0);
        newDuplicatedBlocks.increment(0);
        return;
      }

      initNewLines(scmInfo.get(), context.getPeriod());
      initNewDuplicated(leaf, scmInfo.get(), context.getPeriod());
    }

    private void initNewLines(ScmInfo scmInfo, Period period) {
      scmInfo.getAllChangesets().values().stream()
        .filter(changeset -> isLineInPeriod(changeset, period))
        .forEach(changeset -> newLines.increment(1));
    }

    private void initNewDuplicated(Component component, ScmInfo scmInfo, Period period) {
      DuplicationCounters duplicationCounters = new DuplicationCounters(scmInfo, period, scmInfo.getAllChangesets().size());
      Iterable<Duplication> duplications = duplicationRepository.getDuplications(component);
      for (Duplication duplication : duplications) {
        duplicationCounters.addBlock(duplication.getOriginal());
        duplication.getDuplicates().stream()
          .filter(InnerDuplicate.class::isInstance)
          .map(duplicate -> (InnerDuplicate) duplicate)
          .forEach(duplicate -> duplicationCounters.addBlock(duplicate.getTextBlock()));
      }

      newDuplicatedLines.increment(duplicationCounters.getNewLinesDuplicated());
      newDuplicatedBlocks.increment(duplicationCounters.getNewBlocksDuplicated());
    }

    private static boolean isLineInPeriod(Changeset changeset, Period period) {
      return changeset.getDate() > period.getSnapshotDate();
    }
  }

  private static class DuplicationCounters {
    private final ScmInfo scmInfo;
    private final Period period;
    private final Set<Integer> lineCounts;
    private int blockCounts;

    private DuplicationCounters(ScmInfo scmInfo, Period period, int changesetSize) {
      this.scmInfo = scmInfo;
      this.period = period;
      this.lineCounts = new HashSet<>(changesetSize);
    }

    void addBlock(TextBlock textBlock) {
      Boolean[] newBlock = new Boolean[] {false};
      IntStream.rangeClosed(textBlock.getStart(), textBlock.getEnd())
        .filter(scmInfo::hasChangesetForLine)
        .filter(line -> isLineInPeriod(line, period))
        .forEach(line -> {
          lineCounts.add(line);
          newBlock[0] = true;
        });
      if (newBlock[0]) {
        blockCounts++;
      }
    }

    int getNewLinesDuplicated() {
      return lineCounts.size();
    }

    int getNewBlocksDuplicated() {
      return blockCounts;
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
          return createMeasure(counter.newLines);
        case NEW_DUPLICATED_LINES_KEY:
          return createMeasure(counter.newDuplicatedLines);
        case NEW_DUPLICATED_LINES_DENSITY_KEY:
          return createNewDuplicatedLinesDensityMeasure(counter);
        case NEW_BLOCKS_DUPLICATED_KEY:
          return createMeasure(counter.newDuplicatedBlocks);
        default:
          throw new IllegalArgumentException("Unsupported metric " + context.getMetric());
      }
    }

    private static Optional<Measure> createMeasure(IntValue intValue) {
      return intValue.isSet()
        ? Optional.of(Measure.newMeasureBuilder().setVariation(intValue.getValue()).createNoValue())
        : Optional.absent();
    }

    private static Optional<Measure> createNewDuplicatedLinesDensityMeasure(NewSizeCounter counter) {
      IntValue newLines = counter.newLines;
      IntValue newDuplicatedLines = counter.newDuplicatedLines;
      if (newLines.isSet() && newDuplicatedLines.isSet()) {
        int newLinesVariations = newLines.getValue();
        int newDuplicatedLinesVariations = newDuplicatedLines.getValue();
        if (newLinesVariations > 0d) {
          double density = Math.min(100d, 100d * newDuplicatedLinesVariations / newLinesVariations);
          return Optional.of(Measure.newMeasureBuilder().setVariation(density).createNoValue());
        }
      }
      return Optional.absent();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_KEY, NEW_DUPLICATED_LINES_KEY, NEW_DUPLICATED_LINES_DENSITY_KEY, NEW_BLOCKS_DUPLICATED_KEY};
    }
  }
}
