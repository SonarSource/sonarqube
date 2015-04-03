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

package org.sonar.api.server.debt;

import javax.annotation.CheckForNull;

/**
 * Function used to calculate the remediation cost of an issue. There are three types :
 * <ul>
 * <li>
 * <b>Linear</b> - Each issue of the rule costs the same amount of time (coefficient) to fix.
 * </li>
 * <li>
 * <b>Linear with offset</b> - It takes a certain amount of time to analyze the issues of such kind on the file (offset).
 * Then, each issue of the rule costs the same amount of time (coefficient) to fix. Total remediation cost
 * by file = offset + (number of issues x coefficient)
 * </li>
 * <li><b>Constant/issue</b> - The cost to fix all the issues of the rule is the same whatever the number of issues
 * of this rule in the file. Total remediation cost by file = constant
 * </li>
 * </ul>
 *
 * @since 4.3
 */
public interface DebtRemediationFunction {

  enum Type {
    LINEAR, LINEAR_OFFSET, CONSTANT_ISSUE
  }

  Type type();

  /**
   * Factor is set on types {@link Type#LINEAR} and {@link Type#LINEAR_OFFSET}, else it's null.
   */
  @CheckForNull
  String coefficient();

  /**
   * Offset is set on types {@link Type#LINEAR_OFFSET} and {@link Type#CONSTANT_ISSUE}, else it's null.
   */
  @CheckForNull
  String offset();

}
