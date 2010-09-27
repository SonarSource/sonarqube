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

/**
 * Barriers are used to define the order of execution of Decorators.
 * @since 2.3
 */
public interface DecoratorBarriers {

  
  String START_VIOLATIONS_GENERATION = "START_VIOLATIONS_GENERATION";

  /**
   * This barrier is used by a decorator in order to :
   * <ul>
   *   <li>be executed after all the decorators which generate violations : <code>@DependsUpon(value=DecoratorBarriers.END_OF_VIOLATIONS_GENERATION</code></li>
   *   <li>declare that it generates violations : <code>@DependedUpon(value=DecoratorBarriers.END_OF_VIOLATIONS_GENERATION</code></li>
   * </ul>
   */
  String END_OF_VIOLATIONS_GENERATION = "END_OF_VIOLATIONS_GENERATION";
  
}
