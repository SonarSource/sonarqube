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
package org.sonar.batch.index;

import org.sonar.api.batch.Event;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

import java.util.List;

public final class DefaultPersistenceManager implements PersistenceManager {

  private ResourcePersister resourcePersister;
  private SourcePersister sourcePersister;
  private MeasurePersister measurePersister;
  private DependencyPersister dependencyPersister;
  private ViolationPersister violationPersister;
  private LinkPersister linkPersister;
  private EventPersister eventPersister;

  public DefaultPersistenceManager(ResourcePersister resourcePersister, SourcePersister sourcePersister,
                                   MeasurePersister measurePersister, DependencyPersister dependencyPersister,
                                   ViolationPersister violationPersister, LinkPersister linkPersister,
                                   EventPersister eventPersister) {
    this.resourcePersister = resourcePersister;
    this.sourcePersister = sourcePersister;
    this.measurePersister = measurePersister;
    this.dependencyPersister = dependencyPersister;
    this.violationPersister = violationPersister;
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

  public void saveProject(Project project) {
    resourcePersister.saveProject(project);
  }

  public Snapshot saveResource(Project project, Resource resource) {
    return resourcePersister.saveResource(project, resource);
  }

  public void setSource(Project project, Resource resource, String source) {
    sourcePersister.saveSource(project, resource, source);
  }

  public void saveMeasure(Project project, Resource resource, Measure measure) {
    measurePersister.saveMeasure(project, resource, measure);
  }

  public void saveDependency(Project project, Dependency dependency, Dependency parentDependency) {
    dependencyPersister.saveDependency(project, dependency, parentDependency);
  }

  public List<RuleFailureModel> loadPreviousViolations(Resource resource) {
    return violationPersister.getPreviousViolations(resource);
  }

  public void saveOrUpdateViolation(Project project, Violation violation, RuleFailureModel oldModel) {
    violationPersister.saveOrUpdateViolation(project, violation, oldModel);
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

  public void saveEvent(Project project, Resource resource, Event event) {
    eventPersister.saveEvent(project, resource, event);
  }
}
