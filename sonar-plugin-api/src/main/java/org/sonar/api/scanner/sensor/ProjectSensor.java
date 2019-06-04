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
package org.sonar.api.scanner.sensor;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * <p>
 * A sensor is invoked once for each project analysis. Sensors are mainly used to add measures and issues on {@link org.sonar.api.batch.fs.InputFile}s.
 * <p>
 * For example the Cobertura Sensor parses Cobertura report and saves the first-level of measures on files.
 * 
 * For testing purpose you can use  SensorContextTester
 * @since 7.6
 */
@ScannerSide
@SonarLintSide
@ExtensionPoint
public interface ProjectSensor {

  /**
   * Populate {@link SensorDescriptor} of this sensor.
   */
  void describe(SensorDescriptor descriptor);

  /**
   * The actual sensor code.
   */
  void execute(SensorContext context);

}
