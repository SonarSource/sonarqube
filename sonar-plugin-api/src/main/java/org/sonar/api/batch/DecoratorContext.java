/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch;

import java.util.Collection;
import java.util.List;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/**
 * @since 1.10
 * @deprecated since 5.6 as {@link Decorator} is deprecated
 */
@Deprecated
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
  /**
   * @deprecated since 5.2 No more design features. No-op.
   */
  @Deprecated
  Dependency saveDependency(Dependency dependency);

}
