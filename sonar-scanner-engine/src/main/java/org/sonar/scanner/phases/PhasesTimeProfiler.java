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
package org.sonar.scanner.phases;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.batch.events.SensorsPhaseHandler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.sensor.SensorWrapper;
import org.sonar.scanner.util.ScannerUtils;

public class PhasesTimeProfiler implements SensorExecutionHandler, SensorsPhaseHandler {

  private static final Logger LOG = Loggers.get(PhasesTimeProfiler.class);
  private Profiler profiler = Profiler.create(LOG);
  private final ScannerPluginRepository pluginRepo;

  public PhasesTimeProfiler(ScannerPluginRepository pluginRepo) {
    this.pluginRepo = pluginRepo;
  }

  @Override
  public void onSensorsPhase(SensorsPhaseEvent event) {
    if (event.isStart()) {
      LOG.debug("Sensors : {}", StringUtils.join(event.getSensors(), " -> "));
    }
  }

  @Override
  public void onSensorExecution(SensorExecutionEvent event) {
    if (event.isStart()) {
      ClassLoader cl = getSensorClassLoader(event.getSensor());
      String pluginKey = pluginRepo.getPluginKey(cl);
      String suffix = "";
      if (pluginKey != null) {
        suffix = " [" + pluginKey + "]";
      }
      profiler.startInfo("Sensor " + ScannerUtils.describe(event.getSensor()) + suffix);
    } else {
      profiler.stopInfo();
    }
  }

  private static ClassLoader getSensorClassLoader(Sensor sensor) {
    if (sensor instanceof SensorWrapper) {
      SensorWrapper wrapper = (SensorWrapper) sensor;
      return wrapper.wrappedSensor().getClass().getClassLoader();
    } else {
      return sensor.getClass().getClassLoader();
    }
  }

}
