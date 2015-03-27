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

import java.util.Collection;
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
   * Add a new measure on the current resource. It can not be executed from children contexts.
   * 
   * @return the same context
   */
  DecoratorContext saveMeasure(Measure measure);

  /**
   * Add a new measure on the current resource. It can not be executed from children contexts.
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
   * Save a coding rule violation. The decorator which calls this method must be depended upon BatchBarriers.END_OF_VIOLATIONS_GENERATION.
   * 
   * @since 2.5
   * @param force allows to force creation of violation even if it was suppressed by {@link org.sonar.api.rules.ViolationFilter}
   * @deprecated in 3.6, replaced by {@link org.sonar.api.issue.Issuable}
   */
  @Deprecated
  DecoratorContext saveViolation(Violation violation, boolean force);

  /**
   * Save a coding rule violation. The decorator which calls this method must be depended upon BatchBarriers.END_OF_VIOLATIONS_GENERATION.
   * @deprecated in 3.6, replaced by {@link org.sonar.api.issue.Issuable}
   */
  @Deprecated
  DecoratorContext saveViolation(Violation violation);

}
