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

import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.indexer.DefaultSonarIndex;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DefaultSensorContext implements SensorContext {

  private DefaultSonarIndex index;
  private Project project;

  public DefaultSensorContext(DefaultSonarIndex index, Project project) {
    this.index = index;
    this.project = project;
  }

  public Project getProject() {
    return project;
  }

  public Measure getMeasure(Metric metric) {
    return index.getMeasure(project, metric);
  }

  public <M> M getMeasures(MeasuresFilter<M> filter) {
    return index.getMeasures(project, filter);
  }

  public Measure saveMeasure(Measure measure) {
    return index.saveMeasure(project, measure);
  }

  public Measure saveMeasure(Metric metric, Double value) {
    return index.saveMeasure(project, new Measure(metric, value));
  }

  public Measure getMeasure(Resource resource, Metric metric) {
    return index.getMeasure(resource, metric);
  }

  public String saveResource(Resource resource) {
    Resource persistedResource = index.addResource(resource);
    if (persistedResource!=null) {
      return persistedResource.getEffectiveKey();
    }
    return null;
  }

  public Resource getResource(Resource resource) {
    return index.getResource(resource);
  }

  public <M> M getMeasures(Resource resource, MeasuresFilter<M> filter) {
    return index.getMeasures(resource, filter);
  }

  public Measure saveMeasure(Resource resource, Metric metric, Double value) {
    return index.saveMeasure(resourceOrProject(resource), new Measure(metric, value));
  }

  public Measure saveMeasure(Resource resource, Measure measure) {
    return index.saveMeasure(resourceOrProject(resource), measure);
  }

  public void saveViolation(Violation violation) {
    if (violation.getResource()==null) {
      violation.setResource(resourceOrProject(violation.getResource()));
    }
    index.addViolation(violation);
  }

  public void saveViolations(Collection<Violation> violations) {
    if (violations!=null) {
      for (Violation violation : violations) {
        saveViolation(violation);
      }
    }
  }

  public Dependency saveDependency(Dependency dependency) {
    return index.saveDependency(dependency);
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

  public void saveSource(Resource resource, String source) {
    index.setSource(resource, source);
  }

  public void saveLink(ProjectLink link) {
    index.saveLink(link);
  }

  public void deleteLink(String key) {
    index.deleteLink(key);
  }

  public List<Event> getEvents(Resource resource) {
    return index.getEvents(resource);
  }

  public Event createEvent(Resource resource, String name, String description, String category, Date date) {
    return index.createEvent(resource, name, description, category, date);
  }

  public void deleteEvent(Event event) {
    index.deleteEvent(event);
  }

  private Resource resourceOrProject(Resource resource) {
    return (resource!=null ? resource : project);
  }
}
