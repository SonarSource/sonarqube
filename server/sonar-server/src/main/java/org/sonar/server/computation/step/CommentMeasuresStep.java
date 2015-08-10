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
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.FileAggregateContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentCrawler;
import org.sonar.server.computation.formula.SumCounter;
import org.sonar.server.computation.formula.SumFormula;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.COMMENTED_OUT_CODE_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY;
import static org.sonar.server.computation.component.Component.Type.FILE;

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
    this.formulas = ImmutableList.<Formula>of(
      new SumFormula(COMMENTED_OUT_CODE_LINES_KEY),
      new DocumentationFormula(),
      new CommentDensityFormula()
      );
  }

  @Override
  public void execute() {
    FormulaExecutorComponentCrawler.newBuilder(metricRepository, measureRepository)
      .buildFor(formulas)
      .visit(treeRootHolder.getRoot());
  }

  private class CommentDensityFormula implements Formula<SumCounter> {

    private final Metric nclocMetric;

    public CommentDensityFormula() {
      this.nclocMetric = metricRepository.getByKey(NCLOC_KEY);
    }

    @Override
    public SumCounter createNewCounter() {
      return new SumCounter(COMMENT_LINES_KEY);
    }

    @Override
    public Optional<Measure> createMeasure(SumCounter counter, CreateMeasureContext context) {
      return createCommentLinesMeasure(counter, context)
        .or(createCommentLinesDensityMeasure(counter, context));
    }

    private Optional<Measure> createCommentLinesMeasure(SumCounter counter, CreateMeasureContext context) {
      Optional<Integer> commentLines = counter.getValue();
      if (context.getMetric().getKey().equals(COMMENT_LINES_KEY) && context.getComponent().getType().isHigherThan(FILE) && commentLines.isPresent()) {
        return Optional.of(Measure.newMeasureBuilder().create(commentLines.get()));
      }
      return Optional.absent();
    }

    private Optional<Measure> createCommentLinesDensityMeasure(SumCounter counter, CreateMeasureContext context) {
      if (context.getMetric().getKey().equals(COMMENT_LINES_DENSITY_KEY)) {
        Optional<Measure> nclocsOpt = measureRepository.getRawMeasure(context.getComponent(), nclocMetric);
        Optional<Integer> commentsOpt = counter.getValue();
        if (nclocsOpt.isPresent() && commentsOpt.isPresent()) {
          double nclocs = nclocsOpt.get().getIntValue();
          double comments = commentsOpt.get();
          double divisor = nclocs + comments;
          if (divisor > 0d) {
            double value = 100d * (comments / divisor);
            return Optional.of(Measure.newMeasureBuilder().create(value));
          }
        }
      }
      return Optional.absent();
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
      return getMeasure(context, counter.getPublicApiValue(), PUBLIC_API_KEY)
        .or(getMeasure(context, counter.getPublicUndocumentedApiValue(), PUBLIC_UNDOCUMENTED_API_KEY))
        .or(getDensityMeasure(counter, context));
    }

    private static Optional<Measure> getMeasure(CreateMeasureContext context, Optional<Integer> metricValue, String metricKey) {
      if (context.getMetric().getKey().equals(metricKey) && metricValue.isPresent() && context.getComponent().getType().isHigherThan(Component.Type.FILE)) {
        return Optional.of(Measure.newMeasureBuilder().create(metricValue.get()));
      }
      return Optional.absent();
    }

    private static Optional<Measure> getDensityMeasure(DocumentationCounter counter, CreateMeasureContext context) {
      if (context.getMetric().getKey().equals(PUBLIC_DOCUMENTED_API_DENSITY_KEY) && counter.getPublicApiValue().isPresent()
        && counter.getPublicUndocumentedApiValue().isPresent()) {
        double publicApis = counter.getPublicApiValue().get();
        double publicUndocumentedApis = counter.getPublicUndocumentedApiValue().get();
        if (publicApis > 0d) {
          double documentedAPI = publicApis - publicUndocumentedApis;
          double value = 100d * (documentedAPI / publicApis);
          return Optional.of(Measure.newMeasureBuilder().create(value));
        }
      }
      return Optional.absent();
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
      this.publicApiCounter = new SumCounter(PUBLIC_API_KEY);
      this.publicUndocumentedApiCounter = new SumCounter(PUBLIC_UNDOCUMENTED_API_KEY);
    }

    @Override
    public void aggregate(DocumentationCounter counter) {
      publicApiCounter.aggregate(counter.publicApiCounter);
      publicUndocumentedApiCounter.aggregate(counter.publicUndocumentedApiCounter);
    }

    @Override
    public void aggregate(FileAggregateContext context) {
      publicApiCounter.aggregate(context);
      publicUndocumentedApiCounter.aggregate(context);
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
    return "Aggregation of comment measures";
  }
}
