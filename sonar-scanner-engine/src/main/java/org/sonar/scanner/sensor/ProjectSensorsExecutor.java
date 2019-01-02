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
package org.sonar.scanner.sensor;

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;

public class ProjectSensorsExecutor {
  private static final Logger LOG = Loggers.get(ProjectSensorsExecutor.class);
  private static final Profiler profiler = Profiler.create(LOG);
  private final ProjectSensorExtensionDictionnary selector;
  private final ScannerPluginRepository pluginRepo;

  public ProjectSensorsExecutor(ProjectSensorExtensionDictionnary selector, ScannerPluginRepository pluginRepo) {
    this.selector = selector;
    this.pluginRepo = pluginRepo;
  }

  public void execute() {
    List<ProjectSensorWrapper> sensors = selector.selectSensors();

    LOG.debug("Sensors : {}", sensors.stream()
      .map(Object::toString)
      .collect(Collectors.joining(" -> ")));
    for (ProjectSensorWrapper sensor : sensors) {
      String sensorName = getSensorName(sensor);
      profiler.startInfo("Sensor " + sensorName);
      sensor.analyse();
      profiler.stopInfo();
    }
  }

  private String getSensorName(ProjectSensorWrapper sensor) {
    ClassLoader cl = getSensorClassLoader(sensor);
    String pluginKey = pluginRepo.getPluginKey(cl);
    if (pluginKey != null) {
      return sensor.toString() + " [" + pluginKey + "]";
    }
    return sensor.toString();
  }

  private static ClassLoader getSensorClassLoader(ProjectSensorWrapper sensor) {
    return sensor.wrappedSensor().getClass().getClassLoader();
  }
}
