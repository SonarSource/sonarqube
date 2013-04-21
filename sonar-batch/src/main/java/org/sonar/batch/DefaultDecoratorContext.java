/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;

public class DefaultDecoratorContext implements DecoratorContext {

  private SonarIndex index;
  private Resource resource;
  private boolean readOnly = false;

  private List<DecoratorContext> childrenContexts;

  public DefaultDecoratorContext(Resource resource,
                                 SonarIndex index,
                                 List<DecoratorContext> childrenContexts) {
    this.index = index;
    this.resource = resource;
    this.childrenContexts = childrenContexts;
  }

  public DefaultDecoratorContext setReadOnly(boolean b) {
    readOnly = b;
    childrenContexts = null;
    return this;
  }

  public Project getProject() {
    return index.getProject();
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
    return index.getMeasures(resource, filter);
  }

  public Measure getMeasure(Metric metric) {
    return index.getMeasure(resource, metric);
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
    checkReadOnly("saveMeasure");
    index.addMeasure(resource, measure);
    return this;
  }

  public DecoratorContext saveMeasure(Metric metric, Double value) {
    checkReadOnly("saveMeasure");
    index.addMeasure(resource, new Measure(metric, value));
    return this;
  }

  /**
  * {@inheritDoc}
  */
  public List<Violation> getViolations(ViolationQuery violationQuery) {
    return index.getViolations(violationQuery);
  }
  
  /**
  * {@inheritDoc}
  */
  public List<Violation> getViolations() {
    return index.getViolations(resource);
  }

  public Dependency saveDependency(Dependency dependency) {
    checkReadOnly("addDependency");
    return index.addDependency(dependency);
  }

  public Set<Dependency> getDependencies() {
    return index.getDependencies();
  }

  public Collection<Dependency> getIncomingDependencies() {
    return index.getIncomingEdges(resource);
  }

  public Collection<Dependency> getOutgoingDependencies() {
    return index.getOutgoingEdges(resource);
  }

  public List<Event> getEvents() {
    return index.getEvents(resource);
  }

  public Event createEvent(String name, String description, String category, Date date) {
    return index.addEvent(resource, name, description, category, date);
  }

  public void deleteEvent(Event event) {
    index.deleteEvent(event);
  }

  public DefaultDecoratorContext saveViolation(Violation violation, boolean force) {
    if (violation.getResource() == null) {
      violation.setResource(resource);
    }
    index.addViolation(violation, force);
    return this;
  }

  public DefaultDecoratorContext saveViolation(Violation violation) {
    return saveViolation(violation, false);
  }
}
