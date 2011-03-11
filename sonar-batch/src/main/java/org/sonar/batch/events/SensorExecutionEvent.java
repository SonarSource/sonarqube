/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.events;

import org.sonar.api.batch.Sensor;

/**
 * Fired on each execution of {@link Sensor} on start and on finish.
 */
public class SensorExecutionEvent extends SonarEvent<SensorExecutionHandler> {

  private Sensor sensor;
  private boolean start;

  public SensorExecutionEvent(Sensor sensor, boolean start) {
    this.sensor = sensor;
    this.start = start;
  }

  public Sensor getSensor() {
    return sensor;
  }

  public boolean isStartExecution() {
    return start;
  }

  public boolean isDoneExecution() {
    return !start;
  }

  @Override
  public void dispatch(SensorExecutionHandler handler) {
    handler.onSensorExecution(this);
  }

  @Override
  public Class getType() {
    return SensorExecutionHandler.class;
  }

}
