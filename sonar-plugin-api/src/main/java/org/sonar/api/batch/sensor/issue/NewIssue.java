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
package org.sonar.api.batch.sensor.issue;

import com.google.common.annotations.Beta;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rule.RuleKey;

/**
 * Represents an issue detected by a {@link Sensor}.
 *
 * @since 5.1
 */
@Beta
public interface NewIssue {

  /**
   * The {@link RuleKey} of the issue.
   */
  NewIssue forRule(RuleKey ruleKey);

  /**
   * Effort to fix the issue.
   */
  NewIssue effortToFix(@Nullable Double effortToFix);

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
   * Register an additional location for this issue.
   * @since 5.2
   */
  NewIssue addLocation(NewIssueLocation location);

  /**
   * Register an execution flow for this issue. An execution flow is an ordered list of issue locations that help to understand the issue.
   * It is usually the path leading to the primary location. Several execution flows can be registered.
   * @since 5.2
   */
  NewIssue addExecutionFlow(Iterable<NewIssueLocation> flowLocations);

  /**
   * Create a new location for this issue. First registered location is considered as primary location.
   * @since 5.2
   */
  NewIssueLocation newLocation();

  /**
   * Attach a new attribute to the issue. Not used by SQ but can be reused later for integration needs (for example it is returned by WS).
   * @since 5.2
   */
  NewIssue addAttribute(String key, String value);

  /**
   * Save the issue. If rule key is unknown or rule not enabled in the current quality profile then a warning is logged but no exception
   * is thrown.
   */
  void save();

}
