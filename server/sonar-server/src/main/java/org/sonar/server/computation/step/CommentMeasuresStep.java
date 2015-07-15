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
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.BiSumCounter;
import org.sonar.server.computation.formula.CreateMeasureContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;

/**
 * Computes comments measures on files and then aggregates them on higher components.
 */
public class CommentMeasuresStep implements ComputationStep {

  private static final ImmutableList<Formula> FORMULAS = ImmutableList.<Formula>of(
    // TODO add this formula when {@link CoreMetrics.DUPLICATED_LINES_DENSITY} will be computed in CE
    // new SumFormula(CoreMetrics.COMMENT_LINES_KEY),
    new CommentDensityFormula()
    );

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public CommentMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
    FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(FORMULAS)
      .visit(treeRootHolder.getRoot());
  }

  private static class CommentDensityFormula implements Formula<BiSumCounter> {

    @Override
    public BiSumCounter createNewCounter() {
      return new BiSumCounter(NCLOC_KEY, COMMENT_LINES_KEY);
    }

    @Override
    public Optional<Measure> createMeasure(BiSumCounter counter, CreateMeasureContext context) {
      if (counter.getValue1().isPresent() && counter.getValue2().isPresent()) {
        double nclocs = counter.getValue1().get();
        double comments = counter.getValue2().get();
        double divisor = nclocs + comments;
        if (divisor > 0d) {
          double value = 100d * (comments / divisor);
          return Optional.of(Measure.newMeasureBuilder().create(value));
        }
      }
      return Optional.absent();
    }

    @Override
    public String[] getOutputMetricKeys() {
      return new String[] {COMMENT_LINES_DENSITY_KEY};
    }
  }

  @Override
  public String getDescription() {
    return "Aggregation of comment measures";
  }
}
