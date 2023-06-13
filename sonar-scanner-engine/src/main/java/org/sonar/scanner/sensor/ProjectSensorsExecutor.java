/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.util.logs.Profiler;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;

public class ProjectSensorsExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectSensorsExecutor.class);
  private static final Profiler profiler = Profiler.create(LOG);
  private final ProjectSensorExtensionDictionary selector;
  private final ScannerPluginRepository pluginRepo;
  private final ExecutingSensorContext executingSensorCtx;

  public ProjectSensorsExecutor(ProjectSensorExtensionDictionary selector, ScannerPluginRepository pluginRepo, ExecutingSensorContext executingSensorCtx) {
    this.selector = selector;
    this.pluginRepo = pluginRepo;
    this.executingSensorCtx = executingSensorCtx;
  }

  public void execute() {
    List<ProjectSensorWrapper> sensors = selector.selectSensors();

    LOG.debug("Sensors : {}", sensors.stream()
      .map(Object::toString)
      .collect(Collectors.joining(" -> ")));
    for (ProjectSensorWrapper sensor : sensors) {
      SensorId sensorId = getSensorId(sensor);
      executingSensorCtx.setSensorExecuting(sensorId);
      profiler.startInfo("Sensor " + sensorId);
      sensor.analyse();
      profiler.stopInfo();
      executingSensorCtx.clearExecutingSensor();
    }
  }

  private SensorId getSensorId(ProjectSensorWrapper sensor) {
    ClassLoader cl = getSensorClassLoader(sensor);
    String pluginKey = pluginRepo.getPluginKey(cl);
    return new SensorId(pluginKey, sensor.toString());
  }

  private static ClassLoader getSensorClassLoader(ProjectSensorWrapper sensor) {
    return sensor.wrappedSensor().getClass().getClassLoader();
  }
}
