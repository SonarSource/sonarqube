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
import org.sonar.api.resources.DuplicatedSourceException;
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

  /**
   * Indexes a resource as a direct child of project. This method does nothing and returns true if the resource already indexed.
   *
   * @return false if the resource is excluded
   * @since 2.6
   */
  public abstract boolean index(Resource resource);


  /**
   * Indexes a resource. This method does nothing if the resource is already indexed.
   *
   * @param resource        the resource to index. Not nullable
   * @param parentReference a reference to the indexed parent. If null, the resource is indexed as a direct child of project.
   * @return false if the parent is not indexed or if the resource is excluded
   * @since 2.6
   */
  public abstract boolean index(Resource resource, Resource parentReference);

  /**
   * Returns true if the referenced resource is excluded. An excluded resource is not indexed.
   * @since 2.6
   */
  public abstract boolean isExcluded(Resource reference);

  /**
   * @since 2.6
   */
  public abstract boolean isIndexed(Resource reference);

  /**
   * Search for an indexed resource.
   *
   * @param reference the resource reference
   * @return the indexed resource, null if it's not indexed
   * @since 1.10. Generic types since 2.6.
   */
  public abstract <R extends Resource> R getResource(R reference);

  /**
   * @since 2.6
   */
  public abstract Resource getParent(Resource reference);

  /**
   * @since 2.6
   */

  public abstract Collection<Resource> getChildren(Resource reference);

  /**
   * Save the source code of a file. The file must be have been indexed before.
   * Note: the source stream is not closed.
   *
   * @return false if the resource is excluded or not indexed
   * @throws org.sonar.api.resources.DuplicatedSourceException
   *          if the source has already been set on this resource
   */
  public abstract boolean setSource(Resource reference, String source) throws DuplicatedSourceException;

  public abstract Project getProject();

  public final Collection<Resource> getResources() {
    return getVertices();
  }

  /**
   * Indexes the resource.
   * @return the indexed resource, even if it's excluded
   * @deprecated since 2.6. Use methods index()
   */
  @Deprecated
  public abstract Resource addResource(Resource resource);

  public abstract Measure getMeasure(Resource resource, Metric metric);

  public abstract <M> M getMeasures(Resource resource, MeasuresFilter<M> filter);

  /**
   * @since 2.5
   */
  public abstract void addViolation(Violation violation, boolean force);

  public final void addViolation(Violation violation) {
    addViolation(violation, false);
  }

  /**
   * Warning: the resource is automatically indexed for backward-compatibility, but it should be explictly
   * indexed before. Next versions will deactivate this automatic indexation.
   *
   * @throws SonarException if the metric is unknown.
   */
  public abstract Measure addMeasure(Resource resource, Measure measure);

  public abstract Dependency addDependency(Dependency dependency);

  public abstract Set<Dependency> getDependencies();

  public abstract void addLink(ProjectLink link);

  public abstract void deleteLink(String key);

  public abstract List<Event> getEvents(Resource resource);

  public abstract void deleteEvent(Event event);

  public abstract Event addEvent(Resource resource, String name, String description, String category, Date date);

  public final Collection<Dependency> getOutgoingDependencies(Resource from) {
    return getOutgoingEdges(from);
  }

  public final Collection<Dependency> getIncomingDependencies(Resource to) {
    return getIncomingEdges(to);
  }
}
