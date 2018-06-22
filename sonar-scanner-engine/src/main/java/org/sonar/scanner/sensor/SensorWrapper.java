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
package org.sonar.scanner.sensor;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

public class SensorWrapper {
  private final Sensor wrappedSensor;
  private final SensorContext context;
  private final DefaultSensorDescriptor descriptor;
  private final SensorOptimizer optimizer;

  public SensorWrapper(Sensor newSensor, SensorContext context, SensorOptimizer optimizer) {
    this.wrappedSensor = newSensor;
    this.optimizer = optimizer;
    this.context = context;
    this.descriptor = new DefaultSensorDescriptor();
    newSensor.describe(this.descriptor);
  }

  public boolean shouldExecute() {
    return optimizer.shouldExecute(descriptor);
  }

  public void analyse() {
    wrappedSensor.execute(context);
  }

  public Sensor wrappedSensor() {
    return wrappedSensor;
  }

  @Override
  public String toString() {
    if (descriptor.name() != null) {
      return descriptor.name();
    } else {
      return wrappedSensor.getClass().getName();
    }
  }

  public boolean isGlobal() {
    return descriptor.isGlobal();
  }
}
