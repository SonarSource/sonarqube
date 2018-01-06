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
package org.sonar.api.batch.sensor.issue;

import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rule.RuleKey;

/**
 * Represents an issue detected by a {@link Sensor}.
 *
 * @since 5.1
 */
public interface NewIssue {

  /**
   * The {@link RuleKey} of the issue.
   */
  NewIssue forRule(RuleKey ruleKey);

  /**
   * Effort to fix the issue.
   * @deprecated since 5.5 use {@link #gap(Double)}
   */
  @Deprecated
  NewIssue effortToFix(@Nullable Double effortToFix);

  /**
   * Gap used for the computation of the effort. 
   * @since 5.5
   */
  NewIssue gap(@Nullable Double gap);

  /**
   * Override severity of the issue.
   * Setting a null value or not calling this method means to use severity configured in quality profile.
   */
  NewIssue overrideSeverity(@Nullable Severity severity);

  /**
   * Primary location for this issue.
   * @since 5.2
   */
  NewIssue at(NewIssueLocation primaryLocation);

  /**
   * Add a secondary location for this issue. Several secondary locations can be registered.
   * @since 5.2
   */
  NewIssue addLocation(NewIssueLocation secondaryLocation);

  /**
   * Register a flow for this issue. A flow is an ordered list of issue locations that help to understand the issue.
   * It should be a <b>path that backtracks the issue from its primary location to the start of the flow</b>. 
   * Several flows can be registered.
   * @since 5.2
   */
  NewIssue addFlow(Iterable<NewIssueLocation> flowLocations);

  /**
   * Create a new location for this issue. First registered location is considered as primary location.
   * @since 5.2
   */
  NewIssueLocation newLocation();

  /**
   * Save the issue. If rule key is unknown or rule not enabled in the current quality profile then a warning is logged but no exception
   * is thrown.
   */
  void save();

}
