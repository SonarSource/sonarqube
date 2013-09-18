/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.issue;

import org.sonar.api.BatchComponent;

/**
 * Used by batch components to get the issues of the current module. It does not allow
 * to get issues of all project modules.
 *
 * @since 4.0
 */
public interface ModuleIssues extends BatchComponent {

  /**
   * All the unresolved issues of the current module, including the issues reported by end-users.
   */
  Iterable<Issue> issues();

  /**
   * All the issues of this module that have been marked as resolved during this scan
   */
  Iterable<Issue> resolvedIssues();
}
