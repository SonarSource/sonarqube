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
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @since 1.10
 */
public interface SensorContext {

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
   */
  String saveResource(Resource resource);

  /**
   * @return the resource saved in sonar index
   */
  Resource getResource(Resource resource);

  /**
   * Find all measures for this project. Never return null.
   */
  <M> M getMeasures(Resource resource, MeasuresFilter<M> filter);

  /**
   * Add or update a measure.
   * <p/>
   * <p>The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as excluded.</p>
   */
  Measure saveMeasure(Resource resource, Metric metric, Double value);

  /**
   * Add or update a measure.
   * <p/>
   * <p>The resource is automatically saved, so there is no need to execute the method saveResource(). Does nothing if the resource is set as excluded.</p>
   */
  Measure saveMeasure(Resource resource, Measure measure);


  // ----------- RULE VIOLATIONS --------------

  /**
   * Save a coding rule violation.
   */
  void saveViolation(Violation violation);

  /**
   * Saves a list of violations
   */
  void saveViolations(Collection<Violation> violations);


  // ----------- DEPENDENCIES BETWEEN RESOURCES  --------------

  /**
   * Build a new dependency : from depends upon to. The dependency is NOT saved. The method saveDependency() must still be executed.
   */
  Dependency saveDependency(Dependency dependency);

  Set<Dependency> getDependencies();

  Collection<Dependency> getIncomingDependencies(Resource to);

  Collection<Dependency> getOutgoingDependencies(Resource from);

  // ----------- FILE SOURCES  --------------

  /**
   * Does nothing if the resource is set as excluded.
   */
  void saveSource(Resource resource, String source);


  // ----------- LINKS --------------

  /**
   * add a link to an external page like project homepage, sources (subversion, ...), continuous integration server...
   * Example : context.addLink(new ProjectLink("maven_site, "Maven site", "http://my.maven.com)
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

}
