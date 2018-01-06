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
package org.sonar.scanner.sensor.noop;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;

public class NoOpNewCoverage implements NewCoverage {

  @Override
  public NewCoverage onFile(InputFile inputFile) {
    // no op
    return this;
  }

  @Override
  public NewCoverage ofType(CoverageType type) {
    // no op
    return this;
  }

  @Override
  public NewCoverage lineHits(int line, int hits) {
    // no op
    return this;
  }

  @Override
  public NewCoverage conditions(int line, int conditions, int coveredConditions) {
    // no op
    return this;
  }

  @Override
  public void save() {
    // no op
  }
}
