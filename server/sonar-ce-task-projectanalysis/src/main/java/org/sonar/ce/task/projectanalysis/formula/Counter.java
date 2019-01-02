/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.formula;

import org.sonar.ce.task.projectanalysis.component.Component;

import static org.sonar.ce.task.projectanalysis.component.Component.Type;

/**
 * A counter is used to aggregate some data
 */
public interface Counter<T extends Counter<T>> {

  /**
   * This method is used on not leaf levels, to aggregate the value of the specified counter of a child to the counter
   * of the current component.
   */
  void aggregate(T counter);

  /**
   * This method is called on leaves of the Component tree (usually a {@link Component.Type#FILE} or a {@link Component.Type#PROJECT_VIEW}
   * but can also be a {@link Component.Type#SUBVIEW} or {@link Component.Type#VIEW}) to initialize the counter.
   */
  void initialize(CounterInitializationContext context);

}
