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
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import javax.annotation.Nullable;

import java.util.List;

public final class DefaultPersistenceManager implements PersistenceManager {

  private ResourcePersister resourcePersister;
  private SourcePersister sourcePersister;
  private DependencyPersister dependencyPersister;
  private LinkPersister linkPersister;
  private EventPersister eventPersister;

  public DefaultPersistenceManager(ResourcePersister resourcePersister, SourcePersister sourcePersister,
    DependencyPersister dependencyPersister, LinkPersister linkPersister, EventPersister eventPersister) {
    this.resourcePersister = resourcePersister;
    this.sourcePersister = sourcePersister;
    this.dependencyPersister = dependencyPersister;
    this.linkPersister = linkPersister;
    this.eventPersister = eventPersister;
  }

  @Override
  public void clear() {
    resourcePersister.clear();
  }

  @Override
  public void saveProject(Project project, @Nullable Project parent) {
    resourcePersister.saveProject(project, parent);
  }

  @Override
  public Snapshot saveResource(Project project, Resource resource, @Nullable Resource parent) {
    if (ResourceUtils.isPersistable(resource)) {
      return resourcePersister.saveResource(project, resource, parent);
    }
    return null;
  }

  @Override
  public String getSource(Resource resource) {
    return sourcePersister.getSource(resource);
  }

  @Override
  public void saveDependency(Project project, Dependency dependency, Dependency parentDependency) {
    if (ResourceUtils.isPersistable(dependency.getFrom()) && ResourceUtils.isPersistable(dependency.getTo())) {
      dependencyPersister.saveDependency(project, dependency, parentDependency);
    }
  }

  @Override
  public void saveLink(Project project, ProjectLink link) {
    linkPersister.saveLink(project, link);
  }

  @Override
  public void deleteLink(Project project, String key) {
    linkPersister.deleteLink(project, key);
  }

  @Override
  public List<Event> getEvents(Resource resource) {
    return eventPersister.getEvents(resource);
  }

  @Override
  public void deleteEvent(Event event) {
    eventPersister.deleteEvent(event);
  }

  @Override
  public void saveEvent(Resource resource, Event event) {
    if (ResourceUtils.isPersistable(resource)) {
      eventPersister.saveEvent(resource, event);
    }
  }
}
