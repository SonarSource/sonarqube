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

import java.util.List;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.events.SensorsPhaseHandler;

class SensorsPhaseEvent extends AbstractPhaseEvent<SensorsPhaseHandler>
    implements org.sonar.api.batch.events.SensorsPhaseHandler.SensorsPhaseEvent {

  private final List<Sensor> sensors;

  SensorsPhaseEvent(List<Sensor> sensors, boolean start) {
    super(start);
    this.sensors = sensors;
  }

  @Override
  public List<Sensor> getSensors() {
    return sensors;
  }

  @Override
  protected void dispatch(SensorsPhaseHandler handler) {
    handler.onSensorsPhase(this);
  }

  @Override
  protected Class getType() {
    return SensorsPhaseHandler.class;
  }

}
