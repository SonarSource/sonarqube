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

import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.CoreFormulaRepository;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.MetricRepository;

public class CoreMetricFormulaExecutorStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final CoreFormulaRepository coreFormulaRepository;

  public CoreMetricFormulaExecutorStep(TreeRootHolder treeRootHolder,
    MetricRepository metricRepository, MeasureRepository measureRepository,
    CoreFormulaRepository coreFormulaRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.coreFormulaRepository = coreFormulaRepository;
  }

  @Override
  public void execute() {
    FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(coreFormulaRepository.getFormulas())
      .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Execute formula to compute measures of Core metrics";
  }

}
