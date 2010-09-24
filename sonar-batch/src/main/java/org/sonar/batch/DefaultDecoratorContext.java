/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Event;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.indexer.Bucket;
import org.sonar.batch.indexer.DefaultSonarIndex;

import java.util.*;

public class DefaultDecoratorContext implements DecoratorContext {

  private DatabaseSession session;
  private DefaultSonarIndex index;
  private Resource resource;
  private boolean readOnly = false;

  private List<DecoratorContext> childrenContexts;
  private ViolationsDao violationsDao;

  public DefaultDecoratorContext(Resource resource,
                                 DefaultSonarIndex index,
                                 List<DecoratorContext> childrenContexts,
                                 DatabaseSession session,
                                 ViolationsDao violationsDao) {
    this.index = index;
    this.session = session;
    this.resource = resource;
    this.childrenContexts = childrenContexts;
    this.violationsDao = violationsDao;
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
    return index.getMeasures(resource, MeasuresFilters.metric(metric));
  }

  public Collection<Measure> getChildrenMeasures(MeasuresFilter filter) {
    List<Measure> result = new ArrayList<Measure>();
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
    index.saveMeasure(resource, measure);
    return this;
  }

  public DecoratorContext saveMeasure(Metric metric, Double value) {
    checkReadOnly("saveMeasure");
    index.saveMeasure(resource, new Measure(metric, value));
    return this;
  }


  public List<Violation> getViolations() {
      Bucket bucket = index.getBucket(resource);
      if (bucket != null && bucket.getSnapshotId() != null) {
        return violationsDao.getViolations(resource, bucket.getSnapshotId());
      }

    return null;
  }

  public Dependency saveDependency(Dependency dependency) {
    checkReadOnly("saveDependency");
    return index.saveDependency(dependency);
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

  protected DatabaseSession getSession() {
    return session;
  }

  public List<Event> getEvents() {
    return index.getEvents(resource);
  }

  public Event createEvent(String name, String description, String category, Date date) {
    return index.createEvent(resource, name, description, category, date);
  }

  public void deleteEvent(Event event) {
    index.deleteEvent(event);
  }

  public DefaultDecoratorContext saveViolation(Violation violation) {
    if (violation.getResource() == null) {
      violation.setResource(resource);
    }
    index.addViolation(violation);
    return this;
  }
}
