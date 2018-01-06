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
package org.sonar.api.batch.sensor.coverage;

import org.sonar.api.batch.fs.InputFile;

/**
 * This class is used to report code coverage on files.
 * 
 * Example:
 * 
 * <pre>
 *   sensorContext.newCoverage().onFile(file)
       .lineHits(1, 2)
       .lineHits(2, 5)
       .lineHits(3, 0)
       . ...
       .conditions(3, 4, 2)
       .conditions(12, 2, 2)
       . ...
       .save();
 *     
 * </pre>
 * 
 * Since 6.2 you can save several reports for the same file and reports will be merged using the following "additive" strategy:
 * <ul>
 *   <li>Line hits are cumulated</li>
 *   <li>We keep the max for condition coverage. Examples: 2/4 + 2/4 = 2/4, 2/4 + 3/4 = 3/4</li>
 * </ul>
 * 
 * @since 5.2
 */
public interface NewCoverage {

  /**
   * The covered file.
   */
  NewCoverage onFile(InputFile inputFile);

  /**
   * @deprecated since 6.2 SonarQube merge all coverage reports and don't keep track of different test category
   */
  @Deprecated
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
   * Call this method to save the coverage report for the given file. Data will be merged with existing coverage information.
   */
  void save();
}
