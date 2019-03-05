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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.formula.counter.IntSumCounter;
import org.sonar.ce.task.projectanalysis.formula.counter.SumCounter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY;

/**
 * Computes comments measures on files and then aggregates them on higher components.
 */
public class CommentMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final ImmutableList<Formula> formulas;

  public CommentMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.formulas = ImmutableList.of(
      new DocumentationFormula(),
      new CommentDensityFormula());
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(
      FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(formulas))
      .visit(treeRootHolder.getRoot());
  }

  private class CommentDensityFormula implements Formula<IntSumCounter> {

    private final Metric nclocMetric;

    public CommentDensityFormula() {
      this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
    }

    @Override
    public IntSumCounter createNewCounter() {
      return new IntSumCounter(COMMENT_LINES_KEY);
    }

    @Override
    public Optional<Measure> createMeasure(IntSumCounter counter, CreateMeasureContext context) {
      Optional<Measure> measure = createCommentLinesMeasure(counter, context);
      return measure.isPresent() ? measure : createCommentLinesDensityMeasure(counter, context);
    }

    private Optional<Measure> createCommentLinesMeasure(SumCounter counter, CreateMeasureContext context) {
      Optional<Integer> commentLines = counter.getValue();
      if (COMMENT_LINES_KEY.equals(context.getMetric().getKey())
        && commentLines.isPresent()
        && CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType())) {
        return Optional.of(Measure.newMeasureBuilder().create(commentLines.get()));
      }
      return Optional.empty();
    }

    private Optional<Measure> createCommentLinesDensityMeasure(SumCounter counter, CreateMeasureContext context) {
      if (COMMENT_LINES_DENSITY_KEY.equals(context.getMetric().getKey())) {
        Optional<Measure> nclocsOpt = measureRepository.getRawMeasure(context.getComponent(), nclocMetric);
        Optional<Integer> commentsOpt = counter.getValue();
        if (nclocsOpt.isPresent() && commentsOpt.isPresent()) {
          double nclocs = nclocsOpt.get().getIntValue();
          double comments = commentsOpt.get();
          double divisor = nclocs + comments;
          if (divisor > 0d) {
            double value = 100d * (comments / divisor);
            return Optional.of(Measure.newMeasureBuilder().create(value, context.getMetric().getDecimalScale()));
          }
        }
      }
      return Optional.empty();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {COMMENT_LINES_KEY, COMMENT_LINES_DENSITY_KEY};
    }
  }

  private static class DocumentationFormula implements Formula<DocumentationCounter> {

    @Override
    public DocumentationCounter createNewCounter() {
      return new DocumentationCounter();
    }

    @Override
    public Optional<Measure> createMeasure(DocumentationCounter counter, CreateMeasureContext context) {
      Optional<Measure> measure = getMeasure(context, counter.getPublicApiValue(), PUBLIC_API_KEY);
      if (measure.isPresent()) {
        return measure;
      }
      measure = getMeasure(context, counter.getPublicUndocumentedApiValue(), PUBLIC_UNDOCUMENTED_API_KEY);
      return measure.isPresent() ? measure : getDensityMeasure(counter, context);
    }

    private static Optional<Measure> getMeasure(CreateMeasureContext context, Optional<Integer> metricValue, String metricKey) {
      if (context.getMetric().getKey().equals(metricKey) && metricValue.isPresent()
        && CrawlerDepthLimit.LEAVES.isDeeperThan(context.getComponent().getType())) {
        return Optional.of(Measure.newMeasureBuilder().create(metricValue.get()));
      }
      return Optional.empty();
    }

    private static Optional<Measure> getDensityMeasure(DocumentationCounter counter, CreateMeasureContext context) {
      if (context.getMetric().getKey().equals(PUBLIC_DOCUMENTED_API_DENSITY_KEY) && counter.getPublicApiValue().isPresent()
        && counter.getPublicUndocumentedApiValue().isPresent()) {
        double publicApis = counter.getPublicApiValue().get();
        double publicUndocumentedApis = counter.getPublicUndocumentedApiValue().get();
        if (publicApis > 0d) {
          double documentedAPI = publicApis - publicUndocumentedApis;
          double value = 100d * (documentedAPI / publicApis);
          return Optional.of(Measure.newMeasureBuilder().create(value, context.getMetric().getDecimalScale()));
        }
      }
      return Optional.empty();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {PUBLIC_API_KEY, PUBLIC_UNDOCUMENTED_API_KEY, PUBLIC_DOCUMENTED_API_DENSITY_KEY};
    }
  }

  private static class DocumentationCounter implements Counter<DocumentationCounter> {

    private final SumCounter publicApiCounter;
    private final SumCounter publicUndocumentedApiCounter;

    public DocumentationCounter() {
      this.publicApiCounter = new IntSumCounter(PUBLIC_API_KEY);
      this.publicUndocumentedApiCounter = new IntSumCounter(PUBLIC_UNDOCUMENTED_API_KEY);
    }

    @Override
    public void aggregate(DocumentationCounter counter) {
      publicApiCounter.aggregate(counter.publicApiCounter);
      publicUndocumentedApiCounter.aggregate(counter.publicUndocumentedApiCounter);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      publicApiCounter.initialize(context);
      publicUndocumentedApiCounter.initialize(context);
    }

    public Optional<Integer> getPublicApiValue() {
      return publicApiCounter.getValue();
    }

    public Optional<Integer> getPublicUndocumentedApiValue() {
      return publicUndocumentedApiCounter.getValue();
    }
  }

  @Override
  public String getDescription() {
    return "Compute comment measures";
  }
}
