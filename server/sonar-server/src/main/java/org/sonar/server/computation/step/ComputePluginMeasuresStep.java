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

import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.ProjectSettingsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.measure.MeasureComputersHolder;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.api.MeasureComputerImplementationContext;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class ComputePluginMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final ProjectSettingsRepository settings;

  private final MeasureComputersHolder measureComputersHolder;

  public ComputePluginMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository, ProjectSettingsRepository settings,
    MeasureComputersHolder measureComputersHolder) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.settings = settings;
    this.measureComputersHolder = measureComputersHolder;
  }

  @Override
  public void execute() {
    new NewMetricDefinitionsVisitor().visit(treeRootHolder.getRoot());
  }

  private class NewMetricDefinitionsVisitor extends DepthTraversalTypeAwareVisitor {

    public NewMetricDefinitionsVisitor() {
      super(FILE, PRE_ORDER);
    }

    @Override
    public void visitAny(org.sonar.server.computation.component.Component component) {
      for (MeasureComputer computer : measureComputersHolder.getMeasureComputers()) {
        MeasureComputerImplementationContext measureComputerContext = new MeasureComputerImplementationContext(component, computer, settings, measureRepository, metricRepository);
        computer.getImplementation().compute(measureComputerContext);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Compute measures from plugin";
  }

}
