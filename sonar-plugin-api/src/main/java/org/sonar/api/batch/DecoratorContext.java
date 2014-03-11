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
package org.sonar.api.batch;

import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @since 1.10
 */
public interface DecoratorContext {

  /**
   * @return the project in which the decorator is
   */
  Project getProject();

  /**
   * @return the resource that is currently decorated
   */
  Resource getResource();

  /**
   * Child contexts are read only
   */
  List<DecoratorContext> getChildren();

  // MEASURES

  /**
   * Find a measure for the resource
   */
  Measure getMeasure(Metric metric);

  /**
   * Never return null.
   */
  <M> M getMeasures(MeasuresFilter<M> filter);

  /**
   * Never return null.
   */
  Collection<Measure> getChildrenMeasures(MeasuresFilter filter);

  /**
   * @return the resource children measures for the given metric
   */
  Collection<Measure> getChildrenMeasures(Metric metric);

  /**
   * Add a measure on the current resource. It can not be executed from children contexts.
   * 
   * @return the same context
   */
  DecoratorContext saveMeasure(Measure measure);

  /**
   * Add a measure on the current resource. It can not be executed from children contexts.
   * 
   * @return the current object
   */
  DecoratorContext saveMeasure(Metric metric, Double value);

  // DEPENDENCIES

  Dependency saveDependency(Dependency dependency);

  Set<Dependency> getDependencies();

  Collection<Dependency> getIncomingDependencies();

  Collection<Dependency> getOutgoingDependencies();

  // RULES

  /**
   * Returns the violations that match the {@link ViolationQuery} parameters.
   * 
   * @since 2.8
   * @param violationQuery
   *          the request parameters specified as a {@link ViolationQuery}
   * @return the list of violations that match those parameters
   */
  List<Violation> getViolations(ViolationQuery violationQuery);

  /**
   * Returns all the active (= non switched-off) violations found on the current resource.
   * 
   * @return the list of violations
   */
  List<Violation> getViolations();

  /**
   * Save a coding rule violation. The decorator which calls this method must be depended upon BatchBarriers.END_OF_VIOLATIONS_GENERATION.
   * 
   * @since 2.5
   * @param force allows to force creation of violation even if it was suppressed by {@link org.sonar.api.rules.ViolationFilter}
   */
  DecoratorContext saveViolation(Violation violation, boolean force);

  /**
   * Save a coding rule violation. The decorator which calls this method must be depended upon BatchBarriers.END_OF_VIOLATIONS_GENERATION.
   */
  DecoratorContext saveViolation(Violation violation);

  // EVENTS

  /**
   * @return the list of events associated to the current resource
   */
  List<Event> getEvents();

  /**
   * Creates an event for a given date
   * 
   * @param name the event name
   * @param description the event description
   * @param category the event category
   * @param date the event date
   * @return the created event
   */
  Event createEvent(String name, String description, String category, Date date);

  /**
   * Deletes an event
   * 
   * @param event the event to delete
   */
  void deleteEvent(Event event);

}
