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
package org.sonar.ce.task.projectanalysis.duplication;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;

public class DuplicationMeasures {
  protected final ImmutableList<Formula> formulas;
  protected final TreeRootHolder treeRootHolder;
  protected final MetricRepository metricRepository;
  protected final MeasureRepository measureRepository;
  private final DuplicationRepository duplicationRepository;

  public DuplicationMeasures(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    @Nullable DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    // will be null for views
    this.duplicationRepository = duplicationRepository;
    this.formulas = ImmutableList.of(new DuplicationFormula());
  }

  /**
   * Constructor used by Pico in Views where no DuplicationRepository is available.
   */
  public DuplicationMeasures(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this(treeRootHolder, metricRepository, measureRepository, null);
  }

  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(formulas))
      .visit(treeRootHolder.getReportTreeRoot());
  }

  protected DuplicationCounter createCounter() {
    return new DuplicationCounter(duplicationRepository);
  }

  protected static class DuplicationCounter implements Counter<DuplicationCounter> {
    @CheckForNull
    private final DuplicationRepository duplicationRepository;
    protected int fileCount = 0;
    protected int blockCount = 0;
    protected int dupLineCount = 0;
    protected int lineCount = 0;

    private DuplicationCounter(@Nullable DuplicationRepository duplicationRepository) {
      this.duplicationRepository = duplicationRepository;
    }

    @Override
    public void aggregate(DuplicationCounter counter) {
      this.fileCount += counter.fileCount;
      this.blockCount += counter.blockCount;
      this.dupLineCount += counter.dupLineCount;
      this.lineCount += counter.lineCount;
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component leaf = context.getLeaf();
      if (leaf.getType() == Component.Type.FILE && !leaf.getFileAttributes().isUnitTest()) {
        initializeForFile(leaf);
      } else if (leaf.getType() == Component.Type.PROJECT_VIEW) {
        initializeForProjectView(context);
      }
    }

    protected void initializeForFile(Component file) {
      // don't use measure since it won't be available for some files in the report tree in SLB
      this.lineCount = file.getFileAttributes().getLines();
      Iterable<Duplication> duplications = requireNonNull(this.duplicationRepository, "DuplicationRepository missing")
        .getDuplications(file);
      if (isEmpty(duplications)) {
        return;
      }

      // use a set to count lines only once
      Set<Integer> duplicatedLineNumbers = new HashSet<>();
      int blocks = 0;
      for (Duplication duplication : duplications) {
        blocks++;
        addLines(duplication.getOriginal(), duplicatedLineNumbers);
        InnerDuplicate[] innerDuplicates = Arrays.stream(duplication.getDuplicates())
          .filter(x -> x instanceof InnerDuplicate)
          .map(d -> (InnerDuplicate) d)
          .toArray(InnerDuplicate[]::new);

        for (InnerDuplicate innerDuplicate : innerDuplicates) {
          blocks++;
          addLines(innerDuplicate.getTextBlock(), duplicatedLineNumbers);
        }
      }

      this.fileCount += 1;
      this.blockCount += blocks;
      this.dupLineCount += duplicatedLineNumbers.size();

    }

    private static void addLines(TextBlock textBlock, Set<Integer> duplicatedLineNumbers) {
      for (int i = textBlock.getStart(); i <= textBlock.getEnd(); i++) {
        duplicatedLineNumbers.add(i);
      }
    }

    private void initializeForProjectView(CounterInitializationContext context) {
      fileCount += getMeasure(context, DUPLICATED_FILES_KEY);
      blockCount += getMeasure(context, DUPLICATED_BLOCKS_KEY);
      dupLineCount += getMeasure(context, DUPLICATED_LINES_KEY);
      lineCount += getMeasure(context, LINES_KEY);
    }

    private static int getMeasure(CounterInitializationContext context, String metricKey) {
      Optional<Measure> files = context.getMeasure(metricKey);
      return files.map(Measure::getIntValue).orElse(0);
    }
  }

  private final class DuplicationFormula implements Formula<DuplicationCounter> {
    @Override
    public DuplicationCounter createNewCounter() {
      return createCounter();
    }

    @Override
    public Optional<Measure> createMeasure(DuplicationCounter counter, CreateMeasureContext context) {
      switch (context.getMetric().getKey()) {
        case DUPLICATED_FILES_KEY:
          return Optional.of(Measure.newMeasureBuilder().create(counter.fileCount));
        case DUPLICATED_LINES_KEY:
          return Optional.of(Measure.newMeasureBuilder().create(counter.dupLineCount));
        case DUPLICATED_LINES_DENSITY_KEY:
          return createDuplicatedLinesDensityMeasure(counter, context);
        case DUPLICATED_BLOCKS_KEY:
          return Optional.of(Measure.newMeasureBuilder().create(counter.blockCount));
        default:
          throw new IllegalArgumentException("Unsupported metric " + context.getMetric());
      }
    }

    private Optional<Measure> createDuplicatedLinesDensityMeasure(DuplicationCounter counter, CreateMeasureContext context) {
      int duplicatedLines = counter.dupLineCount;
      int nbLines = counter.lineCount;
      if (nbLines > 0) {
        double density = Math.min(100.0, 100.0 * duplicatedLines / nbLines);
        return Optional.of(Measure.newMeasureBuilder().create(density, context.getMetric().getDecimalScale()));
      }
      return Optional.empty();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {DUPLICATED_FILES_KEY, DUPLICATED_LINES_KEY, DUPLICATED_LINES_DENSITY_KEY, DUPLICATED_BLOCKS_KEY};
    }
  }
}
