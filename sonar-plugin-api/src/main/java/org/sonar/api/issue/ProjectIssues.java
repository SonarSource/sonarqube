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
package org.sonar.api.issue;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.InstantiationStrategy;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;

/**
 * Used by batch components to get all project issues.
 *
 * @since 4.0
 */
@InstantiationStrategy(PER_BATCH)
@ScannerSide
public interface ProjectIssues {

  /**
   * All the unresolved issues of the project, including the issues reported by end-users.
   */
  Iterable<Issue> issues();

  /**
   * All the issues of this project that have been marked as resolved during this scan
   */
  Iterable<Issue> resolvedIssues();
}
