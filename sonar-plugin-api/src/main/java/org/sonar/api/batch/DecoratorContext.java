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
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

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


  Dependency saveDependency(Dependency dependency);

  Set<Dependency> getDependencies();

  Collection<Dependency> getIncomingDependencies();

  Collection<Dependency> getOutgoingDependencies();


  // RULES

  /**
   * Read-only rule failures.
   *
   * @return the rule failures for file/classes resources, null for the others
   */
  List<Violation> getViolations();


  /**
   * Save a coding rule violation. The decorator which calls this method must be depended upon BatchBarriers.END_OF_VIOLATIONS_GENERATION.
   * @see org.sonar.api.batch.BatchBarriers
   */
  DecoratorContext saveViolation(Violation violation);

  /**
   * @return the list of events associated to the current resource
   */
  List<Event> getEvents();

  /**
   * Creates an event for a given date
   *
   * @param name        the event name
   * @param description the event description
   * @param category    the event category
   * @param date        the event date
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
