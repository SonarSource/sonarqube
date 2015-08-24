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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.formula.counter.IntSumCounter;
import org.sonar.server.computation.formula.CounterInitializationContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;

/**
 * Computes duplication measures on files and then aggregates them on higher components.
 * 
 * This step must be executed after {@link CommentMeasuresStep} as it depends on {@link CoreMetrics#COMMENT_LINES}
 */
public class DuplicationMeasuresStep implements ComputationStep {

  private final ImmutableList<Formula> formulas;

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public DuplicationMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.formulas = ImmutableList.<Formula>of(
      new SumDuplicationFormula(DUPLICATED_BLOCKS_KEY),
      new SumDuplicationFormula(DUPLICATED_FILES_KEY),
      new DuplicationFormula());
  }

  @Override
  public void execute() {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(formulas))
        .visit(treeRootHolder.getRoot());
  }

  private class DuplicationFormula implements Formula<SumDuplicationCounter> {

    private final Metric nclocMetric;
    private final Metric linesMetric;
    private final Metric commentLinesMetric;

    public DuplicationFormula() {
      this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
      this.linesMetric = metricRepository.getByKey(LINES_KEY);
      this.commentLinesMetric = metricRepository.getByKey(COMMENT_LINES_KEY);
    }

    @Override
    public SumDuplicationCounter createNewCounter() {
      return new SumDuplicationCounter(DUPLICATED_LINES_KEY);
    }

    @Override
    public Optional<Measure> createMeasure(SumDuplicationCounter counter, CreateMeasureContext context) {
      return createDuplicatedLinesMeasure(counter, context)
        .or(createDuplicatedLinesDensityMeasure(counter, context));
    }

    private Optional<Measure> createDuplicatedLinesMeasure(SumDuplicationCounter counter, CreateMeasureContext context) {
      int duplicatedLines = counter.value;
      if (context.getMetric().getKey().equals(DUPLICATED_LINES_KEY)
        && CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType())) {
        return Optional.of(Measure.newMeasureBuilder().create(duplicatedLines));
      }
      return Optional.absent();
    }

    private Optional<Measure> createDuplicatedLinesDensityMeasure(SumDuplicationCounter counter, CreateMeasureContext context) {
      int duplicatedLines = counter.value;
      if (context.getMetric().getKey().equals(DUPLICATED_LINES_DENSITY_KEY)) {
        Optional<Integer> nbLines = getNbLinesFromLocOrNcloc(context);
        if (nbLines.isPresent() && nbLines.get() > 0) {
          double density = Math.min(100d, 100d * duplicatedLines / nbLines.get());
          return Optional.of(Measure.newMeasureBuilder().create(density));
        }
      }
      return Optional.absent();
    }

    private Optional<Integer> getNbLinesFromLocOrNcloc(CreateMeasureContext context) {
      Optional<Measure> lines = measureRepository.getRawMeasure(context.getComponent(), linesMetric);
      if (lines.isPresent()) {
        return Optional.of(lines.get().getIntValue());
      }
      Optional<Measure> nclocs = measureRepository.getRawMeasure(context.getComponent(), nclocMetric);
      if (nclocs.isPresent()) {
        Optional<Measure> commentLines = measureRepository.getRawMeasure(context.getComponent(), commentLinesMetric);
        int nbLines = nclocs.get().getIntValue();
        return Optional.of(commentLines.isPresent() ? (nbLines + commentLines.get().getIntValue()) : nbLines);
      }
      return Optional.absent();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {DUPLICATED_LINES_KEY, DUPLICATED_LINES_DENSITY_KEY};
    }
  }

  private class SumDuplicationFormula implements Formula<SumDuplicationCounter> {

    private final String metricKey;

    public SumDuplicationFormula(String metricKey) {
      this.metricKey = requireNonNull(metricKey, "Metric key cannot be null");
    }

    @Override
    public SumDuplicationCounter createNewCounter() {
      return new SumDuplicationCounter(metricKey);
    }

    @Override
    public Optional<Measure> createMeasure(SumDuplicationCounter counter, CreateMeasureContext context) {
      int value = counter.value;
      if (CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType())) {
        return Optional.of(Measure.newMeasureBuilder().create(value));
      }
      return Optional.absent();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {metricKey};
    }
  }

  /**
   * This counter is almost the same as {@link IntSumCounter}, expect that it will aggregate a value of 0 when there's no measure on file level
   */
  private class SumDuplicationCounter implements Counter<SumDuplicationCounter> {

    private final String metricKey;

    private int value = 0;

    public SumDuplicationCounter(String metricKey) {
      this.metricKey = metricKey;
    }

    @Override
    public void aggregate(SumDuplicationCounter counter) {
      value += counter.value;
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      Optional<Measure> measureOptional = context.getMeasure(metricKey);
      if (measureOptional.isPresent()) {
        value += measureOptional.get().getIntValue();
      }
    }
  }

  @Override
  public String getDescription() {
    return "Aggregation of duplication measures";
  }
}
