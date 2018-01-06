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
package org.sonar.server.component.ws;

import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;

public class MeasuresWsParameters {
  public static final String CONTROLLER_MEASURES = "api/measures";

  // actions
  public static final String ACTION_COMPONENT_TREE = "component_tree";
  public static final String ACTION_COMPONENT = "component";
  public static final String ACTION_SEARCH_HISTORY = "search_history";

  // parameters
  public static final String DEPRECATED_PARAM_BASE_COMPONENT_ID = "baseComponentId";
  public static final String DEPRECATED_PARAM_BASE_COMPONENT_KEY = "baseComponentKey";
  public static final String PARAM_COMPONENT = "component";
  public static final String PARAM_BRANCH = "branch";
  public static final String PARAM_STRATEGY = "strategy";
  public static final String PARAM_QUALIFIERS = "qualifiers";
  public static final String PARAM_METRICS = "metrics";
  public static final String PARAM_METRIC_KEYS = "metricKeys";
  public static final String PARAM_METRIC_SORT = "metricSort";
  public static final String PARAM_METRIC_PERIOD_SORT = "metricPeriodSort";
  public static final String PARAM_METRIC_SORT_FILTER = "metricSortFilter";
  public static final String PARAM_ADDITIONAL_FIELDS = "additionalFields";
  public static final String DEPRECATED_PARAM_COMPONENT_ID = "componentId";
  public static final String DEPRECATED_PARAM_COMPONENT_KEY = "componentKey";
  public static final String PARAM_PROJECT_KEYS = "projectKeys";
  public static final String PARAM_DEVELOPER_ID = "developerId";
  public static final String PARAM_DEVELOPER_KEY = "developerKey";
  public static final String PARAM_FROM = "from";
  public static final String PARAM_TO = "to";

  public static final String ADDITIONAL_METRICS = "metrics";
  public static final String ADDITIONAL_PERIODS = "periods";

  public static final Set<String> ADDITIONAL_FIELDS = ImmutableSortedSet.of(ADDITIONAL_METRICS, ADDITIONAL_PERIODS);

  private MeasuresWsParameters() {
    // static constants only
  }
}
