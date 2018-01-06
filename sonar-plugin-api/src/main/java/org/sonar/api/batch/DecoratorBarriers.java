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

/**
 * Barriers are used to define the order of execution of Decorators. Decorators must be annotated with the following :
 * <br>
 * <ul>
 * <li>{@code @DependsUpon(BARRIER)} in order to be executed after BARRIER</li>
 * <li>{@code @DependedUpon(BARRIER)} in order to be executed before BARRIER</li>
 * </ul>
 *
 * @since 2.3
 * @deprecated since 5.6 as {@link Decorator} is deprecated
 */
@Deprecated
public interface DecoratorBarriers {

  /**
   * This barrier is before {@link #ISSUES_TRACKED}. The decorators that register issues must be declared before this
   * barrier : {@code @DependedUpon(value=DecoratorBarriers.ISSUES_ADDED)}
   *
   * @since 3.6
   */
  String ISSUES_ADDED = "END_OF_VIOLATIONS_GENERATION";

  /**
   * This barrier is after {@link #ISSUES_ADDED}. The decorators that need to list all issues must be declared
   * after this barrier : {@code @DependsUpon(value=DecoratorBarriers.ISSUES_TRACKED)}
   *
   * @since 3.6
   */
  String ISSUES_TRACKED = "END_OF_VIOLATION_TRACKING";

  /**
   * @deprecated in 3.6. Not required anymore.
   */
  @Deprecated
  String START_VIOLATIONS_GENERATION = "START_VIOLATIONS_GENERATION";

  /**
   * This barrier is used by a decorator in order to :
   * <ul>
   * <li>be executed after all the decorators which generate violations :
   * {@code @DependsUpon(value=DecoratorBarriers.END_OF_VIOLATIONS_GENERATION}</li>
   * <li>declare that it generates violations : {@code @DependedUpon(value=DecoratorBarriers.END_OF_VIOLATIONS_GENERATION}</li>
   * </ul>
   *
   * @deprecated in 3.6. Replaced by {@link #ISSUES_ADDED}
   */
  @Deprecated
  String END_OF_VIOLATIONS_GENERATION = "END_OF_VIOLATIONS_GENERATION";

  /**
   * Extensions which call the method {@code Violation#setSwitchedOff} must be executed before this barrier
   * ({@code @DependedUpon(value=DecoratorBarriers.VIOLATION_TRACKING})
   * <br>
   * This barrier is after {@code END_OF_VIOLATIONS_GENERATION}
   *
   * @since 2.8
   * @deprecated in 3.6. Not required anymore.
   */
  @Deprecated
  String START_VIOLATION_TRACKING = "START_VIOLATION_TRACKING";

  /**
   * This barrier is after {@code END_OF_VIOLATIONS_GENERATION} and {@code START_VIOLATION_TRACKING}.
   * Decorators executed after this barrier ({@code @DependsUpon(value=DecoratorBarriers.END_OF_VIOLATION_TRACKING})
   * can benefit from all the features of violation tracking :
   * <ul>
   * <li>{@code Violation#getCreatedAt()}</li>
   * <li>{@code Violation#isSwitchedOff()}, usually to know if a violation has been flagged as false-positives in UI</li>
   * </ul>
   *
   * @since 2.8
   * @deprecated in 3.6. Replaced by {@link #ISSUES_TRACKED}
   */
  @Deprecated
  String END_OF_VIOLATION_TRACKING = "END_OF_VIOLATION_TRACKING";

  /**
   * @since 2.13
   * @deprecated in 3.6. Issues are persisted at the end of analysis.
   */
  @Deprecated
  String START_VIOLATION_PERSISTENCE = "START_VIOLATION_PERSISTENCE";

  /**
   * @since 2.13
   * @deprecated in 3.6. Issues are persisted at the end of analysis
   */
  @Deprecated
  String END_OF_VIOLATION_PERSISTENCE = "END_OF_VIOLATION_PERSISTENCE";

  /**
   * Any kinds of time machine data are calculated before this barrier. Decorators executed after this barrier can use
   * Measure#getVariationValue() method.
   *
   * @since 2.5
   */
  String END_OF_TIME_MACHINE = "END_OF_TIME_MACHINE";
}
