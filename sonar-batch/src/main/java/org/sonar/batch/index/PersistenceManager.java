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

import java.util.List;

public interface PersistenceManager {
  void clear();

  void setDelayedMode(boolean b);

  void dump();

  void saveProject(Project project, Project parent);

  Snapshot saveResource(Project project, Resource resource, Resource parent);

  void setSource(Resource file, String source);

  String getSource(Resource resource);

  void saveMeasure(Resource resource, Measure measure);

  Measure reloadMeasure(Measure measure);

  void saveDependency(Project project, Dependency dependency, Dependency parentDependency);

  void saveLink(Project project, ProjectLink link);

  void deleteLink(Project project, String key);

  List<Event> getEvents(Resource resource);

  void deleteEvent(Event event);

  void saveEvent(Resource resource, Event event);
}
