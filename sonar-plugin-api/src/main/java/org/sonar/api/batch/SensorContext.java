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
package org.sonar.api.batch;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @since 1.10
 */
public interface SensorContext {

  /**
   * Indexes a resource as a direct child of project. This method does nothing and returns true if the resource already indexed.
   *
   * @return false if the resource is excluded
   * @deprecated since 4.2 Resource indexing is done by the platform for all physical resources.
   */
  @Deprecated
  boolean index(Resource resource);

  /**
   * Indexes a resource. This method does nothing if the resource is already indexed.
   *
   * @param resource        the resource to index. Not nullable
   * @param parentReference a reference to the parent. If null, the the resource is indexed as a direct child of project.
   * @return false if the parent is not indexed or if the resource is excluded
   * @deprecated since 4.2 Resource indexing is done by the platform for all physical resources.
   */
  @Deprecated
  boolean index(Resource resource, Resource parentReference);

  /**
   * Returns true if the referenced resource is indexed and excluded.
   *
   * @since 2.6
   * @deprecated since 4.2 Excluded resources are not indexed.
   */
  @Deprecated
  boolean isExcluded(Resource reference);

  /**
   * Returns true if the referenced resource is indexed.
   *
   * @since 2.6
   * @deprecated since 4.2 Excluded resources are not indexed.
   */
  @Deprecated
  boolean isIndexed(Resource reference, boolean acceptExcluded);

  /**
   * Search for an indexed resource.
   *
   * @param reference the resource reference
   * @return the indexed resource, null if it's not indexed
   * @since 1.10. Generic types since 2.6.
   */
  @CheckForNull
  <R extends Resource> R getResource(R reference);

  /**
   * @since 2.6
   */
  Resource getParent(Resource reference);

  /**
   * @since 2.6
   */

  Collection<Resource> getChildren(Resource reference);

  // ----------- MEASURES ON PROJECT --------------

  /**
   * Find a project measure
   */
  Measure getMeasure(Metric metric);

  /**
   * All measures of the project. Never return null.
   */
  <M> M getMeasures(MeasuresFilter<M> filter);

  /**
   * Add a measure on project
   */
  Measure saveMeasure(Measure measure);

  /**
   * Add a measure on project
   */
  Measure saveMeasure(Metric metric, Double value);

  // ----------- MEASURES ON RESOURCES --------------

  /**
   * Find a measure for this project
   */
  Measure getMeasure(Resource resource, Metric metric);

  /**
   * Key is updated when saving the resource.
   *
   * @return the key as saved in database. Null if the resource is set as excluded.
   * @deprecated use the methods index()
   */
  @Deprecated
  String saveResource(Resource resource);

  /**
   * Find all measures for this project. Never return null.
   */
  <M> M getMeasures(Resource resource, MeasuresFilter<M> filter);

  /**
   * Add or update a measure.
   * <p>
   * The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as
   * excluded.
   * </p>
   */
  Measure saveMeasure(Resource resource, Metric metric, Double value);

  /**
   * Add or update a measure.
   * <p>
   * The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as
   * excluded.
   * </p>
   */
  Measure saveMeasure(Resource resource, Measure measure);

  // ----------- RULE VIOLATIONS --------------

  /**
   * Save a coding rule violation.
   *
   * @param force allows to force creation of violation even if it was supressed by {@link org.sonar.api.rules.ViolationFilter}
   * @since 2.5
   */
  void saveViolation(Violation violation, boolean force);

  /**
   * Save a coding rule violation.
   */
  void saveViolation(Violation violation);

  /**
   * Saves a list of violations.
   */
  void saveViolations(Collection<Violation> violations);

  // ----------- DEPENDENCIES BETWEEN RESOURCES --------------

  /**
   * Build a new dependency : from depends upon to. The dependency is NOT saved. The method saveDependency() must still be executed.
   */
  Dependency saveDependency(Dependency dependency);

  Set<Dependency> getDependencies();

  Collection<Dependency> getIncomingDependencies(Resource to);

  Collection<Dependency> getOutgoingDependencies(Resource from);

  // ----------- FILE SOURCES --------------

  /**
   * Save the source code of a file. The file must be have been indexed before.
   *
   * @throws org.sonar.api.resources.DuplicatedSourceException if the source has already been set on this resource
   * @since 1.10. Returns a boolean since 2.6.
   * @deprecated since 4.2 Source import is done by the platform
   */
  @Deprecated
  void saveSource(Resource reference, String source);

  // ----------- LINKS --------------

  /**
   * add a link to an external page like project homepage, sources (subversion, ...), continuous integration server... Example :
   * context.addLink(new ProjectLink("maven_site, "Maven site", "http://my.maven.com)
   */
  void saveLink(ProjectLink link);

  /**
   * remove a link. It does not fail if key is unknown.
   */
  void deleteLink(String key);

  // ----------- EVENTS --------------

  /**
   * @param resource set null for project events
   */
  List<Event> getEvents(Resource resource);

  /**
   * Creates an event for a given date
   *
   * @param name        the event name
   * @param description the event description
   * @param category    the event category
   * @param date        the event date
   * @return the created event
   */
  Event createEvent(Resource resource, String name, String description, String category, Date date);

  /**
   * Deletes an event
   *
   * @param event the event to delete
   */
  void deleteEvent(Event event);

  /**
   * Experimental
   * @since 4.2
   */
  Measure saveMeasure(InputFile inputFile, Metric metric, Double value);

  /**
   * Experimental
   * @since 4.2
   */
  Measure saveMeasure(InputFile inputFile, Measure measure);
}
