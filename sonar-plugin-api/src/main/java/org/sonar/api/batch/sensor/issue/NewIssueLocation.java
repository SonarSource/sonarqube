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

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

/**
 * Represents one issue location. See {@link NewIssue#newLocation()}.
 *
 * @since 5.2
 */
public interface NewIssueLocation {

  /**
   * Maximum number of characters in the message.
   */
  int MESSAGE_MAX_SIZE = 4000;

  /**
   * The {@link InputComponent} the issue location belongs to. Mandatory.
   */
  NewIssueLocation on(InputComponent component);

  /**
   * Position in the file. Only applicable when {@link #on(InputComponent)} has been called with an InputFile. 
   * See {@link InputFile#newRange(org.sonar.api.batch.fs.TextPointer, org.sonar.api.batch.fs.TextPointer)}
   */
  NewIssueLocation at(TextRange location);

  /**
   * Optional, but recommended, plain-text message for this location.
   * <br>
   * Formats like Markdown or HTML are not supported. Size must not be greater than {@link #MESSAGE_MAX_SIZE} characters.
   */
  NewIssueLocation message(String message);

}
