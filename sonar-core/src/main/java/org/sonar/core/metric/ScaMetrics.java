/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.core.metric;

import java.util.Map;
import java.util.Set;

/**
 * Normally SCA metrics are only available when using Enterprise Edition or up
 * and when the appropriate license has been purchased.
 * However, we need to list these metrics out in the Measures API documentation,
 * so we have them defined in this core module and use them throughout the
 * non-SCA-specific areas of SonarQube.
 */
public class ScaMetrics {
  /**
   * These string values of SCA severity metric threshold values are 1 less numerically than
   * what's returned in measures, in order to support greater-than-or-equal comparisons.
   */
  public static final Map<String, String> SCA_SEVERITY_SETTING_VALUE_TO_TEXT = Map.of(
    "4", "info",
    "9", "low",
    "14", "medium",
    "19", "high",
    "24", "blocker");

  // These must be kept in sync with SCA metrics!
  // There's currently no way to do this in an automated fashion.
  private static final String SCA_SEVERITY_PREFIX = "sca_severity";
  private static final String SCA_COUNT_PREFIX = "sca_count";
  private static final String SCA_RATING_PREFIX = "sca_rating";
  private static final String SCA_ISSUE_TYPE_ANY_ISSUE = "any_issue";
  private static final String SCA_ISSUE_TYPE_VULNERABILITY = "vulnerability";
  private static final String SCA_ISSUE_TYPE_LICENSING = "licensing";
  private static final String BASE_KEY_FORMAT = "%s_%s";
  private static final String NEW_KEY_FORMAT = "new_" + BASE_KEY_FORMAT;
  public static final String NEW_SCA_RATING_LICENSING_KEY = String.format(NEW_KEY_FORMAT, SCA_RATING_PREFIX, SCA_ISSUE_TYPE_LICENSING);
  public static final String NEW_SCA_RATING_VULNERABILITY_KEY = String.format(NEW_KEY_FORMAT, SCA_RATING_PREFIX, SCA_ISSUE_TYPE_VULNERABILITY);
  public static final String NEW_SCA_RATING_ANY_ISSUE_KEY = String.format(NEW_KEY_FORMAT, SCA_RATING_PREFIX, SCA_ISSUE_TYPE_ANY_ISSUE);
  public static final String NEW_SCA_SEVERITY_LICENSING_KEY = String.format(NEW_KEY_FORMAT, SCA_SEVERITY_PREFIX, SCA_ISSUE_TYPE_LICENSING);
  public static final String NEW_SCA_SEVERITY_VULNERABILITY_KEY = String.format(NEW_KEY_FORMAT, SCA_SEVERITY_PREFIX, SCA_ISSUE_TYPE_VULNERABILITY);
  public static final String NEW_SCA_SEVERITY_ANY_ISSUE_KEY = String.format(NEW_KEY_FORMAT, SCA_SEVERITY_PREFIX, SCA_ISSUE_TYPE_ANY_ISSUE);
  public static final String NEW_SCA_COUNT_ANY_ISSUE_KEY = String.format(NEW_KEY_FORMAT, SCA_COUNT_PREFIX, SCA_ISSUE_TYPE_ANY_ISSUE);
  public static final String SCA_RATING_LICENSING_KEY = String.format(BASE_KEY_FORMAT, SCA_RATING_PREFIX, SCA_ISSUE_TYPE_LICENSING);
  public static final String SCA_RATING_VULNERABILITY_KEY = String.format(BASE_KEY_FORMAT, SCA_RATING_PREFIX, SCA_ISSUE_TYPE_VULNERABILITY);
  public static final String SCA_RATING_ANY_ISSUE_KEY = String.format(BASE_KEY_FORMAT, SCA_RATING_PREFIX, SCA_ISSUE_TYPE_ANY_ISSUE);
  public static final String SCA_SEVERITY_LICENSING_KEY = String.format(BASE_KEY_FORMAT, SCA_SEVERITY_PREFIX, SCA_ISSUE_TYPE_LICENSING);
  public static final String SCA_SEVERITY_VULNERABILITY_KEY = String.format(BASE_KEY_FORMAT, SCA_SEVERITY_PREFIX, SCA_ISSUE_TYPE_VULNERABILITY);
  public static final String SCA_SEVERITY_ANY_ISSUE_KEY = String.format(BASE_KEY_FORMAT, SCA_SEVERITY_PREFIX, SCA_ISSUE_TYPE_ANY_ISSUE);
  public static final String SCA_COUNT_ANY_ISSUE_KEY = String.format(BASE_KEY_FORMAT, SCA_COUNT_PREFIX, SCA_ISSUE_TYPE_ANY_ISSUE);
  public static final Set<String> SCA_METRICS_KEYS = Set.of(
    NEW_SCA_COUNT_ANY_ISSUE_KEY,
    NEW_SCA_SEVERITY_ANY_ISSUE_KEY,
    NEW_SCA_SEVERITY_VULNERABILITY_KEY,
    NEW_SCA_SEVERITY_LICENSING_KEY,
    NEW_SCA_RATING_ANY_ISSUE_KEY,
    NEW_SCA_RATING_VULNERABILITY_KEY,
    NEW_SCA_RATING_LICENSING_KEY,
    SCA_COUNT_ANY_ISSUE_KEY,
    SCA_SEVERITY_ANY_ISSUE_KEY,
    SCA_SEVERITY_VULNERABILITY_KEY,
    SCA_SEVERITY_LICENSING_KEY,
    SCA_RATING_ANY_ISSUE_KEY,
    SCA_RATING_VULNERABILITY_KEY,
    SCA_RATING_LICENSING_KEY);

  private ScaMetrics() {
    // constants only
  }
}
