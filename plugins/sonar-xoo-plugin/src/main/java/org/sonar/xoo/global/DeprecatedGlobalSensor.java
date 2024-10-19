/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.xoo.global;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeprecatedGlobalSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedGlobalSensor.class);
  public static final String ENABLE_PROP = "sonar.scanner.mediumtest.deprecatedGlobalSensor";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Deprecated Global Sensor")
      .global()
      .onlyWhenConfiguration(c -> c.hasKey(ENABLE_PROP));
  }

  @Override
  public void execute(SensorContext context) {
    context.fileSystem().inputFiles(context.fileSystem().predicates().all()).forEach(inputFile -> LOG.info("Deprecated Global Sensor: {}", inputFile.relativePath()));
  }
}
