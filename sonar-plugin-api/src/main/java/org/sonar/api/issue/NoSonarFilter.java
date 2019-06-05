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
package org.sonar.api.issue;

import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scanner.ScannerSide;

/**
 * Issue filter used to ignore issues created on lines commented with the tag "NOSONAR".
 * <br>
 * Plugins, via {@link org.sonar.api.batch.sensor.Sensor}s, must feed this filter by registering the
 * lines that contain "NOSONAR". Note that filters are disabled for the issues reported by
 * end-users from UI or web services.
 *
 * @since 3.6
 */
@ScannerSide
public abstract class NoSonarFilter {

  /**
   * Register lines in a file that contains the NOSONAR flag.
   *
   * @param inputFile
   * @param noSonarLines Line number starts at 1 in a file
   * @since 5.0
   * @since 7.6 the method can be called multiple times by different sensors, and NOSONAR lines are merged
   */
  public abstract NoSonarFilter noSonarInFile(InputFile inputFile, Set<Integer> noSonarLines);
}
