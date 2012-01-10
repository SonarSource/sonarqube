/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.collect.Maps;
import org.sonar.api.batch.Event;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReadOnlyPersistenceManager implements PersistenceManager {

  private Map<Resource, String> sources = Maps.newHashMap();

  public void clear() {
    sources.clear();
  }

  public void setDelayedMode(boolean b) {
  }

  public void dump() {
  }

  public void saveProject(Project project, Project parent) {
  }

  public Snapshot saveResource(Project project, Resource resource, Resource parent) {
    return null;
  }

  public void setSource(Resource file, String source) {
    sources.put(file, source);
  }

  public String getSource(Resource resource) {
    return sources.get(resource);
  }

  public void saveMeasure(Resource resource, Measure measure) {
  }

  public Measure reloadMeasure(Measure measure) {
    return measure;
  }

  public void saveDependency(Project project, Dependency dependency, Dependency parentDependency) {
  }

  public void saveLink(Project project, ProjectLink link) {
  }

  public void deleteLink(Project project, String key) {
  }

  public List<Event> getEvents(Resource resource) {
    return Collections.emptyList();
  }

  public void deleteEvent(Event event) {
  }

  public void saveEvent(Resource resource, Event event) {
  }
}
