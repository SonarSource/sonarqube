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
package org.sonar.ce.task.projectanalysis.measure;

import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerContextImpl;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class MeasureComputersVisitor extends TypeAwareVisitorAdapter {

  private static final Logger LOGGER = Loggers.get(MeasureComputersVisitor.class);

  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final ConfigurationRepository settings;

  private final MeasureComputersHolder measureComputersHolder;
  private final ComponentIssuesRepository componentIssuesRepository;

  public MeasureComputersVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ConfigurationRepository settings,
    MeasureComputersHolder measureComputersHolder, ComponentIssuesRepository componentIssuesRepository) {
    super(CrawlerDepthLimit.reportMaxDepth(FILE).withViewsMaxDepth(SUBVIEW), POST_ORDER);
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.settings = settings;
    this.measureComputersHolder = measureComputersHolder;
    this.componentIssuesRepository = componentIssuesRepository;
  }

  @Override
  public void visitAny(org.sonar.ce.task.projectanalysis.component.Component component) {
    MeasureComputerContextImpl context = new MeasureComputerContextImpl(component, settings, measureRepository, metricRepository, componentIssuesRepository);
    for (MeasureComputerWrapper measureComputerWrapper : measureComputersHolder.getMeasureComputers()) {
      context.setDefinition(measureComputerWrapper.getDefinition());
      MeasureComputer measureComputer = measureComputerWrapper.getComputer();
      LOGGER.trace("Measure computer '{}' is computing component {}", measureComputer, component);
      measureComputer.compute(context);
    }
  }
}
