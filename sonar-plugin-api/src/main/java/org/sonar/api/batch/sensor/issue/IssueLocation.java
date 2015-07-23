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
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextRange;

/**
 * Represents an issue location.
 *
 * @since 5.2
 */
@Beta
public interface IssueLocation {

  /**
   * The {@link InputPath} this location belongs to. Returns null if location is global to the project.
   */
  @CheckForNull
  InputPath inputPath();

  /**
   * Range of the issue. Null for global issues and issues on directories. Can also be null
   * for files (issue global to the file).
   */
  @CheckForNull
  TextRange textRange();

  /**
   * Message of the issue.
   */
  @CheckForNull
  String message();

}
