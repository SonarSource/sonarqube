/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.core.measure.MeasurementFilters;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DefaultSensorContext implements SensorContext {

  private SonarIndex index;
  private Project project;
  private MeasurementFilters filters;

  public DefaultSensorContext(SonarIndex index, Project project, MeasurementFilters filters) {
    this.index = index;
    this.project = project;
    this.filters = filters;
  }

  public Project getProject() {
    return project;
  }

  public boolean index(Resource resource) {
    return true;
  }

  public boolean index(Resource resource, Resource parentReference) {
    return true;
  }

  public boolean isExcluded(Resource reference) {
    return index.isExcluded(reference);
  }

  public boolean isIndexed(Resource reference, boolean acceptExcluded) {
    return index.isIndexed(reference, acceptExcluded);
  }

  public Resource getParent(Resource reference) {
    return index.getParent(reference);
  }

  public Collection<Resource> getChildren(Resource reference) {
    return index.getChildren(reference);
  }

  public Measure getMeasure(Metric metric) {
    return index.getMeasure(project, metric);
  }

  public <M> M getMeasures(MeasuresFilter<M> filter) {
    return index.getMeasures(project, filter);
  }

  public Measure saveMeasure(Measure measure) {
    return index.addMeasure(project, measure);
  }

  public Measure saveMeasure(Metric metric, Double value) {
    return index.addMeasure(project, new Measure(metric, value));
  }

  public Measure getMeasure(Resource resource, Metric metric) {
    return index.getMeasure(resource, metric);
  }

  public String saveResource(Resource resource) {
    Resource persistedResource = index.addResource(resource);
    if (persistedResource != null) {
      return persistedResource.getEffectiveKey();
    }
    return null;
  }

  public boolean saveResource(Resource resource, Resource parentReference) {
    return index.index(resource, parentReference);
  }

  public Resource getResource(Resource resource) {
    return index.getResource(resource);
  }

  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    return index.getMeasures(resource, filter);
  }

  public Measure saveMeasure(Resource resource, Metric metric, Double value) {
    return saveMeasure(resource, new Measure(metric, value));
  }

  public Measure saveMeasure(Resource resource, Measure measure) {
    if (filters.accept(resource, measure)) {
      return index.addMeasure(resourceOrProject(resource), measure);
    } else {
      return measure;
    }
  }

  public void saveViolation(Violation violation, boolean force) {
    if (violation.getResource() == null) {
      violation.setResource(resourceOrProject(violation.getResource()));
    }
    index.addViolation(violation, force);
  }

  public void saveViolation(Violation violation) {
    saveViolation(violation, false);
  }

  public void saveViolations(Collection<Violation> violations) {
    if (violations != null) {
      for (Violation violation : violations) {
        saveViolation(violation);
      }
    }
  }

  public Dependency saveDependency(Dependency dependency) {
    return index.addDependency(dependency);
  }

  public Set<Dependency> getDependencies() {
    return index.getDependencies();
  }

  public Collection<Dependency> getIncomingDependencies(Resource to) {
    return index.getIncomingEdges(resourceOrProject(to));
  }

  public Collection<Dependency> getOutgoingDependencies(Resource from) {
    return index.getOutgoingEdges(resourceOrProject(from));
  }

  public void saveSource(Resource reference, String source) {
  }

  public void saveLink(ProjectLink link) {
    index.addLink(link);
  }

  public void deleteLink(String key) {
    index.deleteLink(key);
  }

  public List<Event> getEvents(Resource resource) {
    return index.getEvents(resource);
  }

  public Event createEvent(Resource resource, String name, String description, String category, Date date) {
    return index.addEvent(resource, name, description, category, date);
  }

  public void deleteEvent(Event event) {
    index.deleteEvent(event);
  }

  private Resource resourceOrProject(Resource resource) {
    return resource != null ? resource : project;
  }
}
