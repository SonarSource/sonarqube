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

import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;

/**
 * Represents an issue detected by a {@link Sensor}.
 *
 * @since 5.1
 */
public interface Issue extends IIssue {
  interface Flow {
    /**
     * @return Ordered list of locations for the execution flow
     */
    List<IssueLocation> locations();
  }

  /**
   * Gap used to compute the effort for fixing the issue.
   * @since 5.5
   */
  @CheckForNull
  Double gap();

  /**
   * Overridden severity.
   */
  @CheckForNull
  Severity overriddenSeverity();

  /**
   * Primary locations for this issue.
   * @since 5.2
   */
  @Override
  IssueLocation primaryLocation();

  /**
   * List of flows for this issue. Can be empty.
   * @since 5.2
   */
  @Override
  List<Flow> flows();
}
