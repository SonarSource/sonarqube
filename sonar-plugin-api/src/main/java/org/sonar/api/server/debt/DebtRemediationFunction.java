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
package org.sonar.api.server.debt;

import javax.annotation.CheckForNull;

/**
 * Function used to calculate the remediation cost of an issue. See {@link Type} for details.
 * <p>The gap multiplier and base effort involved in the functions are durations. They are defined in hours, minutes and/or
 * seconds. Examples: "5min", "1h 10min". Supported units are "d" (days), "h" (hour), and "min" (minutes).
 *
 * @since 4.3
 */
public interface DebtRemediationFunction {

  enum Type {

    /**
     * The cost to fix an issue of this type depends on the magnitude of the issue.
     * For instance, an issue related to file size might be linear, with the total cost-to-fix incrementing
     * (by the gap multiplier amount) for each line of code above the allowed threshold.
     * The rule must provide the "gap" value when raising an issue.
     */
    LINEAR(true, false),

    /**
     * It takes a certain amount of time to deal with an issue of this type (this is the gap multiplier).
     * Then, the magnitude of the issue comes in to play. For instance, an issue related to complexity might be linear with offset.
     * So the total cost to fix is the time to make the basic analysis (the base effort) plus the time required to deal
     * with each complexity point above the allowed value.
     * <p>
     * <code>Total remediation cost = base effort + (number of noncompliance x gap multiplier)</code>
     * 
     * <p>The rule must provide the "gap" value when raising an issue. Let's take as a example the "Paragraphs should not be too complex" rule.
     * If you set the rule threshold to 20, and you have a paragraph with a complexity of 27, you have 7 points of complexity
     * to remove. Internally, this is called the Gap. In that case, if you use the LINEAR_OFFSET configuration
     * with an base effort of 4h and a remediation cost of 1mn, the effort for this issue related to a
     * too-complex block of code will be: (7 complexity points x 1min) + 4h = 4h and 7mn
     * 
     */
    LINEAR_OFFSET(true, true),

    /**
     * The cost to fix all the issues of the rule is the same whatever the number of issues
     * of this rule in the file. Total remediation cost by file = constant
     */
    CONSTANT_ISSUE(false, true);

    private final boolean usesGapMultiplier;
    private final boolean usesBaseEffort;

    Type(boolean usesGapMultiplier, boolean usesBaseEffort) {
      this.usesGapMultiplier = usesGapMultiplier;
      this.usesBaseEffort = usesBaseEffort;
    }

    /**
     * @deprecated since 5.5, replaced by {@link #usesGapMultiplier()}
     */
    @Deprecated
    public boolean usesCoefficient() {
      return usesGapMultiplier();
    }

    /**
     * @since 5.5
     */
    public boolean usesGapMultiplier() {
      return usesGapMultiplier;
    }

    /**
     * @deprecated since 5.5, replaced by {@link #usesBaseEffort()}
     */
    @Deprecated
    public boolean usesOffset() {
      return usesBaseEffort();
    }

    /**
     * @since 5.5
     */
    public boolean usesBaseEffort() {
      return usesBaseEffort;
    }

  }

  /**
   * @since 5.5
   */
  Type type();

  /**
   * @deprecated since 5.5, replaced by {@link #gapMultiplier()}
   */
  @Deprecated
  @CheckForNull
  String coefficient();

  /**
   * Non-null value on {@link Type#LINEAR} and {@link Type#LINEAR_OFFSET} functions, else {@code null}.
   *
   * @since 5.5
   */
  @CheckForNull
  String gapMultiplier();

  /**
   * @deprecated since 5.5, replaced by {@link #baseEffort()}
   */
  @Deprecated
  @CheckForNull
  String offset();

  /**
   * Non-null value on {@link Type#LINEAR_OFFSET} and {@link Type#CONSTANT_ISSUE} functions, else {@code null}.
   *
   * @since 5.5
   */
  @CheckForNull
  String baseEffort();

}
