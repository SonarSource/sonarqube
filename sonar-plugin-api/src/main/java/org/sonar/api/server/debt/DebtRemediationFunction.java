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
 * Function used to calculate the remediation cost of an issue. See {@link Type} for details.
 * <p>The coefficient and offset involved in the functions are durations. They are defined in hours, minutes and/or
 * seconds. Examples: "5min", "1h 10min". Supported units are "d" (days), "h" (hour), and "min" (minutes).</p>
 *
 * @since 4.3
 */
public interface DebtRemediationFunction {

  enum Type {

    /**
     * The cost to fix an issue of this type depends on the magnitude of the issue.
     * For instance, an issue related to file size might be linear, with the total cost-to-fix incrementing
     * (by the coefficient amount) for each line of code above the allowed threshold.
     * The rule must provide the "effort to fix" value when raising an issue.
     */
    LINEAR(true, false),

    /**
     * It takes a certain amount of time to deal with an issue of this type (this is the offset).
     * Then, the magnitude of the issue comes in to play. For instance, an issue related to complexity might be linear with offset.
     * So the total cost to fix is the time to make the basic analysis (the offset) plus the time required to deal
     * with each complexity point above the allowed value.
     * <p>
     * <code>Total remediation cost = offset + (number of noncompliance x coefficient)</code>
     * </p>
     * <p>The rule must provide the "effort to fix" value when raising an issue. Let’s take as a example the “Paragraphs should not be too complex” rule.
     * If you set the rule threshold to 20, and you have a paragraph with a complexity of 27, you have 7 points of complexity
     * to remove. Internally, this is called the Effort to Fix. In that case, if you use the LINEAR_OFFSET configuration
     * with an offset of 4h and a remediation cost of 1mn, the technical debt for this issue related to a
     * too-complex block of code will be: (7 complexity points x 1min) + 4h = 4h and 7mn
     * </p>
     */
    LINEAR_OFFSET(true, true),

    /**
     * The cost to fix all the issues of the rule is the same whatever the number of issues
     * of this rule in the file. Total remediation cost by file = constant
     */
    CONSTANT_ISSUE(false, true);

    private final boolean usesCoefficient;
    private final boolean usesOffset;

    Type(boolean usesCoefficient, boolean usesOffset) {
      this.usesCoefficient = usesCoefficient;
      this.usesOffset = usesOffset;
    }

    public boolean usesCoefficient() {
      return usesCoefficient;
    }

    public boolean usesOffset() {
      return usesOffset;
    }
  }

  Type type();

  /**
   * Non-null value on {@link Type#LINEAR} and {@link Type#LINEAR_OFFSET} functions, else {@code null}.
   */
  @CheckForNull
  String coefficient();

  /**
   * Non-null value on {@link Type#LINEAR_OFFSET} and {@link Type#CONSTANT_ISSUE} functions, else {@code null}.
   */
  @CheckForNull
  String offset();

}
