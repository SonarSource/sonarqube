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
import org.sonar.server.computation.formula.SumFormula;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.PUBLIC_API_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY;

/**
 * Computes documentation measures on files and then aggregates them on higher components.
 */
public class DocumentationMeasuresStep implements ComputationStep {

  private static final ImmutableList<Formula> FORMULAS = ImmutableList.<Formula>of(
    new SumFormula(PUBLIC_API_KEY),
    new SumFormula(PUBLIC_UNDOCUMENTED_API_KEY),
    new PublicApiDensityFormula()
    );

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public DocumentationMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
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

  private static class PublicApiDensityFormula implements Formula<BiSumCounter> {

    @Override
    public BiSumCounter createNewCounter() {
      return new BiSumCounter(PUBLIC_API_KEY, PUBLIC_UNDOCUMENTED_API_KEY);
    }

    @Override
    public Optional<Measure> createMeasure(BiSumCounter counter, CreateMeasureContext context) {
      if (counter.getValue1().isPresent() && counter.getValue2().isPresent()) {
        double publicApis = counter.getValue1().get();
        double publicUndocumentedApis = counter.getValue2().get();
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
      return new String[] {PUBLIC_DOCUMENTED_API_DENSITY_KEY};
    }
  }

  @Override
  public String getDescription() {
    return "Aggregation of documentation measures";
  }
}
