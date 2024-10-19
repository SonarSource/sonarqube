/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.duplication.Duplication;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.ce.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.formula.counter.IntValue;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.ce.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;

/**
 * Computes measures on new code related to the size
 */
public class NewSizeMeasuresStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final NewDuplicationFormula duplicationFormula;

  public NewSizeMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    NewLinesRepository newLinesRepository, DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.duplicationFormula = new NewDuplicationFormula(newLinesRepository, duplicationRepository);
  }

  @Override
  public String getDescription() {
    return "Compute size measures on new code";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
        .buildFor(List.of(duplicationFormula)))
      .visit(treeRootHolder.getRoot());
  }

  private static class NewSizeCounter implements Counter<NewSizeCounter> {
    private final DuplicationRepository duplicationRepository;
    private final NewLinesRepository newLinesRepository;

    private final IntValue newLines = new IntValue();
    private final IntValue newDuplicatedLines = new IntValue();
    private final IntValue newDuplicatedBlocks = new IntValue();

    private NewSizeCounter(DuplicationRepository duplicationRepository, NewLinesRepository newLinesRepository) {
      this.duplicationRepository = duplicationRepository;
      this.newLinesRepository = newLinesRepository;
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
      if (leaf.getType() != Component.Type.FILE) {
        return;
      }
      Optional<Set<Integer>> changedLines = newLinesRepository.getNewLines(leaf);
      if (changedLines.isEmpty()) {
        return;
      }

      newLines.increment(0);
      if (leaf.getType() != Component.Type.FILE) {
        newDuplicatedLines.increment(0);
        newDuplicatedBlocks.increment(0);
      } else {
        initNewLines(changedLines.get());
        initNewDuplicated(leaf, changedLines.get());
      }
    }

    private void initNewLines(Set<Integer> changedLines) {
      newLines.increment(changedLines.size());
    }

    private void initNewDuplicated(Component component, Set<Integer> changedLines) {
      DuplicationCounters duplicationCounters = new DuplicationCounters(changedLines);
      Iterable<Duplication> duplications = duplicationRepository.getDuplications(component);
      for (Duplication duplication : duplications) {
        duplicationCounters.addBlock(duplication.getOriginal());
        Arrays.stream(duplication.getDuplicates())
          .filter(InnerDuplicate.class::isInstance)
          .map(duplicate -> (InnerDuplicate) duplicate)
          .forEach(duplicate -> duplicationCounters.addBlock(duplicate.getTextBlock()));
      }

      newDuplicatedLines.increment(duplicationCounters.getNewLinesDuplicated());
      newDuplicatedBlocks.increment(duplicationCounters.getNewBlocksDuplicated());
    }
  }

  private static class DuplicationCounters {
    private final Set<Integer> changedLines;
    private final Set<Integer> lineCounts;
    private int blockCounts;

    private DuplicationCounters(Set<Integer> changedLines) {
      this.changedLines = changedLines;
      this.lineCounts = new HashSet<>(changedLines.size());
    }

    void addBlock(TextBlock textBlock) {
      Boolean[] newBlock = new Boolean[] {false};
      IntStream.rangeClosed(textBlock.getStart(), textBlock.getEnd())
        .filter(changedLines::contains)
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
  }

  private static final class NewDuplicationFormula implements Formula<NewSizeCounter> {
    private final DuplicationRepository duplicationRepository;
    private final NewLinesRepository newLinesRepository;

    private NewDuplicationFormula(NewLinesRepository newLinesRepository, DuplicationRepository duplicationRepository) {
      this.duplicationRepository = duplicationRepository;
      this.newLinesRepository = newLinesRepository;
    }

    @Override
    public NewSizeCounter createNewCounter() {
      return new NewSizeCounter(duplicationRepository, newLinesRepository);
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
        ? Optional.of(Measure.newMeasureBuilder().create(intValue.getValue()))
        : Optional.empty();
    }

    private static Optional<Measure> createNewDuplicatedLinesDensityMeasure(NewSizeCounter counter) {
      IntValue newLines = counter.newLines;
      IntValue newDuplicatedLines = counter.newDuplicatedLines;
      if (newLines.isSet() && newDuplicatedLines.isSet()) {
        int newLinesValue = newLines.getValue();
        int newDuplicatedLinesValue = newDuplicatedLines.getValue();
        if (newLinesValue > 0D) {
          double density = Math.min(100D, 100D * newDuplicatedLinesValue / newLinesValue);
          return Optional.of(Measure.newMeasureBuilder().create(density));
        }
      }
      return Optional.empty();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {NEW_LINES_KEY, NEW_DUPLICATED_LINES_KEY, NEW_DUPLICATED_LINES_DENSITY_KEY, NEW_BLOCKS_DUPLICATED_KEY};
    }
  }
}
