/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.batch.sensor.coverage;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputFile;

/**
 * This builder is used to define code coverage by tests of a given type (UT/IT/Overall) on files.
 * @since 5.2
 */
@Beta
public interface NewCoverage {

  /**
   * The covered file.
   */
  NewCoverage onFile(InputFile inputFile);

  NewCoverage ofType(CoverageType type);

  /**
   * Call this method as many time as needed to report coverage hits per line. This method should only be called for executable lines.
   * @param line Line number (starts at 1).
   * @param hits Number of time the line was hit.
   */
  NewCoverage lineHits(int line, int hits);

  /**
   * Call this method as many time as needed to report coverage of conditions.
   * @param line Line number (starts at 1).
   * @param conditions Number of conditions on this line (should be greater than 1).
   * @param coveredConditions Number of covered conditions.
   */
  NewCoverage conditions(int line, int conditions, int coveredConditions);

  /**
   * Call this method only once when your are done with defining coverage of the file.
   */
  void save();
}
