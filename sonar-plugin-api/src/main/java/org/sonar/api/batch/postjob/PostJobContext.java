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
package org.sonar.api.batch.postjob;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;

/**
 * See {@link PostJob#execute(PostJobContext)}
 * @since 5.2
 */
public interface PostJobContext {

  /**
   * @deprecated since 6.5 use {@link #config()}
   */
  @Deprecated
  Settings settings();

  /**
   * Get configuration of the current project.
   * @since 6.5
   */
  Configuration config();

  AnalysisMode analysisMode();

  // ----------- Only available in preview mode --------------

  /**
   * All the unresolved issues of the project, including the issues reported by end-users. Only available in preview/issues mode.
   * @throw {@link UnsupportedOperationException} if not in preview/issues mode. To test the mode you can use {@link #analysisMode()}.
   */
  Iterable<PostJobIssue> issues();

  /**
   * All the issues of this project that have been marked as resolved during this scan. Only available in preview/issues mode.
   * @throw {@link UnsupportedOperationException} if not in preview mode. To test the mode you can use {@link #analysisMode()}.
   */
  Iterable<PostJobIssue> resolvedIssues();

}
