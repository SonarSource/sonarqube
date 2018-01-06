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
package org.sonar.api.batch.sensor.error;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.sensor.Sensor;

/**
 * Represents an analysis error, such as a parsing error, that occurs during the execution of a {@link Sensor}.
 * Multiple analysis errors might be reported for the same file and issues may be created for a file which also has analysis errors reported. 
 * The handling of such situations is unspecified and might vary depending on the implementation.
 * 
 * @since 6.0
 */
public interface NewAnalysisError {
  /**
   * The file that was being processed when the error occurred. This field must be set before saving the error.
   */
  NewAnalysisError onFile(InputFile inputFile);

  /**
   * Message about the error. This field is optional.
   */
  NewAnalysisError message(String message);

  /**
   * Location of this error. This field is optional.
   */
  NewAnalysisError at(TextPointer location);

  /**
   * Save this error information.
   */
  void save();
}
