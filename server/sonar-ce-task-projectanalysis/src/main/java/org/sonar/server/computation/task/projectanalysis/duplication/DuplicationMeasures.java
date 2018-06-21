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
package org.sonar.server.computation.task.projectanalysis.duplication;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.formula.Counter;
import org.sonar.server.computation.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.server.computation.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.server.computation.task.projectanalysis.formula.Formula;
import org.sonar.server.computation.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

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
    this.formulas = ImmutableList.of(new DuplicationFormula(metricRepository, measureRepository));
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
        .visit(treeRootHolder.getRoot());
  }

  protected DuplicationCounter createCounter() {
    return new DuplicationCounter(duplicationRepository);
  }

  protected static class DuplicationCounter implements Counter<DuplicationCounter> {
    @CheckForNull
    private final DuplicationRepository duplicationRepository;
    protected int fileCount = 0;
    protected int blockCount = 0;
    protected int lineCount = 0;

    protected DuplicationCounter() {
      this(null);
    }

    private DuplicationCounter(@Nullable DuplicationRepository duplicationRepository) {
      this.duplicationRepository = duplicationRepository;
    }

    @Override
    public void aggregate(DuplicationCounter counter) {
      this.fileCount += counter.fileCount;
      this.blockCount += counter.blockCount;
      this.lineCount += counter.lineCount;
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Component leaf = context.getLeaf();
      if (leaf.getType() == Component.Type.FILE) {
        initializeForFile(leaf);
      } else if (leaf.getType() == Component.Type.PROJECT_VIEW) {
        initializeForProjectView(context);
      }
    }

    protected void initializeForFile(Component file) {
      Iterable<Duplication> duplications = requireNonNull(this.duplicationRepository, "DuplicationRepository missing")
        .getDuplications(file);
      if (isEmpty(duplications)) {
        return;
      }

      // use a set to count lines only once
      Set<Integer> duplicatedLineNumbers = new HashSet<>();
      long blocks = 0;
      for (Duplication duplication : duplications) {
        blocks++;
        addLines(duplication.getOriginal(), duplicatedLineNumbers);
        for (InnerDuplicate innerDuplicate : from(duplication.getDuplicates()).filter(InnerDuplicate.class)) {
          blocks++;
          addLines(innerDuplicate.getTextBlock(), duplicatedLineNumbers);
        }
      }

      this.fileCount += 1;
      this.blockCount += blocks;
      this.lineCount += duplicatedLineNumbers.size();
    }

    private static void addLines(TextBlock textBlock, Set<Integer> duplicatedLineNumbers) {
      for (int i = textBlock.getStart(); i <= textBlock.getEnd(); i++) {
        duplicatedLineNumbers.add(i);
      }
    }

    private void initializeForProjectView(CounterInitializationContext context) {
      fileCount += getMeasure(context, DUPLICATED_FILES_KEY);
      blockCount += getMeasure(context, DUPLICATED_BLOCKS_KEY);
      lineCount += getMeasure(context, DUPLICATED_LINES_KEY);
    }

    private static int getMeasure(CounterInitializationContext context, String metricKey) {
      Optional<Measure> files = context.getMeasure(metricKey);
      if (files.isPresent()) {
        return files.get().getIntValue();
      }
      return 0;
    }
  }

  private final class DuplicationFormula implements Formula<DuplicationCounter> {
    private final MeasureRepository measureRepository;
    private final Metric nclocMetric;
    private final Metric linesMetric;
    private final Metric commentLinesMetric;

    private DuplicationFormula(MetricRepository metricRepository, MeasureRepository measureRepository) {
      this.measureRepository = measureRepository;
      this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
      this.linesMetric = metricRepository.getByKey(LINES_KEY);
      this.commentLinesMetric = metricRepository.getByKey(COMMENT_LINES_KEY);
    }

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
          return Optional.of(Measure.newMeasureBuilder().create(counter.lineCount));
        case DUPLICATED_LINES_DENSITY_KEY:
          return createDuplicatedLinesDensityMeasure(counter, context);
        case DUPLICATED_BLOCKS_KEY:
          return Optional.of(Measure.newMeasureBuilder().create(counter.blockCount));
        default:
          throw new IllegalArgumentException("Unsupported metric " + context.getMetric());
      }
    }

    private Optional<Measure> createDuplicatedLinesDensityMeasure(DuplicationCounter counter, CreateMeasureContext context) {
      int duplicatedLines = counter.lineCount;
      java.util.Optional<Integer> nbLines = getNbLinesFromLocOrNcloc(context);
      if (nbLines.isPresent() && nbLines.get() > 0) {
        double density = Math.min(100d, 100d * duplicatedLines / nbLines.get());
        return Optional.of(Measure.newMeasureBuilder().create(density, context.getMetric().getDecimalScale()));
      }
      return Optional.absent();
    }

    private java.util.Optional<Integer> getNbLinesFromLocOrNcloc(CreateMeasureContext context) {
      Optional<Measure> lines = measureRepository.getRawMeasure(context.getComponent(), linesMetric);
      if (lines.isPresent()) {
        return java.util.Optional.of(lines.get().getIntValue());
      }
      Optional<Measure> nclocs = measureRepository.getRawMeasure(context.getComponent(), nclocMetric);
      if (nclocs.isPresent()) {
        Optional<Measure> commentLines = measureRepository.getRawMeasure(context.getComponent(), commentLinesMetric);
        int nbLines = nclocs.get().getIntValue();
        return java.util.Optional.of(commentLines.isPresent() ? (nbLines + commentLines.get().getIntValue()) : nbLines);
      }
      return java.util.Optional.empty();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {DUPLICATED_FILES_KEY, DUPLICATED_LINES_KEY, DUPLICATED_LINES_DENSITY_KEY, DUPLICATED_BLOCKS_KEY};
    }
  }
}
