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
package org.sonar.batch.index;

import org.sonar.api.batch.Event;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.List;

public final class DefaultPersistenceManager implements PersistenceManager {

  private ResourcePersister resourcePersister;
  private SourcePersister sourcePersister;
  private MeasurePersister measurePersister;
  private DependencyPersister dependencyPersister;
  private LinkPersister linkPersister;
  private EventPersister eventPersister;

  public DefaultPersistenceManager(ResourcePersister resourcePersister, SourcePersister sourcePersister,
                                   MeasurePersister measurePersister, DependencyPersister dependencyPersister,
                                   LinkPersister linkPersister, EventPersister eventPersister) {
    this.resourcePersister = resourcePersister;
    this.sourcePersister = sourcePersister;
    this.measurePersister = measurePersister;
    this.dependencyPersister = dependencyPersister;
    this.linkPersister = linkPersister;
    this.eventPersister = eventPersister;
  }

  public void clear() {
    resourcePersister.clear();
    sourcePersister.clear();
  }

  public void setDelayedMode(boolean b) {
    measurePersister.setDelayedMode(b);
  }

  public void dump() {
    measurePersister.dump();
  }

  public void saveProject(Project project, Project parent) {
    resourcePersister.saveProject(project, parent);
  }

  public Snapshot saveResource(Project project, Resource resource, Resource parent) {
    if (ResourceUtils.isPersistable(resource)) {
      return resourcePersister.saveResource(project, resource, parent);
    }
    return null;
  }

  public void setSource(Resource file, String source) {
    sourcePersister.saveSource(file, source);
  }

  public String getSource(Resource resource) {
    return sourcePersister.getSource(resource);
  }

  public void saveMeasure(Resource resource, Measure measure) {
    if (ResourceUtils.isPersistable(resource)) {
      measurePersister.saveMeasure(resource, measure);
    }
  }

  public Measure reloadMeasure(Measure measure) {
    return measurePersister.reloadMeasure(measure);
  }

  public void saveDependency(Project project, Dependency dependency, Dependency parentDependency) {
    if (ResourceUtils.isPersistable(dependency.getFrom()) && ResourceUtils.isPersistable(dependency.getTo())) {
      dependencyPersister.saveDependency(project, dependency, parentDependency);
    }
  }

  public void saveLink(Project project, ProjectLink link) {
    linkPersister.saveLink(project, link);
  }

  public void deleteLink(Project project, String key) {
    linkPersister.deleteLink(project, key);
  }

  public List<Event> getEvents(Resource resource) {
    return eventPersister.getEvents(resource);
  }

  public void deleteEvent(Event event) {
    eventPersister.deleteEvent(event);
  }

  public void saveEvent(Resource resource, Event event) {
    if (ResourceUtils.isPersistable(resource)) {
      eventPersister.saveEvent(resource, event);
    }
  }
}
