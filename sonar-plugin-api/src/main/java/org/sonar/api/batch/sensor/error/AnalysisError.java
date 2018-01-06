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

import javax.annotation.CheckForNull;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.sensor.Sensor;

/**
 * Represents an analysis error, such as a parsing error, reported by a {@link Sensor}.
 *
 * @since 6.0
 */
public interface AnalysisError {
  /**
   * The file that was being processed when the error occurred.
   */
  InputFile inputFile();
  
  /**
   * A description of the error.
   */
  @CheckForNull
  String message();
  
  /**
   * Location of the error.
   */
  @CheckForNull
  TextPointer location();
}
