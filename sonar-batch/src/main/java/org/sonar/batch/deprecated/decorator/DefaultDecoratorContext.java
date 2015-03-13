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
package org.sonar.batch.deprecated.decorator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.duplication.DuplicationUtils;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.sensor.coverage.CoverageExclusions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DefaultDecoratorContext implements DecoratorContext {

  private static final String SAVE_MEASURE_METHOD = "saveMeasure";
  private SonarIndex sonarIndex;
  private Resource resource;
  private boolean readOnly = false;

  private List<DecoratorContext> childrenContexts;

  private ListMultimap<String, Measure> measuresByMetric = ArrayListMultimap.create();
  private MeasureCache measureCache;
  private MetricFinder metricFinder;
  private final DuplicationCache duplicationCache;
  private final CoverageExclusions coverageFilter;

  public DefaultDecoratorContext(Resource resource,
    SonarIndex index,
    List<DecoratorContext> childrenContexts,
    MeasureCache measureCache, MetricFinder metricFinder, DuplicationCache duplicationCache, CoverageExclusions coverageFilter) {
    this.sonarIndex = index;
    this.resource = resource;
    this.childrenContexts = childrenContexts;
    this.measureCache = measureCache;
    this.metricFinder = metricFinder;
    this.duplicationCache = duplicationCache;
    this.coverageFilter = coverageFilter;
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

  @Override
  public Project getProject() {
    return sonarIndex.getProject();
  }

  @Override
  public List<DecoratorContext> getChildren() {
    checkReadOnly("getModules");
    return childrenContexts;
  }

  private void checkReadOnly(String methodName) {
    if (readOnly) {
      throw new IllegalStateException("Method DecoratorContext." + methodName + "() can not be executed on children.");
    }
  }

  @Override
  public <M> M getMeasures(MeasuresFilter<M> filter) {
    Collection<Measure> unfiltered;
    if (filter instanceof MeasuresFilters.MetricFilter) {
      unfiltered = getMeasuresOfASingleMetric(filter);
    } else {
      unfiltered = measuresByMetric.values();
    }
    return filter.filter(unfiltered);
  }

  private <M> Collection<Measure> getMeasuresOfASingleMetric(MeasuresFilter<M> filter) {
    Collection<Measure> unfiltered;
    String metricKey = ((MeasuresFilters.MetricFilter<M>) filter).filterOnMetricKey();
    if (CoreMetrics.DUPLICATIONS_DATA_KEY.equals(metricKey)) {
      // Hack for SONAR-5765
      Iterable<DefaultDuplication> group = duplicationCache.byComponent(resource.getEffectiveKey());
      unfiltered = Arrays.asList(new Measure(CoreMetrics.DUPLICATIONS_DATA, DuplicationUtils.toXml(group)));
    } else {
      // optimization
      unfiltered = measuresByMetric.get(metricKey);
    }
    return unfiltered;
  }

  @Override
  public Measure getMeasure(Metric metric) {
    return getMeasures(MeasuresFilters.metric(metric));
  }

  @Override
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

  @Override
  public Collection<Measure> getChildrenMeasures(Metric metric) {
    return getChildrenMeasures(MeasuresFilters.metric(metric));
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public DecoratorContext saveMeasure(Measure measure) {
    checkReadOnly(SAVE_MEASURE_METHOD);
    Metric metric = metricFinder.findByKey(measure.getMetricKey());
    if (metric == null) {
      throw new SonarException("Unknown metric: " + measure.getMetricKey());
    }
    measure.setMetric(metric);
    if (coverageFilter.accept(resource, measure)) {
      List<Measure> metricMeasures = measuresByMetric.get(measure.getMetricKey());

      boolean add = true;
      if (metricMeasures != null) {
        int index = metricMeasures.indexOf(measure);
        if (index > -1) {
          if (metricMeasures.get(index) == measure) {
            add = false;
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

  @Override
  public DecoratorContext saveMeasure(Metric metric, Double value) {
    checkReadOnly(SAVE_MEASURE_METHOD);
    saveMeasure(new Measure(metric, value));
    return this;
  }

  @Override
  public Dependency saveDependency(Dependency dependency) {
    checkReadOnly("addDependency");
    return sonarIndex.addDependency(dependency);
  }

  @Override
  public Set<Dependency> getDependencies() {
    return sonarIndex.getDependencies();
  }

  @Override
  public Collection<Dependency> getIncomingDependencies() {
    return sonarIndex.getIncomingEdges(resource);
  }

  @Override
  public Collection<Dependency> getOutgoingDependencies() {
    return sonarIndex.getOutgoingEdges(resource);
  }

  @Override
  public DefaultDecoratorContext saveViolation(Violation violation, boolean force) {
    if (violation.getResource() == null) {
      violation.setResource(resource);
    }
    sonarIndex.addViolation(violation, force);
    return this;
  }

  @Override
  public DefaultDecoratorContext saveViolation(Violation violation) {
    return saveViolation(violation, false);
  }
}
