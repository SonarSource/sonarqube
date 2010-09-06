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
package org.sonar.api.batch;

import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.graph.DirectedGraphAccessor;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class SonarIndex implements DirectedGraphAccessor<Resource, Dependency> {

  public abstract Project getRootProject();

  public abstract Project getProject();

  public abstract Resource getResource(Resource resource);

  public final Collection<Resource> getResources() {
    return getVertices();
  }

  public abstract List<Resource> getChildren(Resource resource);

  public abstract Resource addResource(Resource resource);

  public abstract Measure getMeasure(Resource resource, Metric metric);

  public abstract <M> M getMeasures(Resource resource, MeasuresFilter<M> filter);

  public abstract void setSource(Resource resource, String source);

  public abstract void addViolation(Violation violation);

  public abstract Measure saveMeasure(Resource resource, Measure measure);

  public abstract Dependency saveDependency(Dependency dependency);

  public abstract Set<Dependency> getDependencies();

  public abstract void saveLink(ProjectLink link);

  public abstract void deleteLink(String key);

  public abstract List<Event> getEvents(Resource resource);

  public abstract void deleteEvent(Event event);

  public abstract Event createEvent(Resource resource, String name, String description, String category, Date date);

  public final Collection<Dependency> getOutgoingDependencies(Resource from) {
    return getOutgoingEdges(from);
  }

  public final Collection<Dependency> getIncomingDependencies(Resource to) {
    return getIncomingEdges(to);
  }
}
