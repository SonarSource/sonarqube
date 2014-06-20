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
package org.sonar.batch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.measure.MeasurementFilters;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DefaultDecoratorContext implements DecoratorContext {

  private static final String SAVE_MEASURE_METHOD = "saveMeasure";
  private SonarIndex sonarIndex;
  private Resource resource;
  private MeasurementFilters measurementFilters;
  private boolean readOnly = false;

  private List<DecoratorContext> childrenContexts;

  private ListMultimap<String, Measure> measuresByMetric = ArrayListMultimap.create();
  private MeasureCache measureCache;
  private MetricFinder metricFinder;

  public DefaultDecoratorContext(Resource resource,
    SonarIndex index,
    List<DecoratorContext> childrenContexts,
    MeasurementFilters measurementFilters, MeasureCache measureCache, MetricFinder metricFinder) {
    this.sonarIndex = index;
    this.resource = resource;
    this.childrenContexts = childrenContexts;
    this.measurementFilters = measurementFilters;
    this.measureCache = measureCache;
    this.metricFinder = metricFinder;
  }

  public void init() {
    Iterable<Measure> unfiltered = measureCache.byResource(resource);
    for (Measure measure : unfiltered) {
      measuresByMetric.put(measure.getMetricKey(), measure);
    }
  }

  public DefaultDecoratorContext end() {
    readOnly = true;
    childrenContexts = null;
    for (Measure measure : measuresByMetric.values()) {
      measureCache.put(resource, measure);
    }
    return this;
  }

  public Project getProject() {
    return sonarIndex.getProject();
  }

  public List<DecoratorContext> getChildren() {
    checkReadOnly("getModules");
    return childrenContexts;
  }

  private void checkReadOnly(String methodName) {
    if (readOnly) {
      throw new IllegalStateException("Method DecoratorContext." + methodName + "() can not be executed on children.");
    }
  }

  public <M> M getMeasures(MeasuresFilter<M> filter) {
    Collection<Measure> unfiltered;
    if (filter instanceof MeasuresFilters.MetricFilter) {
      // optimization
      unfiltered = measuresByMetric.get(((MeasuresFilters.MetricFilter<M>) filter).filterOnMetricKey());
    } else {
      unfiltered = measuresByMetric.values();
    }
    return filter.filter(unfiltered);
  }

  public Measure getMeasure(Metric metric) {
    return getMeasures(MeasuresFilters.metric(metric));
  }

  public Collection<Measure> getChildrenMeasures(MeasuresFilter filter) {
    List<Measure> result = Lists.newArrayList();
    for (DecoratorContext childContext : childrenContexts) {
      Object childResult = childContext.getMeasures(filter);
      if (childResult != null) {
        if (childResult instanceof Collection) {
          result.addAll((Collection) childResult);
        } else {
          result.add((Measure) childResult);
        }
      }
    }
    return result;
  }

  public Collection<Measure> getChildrenMeasures(Metric metric) {
    return getChildrenMeasures(MeasuresFilters.metric(metric));
  }

  public Resource getResource() {
    return resource;
  }

  public DecoratorContext saveMeasure(Measure measure) {
    checkReadOnly(SAVE_MEASURE_METHOD);
    Metric metric = metricFinder.findByKey(measure.getMetricKey());
    if (metric == null) {
      throw new SonarException("Unknown metric: " + measure.getMetricKey());
    }
    measure.setMetric(metric);
    if (measurementFilters.accept(resource, measure)) {
      List<Measure> metricMeasures = measuresByMetric.get(measure.getMetricKey());

      boolean add = true;
      if (metricMeasures != null) {
        int index = metricMeasures.indexOf(measure);
        if (index > -1) {
          if (metricMeasures.get(index) == measure) {
            add = false;
          } else if (measure.getMetric().equals(CoreMetrics.TESTS)) {
            // Hack for SONAR-5212
            measuresByMetric.remove(measure.getMetric().getKey(), metricMeasures.get(index));
          } else {
            throw new SonarException("Can not add twice the same measure on " + resource + ": " + measure);
          }
        }
      }
      if (add) {
        measuresByMetric.put(measure.getMetricKey(), measure);
      }
    }
    return this;
  }

  public DecoratorContext saveMeasure(Metric metric, Double value) {
    checkReadOnly(SAVE_MEASURE_METHOD);
    saveMeasure(new Measure(metric, value));
    return this;
  }

  /**
  * {@inheritDoc}
  */
  public List<Violation> getViolations(ViolationQuery violationQuery) {
    return sonarIndex.getViolations(violationQuery);
  }

  /**
  * {@inheritDoc}
  */
  public List<Violation> getViolations() {
    return sonarIndex.getViolations(resource);
  }

  public Dependency saveDependency(Dependency dependency) {
    checkReadOnly("addDependency");
    return sonarIndex.addDependency(dependency);
  }

  public Set<Dependency> getDependencies() {
    return sonarIndex.getDependencies();
  }

  public Collection<Dependency> getIncomingDependencies() {
    return sonarIndex.getIncomingEdges(resource);
  }

  public Collection<Dependency> getOutgoingDependencies() {
    return sonarIndex.getOutgoingEdges(resource);
  }

  public List<Event> getEvents() {
    return sonarIndex.getEvents(resource);
  }

  public Event createEvent(String name, String description, String category, Date date) {
    return sonarIndex.addEvent(resource, name, description, category, date);
  }

  public void deleteEvent(Event event) {
    sonarIndex.deleteEvent(event);
  }

  public DefaultDecoratorContext saveViolation(Violation violation, boolean force) {
    if (violation.getResource() == null) {
      violation.setResource(resource);
    }
    sonarIndex.addViolation(violation, force);
    return this;
  }

  public DefaultDecoratorContext saveViolation(Violation violation) {
    return saveViolation(violation, false);
  }
}
