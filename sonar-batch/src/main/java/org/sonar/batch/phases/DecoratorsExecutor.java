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
package org.sonar.batch.phases;

import com.google.common.collect.Lists;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.deprecated.decorator.DecoratorsSelector;
import org.sonar.batch.deprecated.decorator.DefaultDecoratorContext;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.sensor.coverage.CoverageExclusions;

import java.util.Collection;
import java.util.List;

@BatchSide
public class DecoratorsExecutor {

  private final DecoratorsSelector decoratorsSelector;
  private final SonarIndex index;
  private final EventBus eventBus;
  private final Project project;
  private final CoverageExclusions coverageFilter;
  private final MeasureCache measureCache;
  private final MetricFinder metricFinder;
  private final DuplicationCache duplicationCache;
  private final AnalysisMode analysisMode;

  public DecoratorsExecutor(BatchExtensionDictionnary batchExtDictionnary,
    Project project, SonarIndex index, EventBus eventBus, CoverageExclusions coverageFilter, MeasureCache measureCache, MetricFinder metricFinder,
    DuplicationCache duplicationCache, AnalysisMode analysisMode) {
    this.measureCache = measureCache;
    this.metricFinder = metricFinder;
    this.duplicationCache = duplicationCache;
    this.analysisMode = analysisMode;
    this.decoratorsSelector = new DecoratorsSelector(batchExtDictionnary);
    this.index = index;
    this.eventBus = eventBus;
    this.project = project;
    this.coverageFilter = coverageFilter;
  }

  public void execute() {
    if (analysisMode.isPreview()) {
      // Decorators are not executed in preview mode
      return;
    }
    Collection<Decorator> decorators = decoratorsSelector.select(project);
    eventBus.fireEvent(new DecoratorsPhaseEvent(Lists.newArrayList(decorators), true));
    ((DefaultDecoratorContext) decorateResource(project, decorators, true)).end();
    eventBus.fireEvent(new DecoratorsPhaseEvent(Lists.newArrayList(decorators), false));
  }

  DecoratorContext decorateResource(Resource resource, Collection<Decorator> decorators, boolean executeDecorators) {
    List<DecoratorContext> childrenContexts = Lists.newArrayList();
    for (Resource child : index.getChildren(resource)) {
      boolean isModule = child instanceof Project;
      DefaultDecoratorContext childContext = (DefaultDecoratorContext) decorateResource(child, decorators, !isModule);
      childrenContexts.add(childContext.end());
    }

    DefaultDecoratorContext context = new DefaultDecoratorContext(resource, index, childrenContexts, measureCache, metricFinder, duplicationCache, coverageFilter);
    context.init();
    if (executeDecorators) {
      for (Decorator decorator : decorators) {
        executeDecorator(decorator, context, resource);
      }
    }
    return context;
  }

  void executeDecorator(Decorator decorator, DefaultDecoratorContext context, Resource resource) {
    try {
      eventBus.fireEvent(new DecoratorExecutionEvent(decorator, true));
      decorator.decorate(resource, context);
      eventBus.fireEvent(new DecoratorExecutionEvent(decorator, false));

    } catch (MessageException e) {
      throw e;

    } catch (Exception e) {
      // SONAR-2278 the resource should not be lost in exception stacktrace.
      throw new SonarException("Fail to decorate '" + resource + "'", e);
    }
  }

}
