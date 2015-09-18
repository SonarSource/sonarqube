/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.phases;


import org.sonar.batch.util.BatchUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.batch.events.SensorsPhaseHandler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

public class PhasesTimeProfiler implements SensorExecutionHandler, SensorsPhaseHandler {

  private static final Logger LOG = Loggers.get(PhasesTimeProfiler.class);

  private Profiler profiler = Profiler.create(LOG);

  @Override
  public void onSensorsPhase(SensorsPhaseEvent event) {
    if (event.isStart()) {
      LOG.debug("Sensors : {}", StringUtils.join(event.getSensors(), " -> "));
    }
  }

  @Override
  public void onSensorExecution(SensorExecutionEvent event) {
    if (event.isStart()) {
      profiler.startInfo("Sensor " + BatchUtils.describe(event.getSensor()));
    } else {
      profiler.stopInfo();
    }
  }

}
