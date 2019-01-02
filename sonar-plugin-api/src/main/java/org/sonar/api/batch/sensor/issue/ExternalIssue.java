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
package org.sonar.api.batch.sensor.issue;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.rules.RuleType;

/**
 * Represents an issue imported from an external rule engine by a {@link Sensor}.
 * @since 7.2
 */
public interface ExternalIssue extends IIssue {

  /**
   * @since 7.4
   */
  String engineId();

  /**
   * @since 7.4
   */
  String ruleId();

  Severity severity();

  /**
   * Effort to fix the issue, in minutes.
   */
  @CheckForNull
  Long remediationEffort();

  /**
   * Type of the issue.
   */
  RuleType type();

}
