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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.batch.*;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.batch.components.PeriodsDefinition;
import org.sonar.core.DryRunIncompatible;

import java.util.List;

@DryRunIncompatible
@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public class TendencyDecorator implements Decorator {

  public static final String PROP_DAYS_DESCRIPTION = "Number of days the tendency should be calculated on.";

  private TimeMachine timeMachine;
  private TimeMachineQuery query;
  private TendencyAnalyser analyser;
  private List<Metric> metrics;

  public TendencyDecorator(TimeMachine timeMachine, MetricFinder metricFinder) {
    this.timeMachine = timeMachine;
    this.analyser = new TendencyAnalyser();
    this.metrics = Lists.newLinkedList();
    for (Metric metric : metricFinder.findAll()) {
      if (metric.isNumericType()) {
        metrics.add(metric);
      }
    }
  }

  TendencyDecorator(TimeMachine timeMachine, TimeMachineQuery query, TendencyAnalyser analyser) {
    this.timeMachine = timeMachine;
    this.query = query;
    this.analyser = analyser;
  }

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return metrics;
  }

  protected TimeMachineQuery initQuery(Project project) {
    int days = PeriodsDefinition.CORE_TENDENCY_DEPTH_DEFAULT_VALUE;

    // resource is set after
    query = new TimeMachineQuery(null)
      .setFrom(DateUtils.addDays(project.getAnalysisDate(), -days))
      .setToCurrentAnalysis(true)
      .setMetrics(metrics);
    return query;
  }

  protected TimeMachineQuery resetQuery(Project project, Resource resource) {
    if (query == null) {
      initQuery(project);
    }
    query.setResource(resource);
    return query;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      resetQuery(context.getProject(), resource);
      List<Object[]> fields = timeMachine.getMeasuresFields(query);
      ListMultimap<Metric, Double> valuesPerMetric = ArrayListMultimap.create();
      for (Object[] field : fields) {
        valuesPerMetric.put((Metric) field[1], (Double) field[2]);
      }

      for (Metric metric : query.getMetrics()) {
        Measure measure = context.getMeasure(metric);
        if (measure != null) {
          List<Double> values = valuesPerMetric.get(metric);
          values.add(measure.getValue());

          measure.setTendency(analyser.analyseLevel(valuesPerMetric.get(metric)));
          context.saveMeasure(measure);
        }
      }
    }
  }

  private boolean shouldDecorateResource(Resource resource) {
    return StringUtils.equals(Scopes.PROJECT, resource.getScope()) || StringUtils.equals(Scopes.DIRECTORY, resource.getScope());
  }
}
