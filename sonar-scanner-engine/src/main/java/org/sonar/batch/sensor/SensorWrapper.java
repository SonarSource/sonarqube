/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.resources.Project;

public class SensorWrapper implements org.sonar.api.batch.Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(SensorWrapper.class);

  private Sensor wrappedSensor;
  private SensorContext adaptor;
  private DefaultSensorDescriptor descriptor;
  private SensorOptimizer optimizer;

  public SensorWrapper(Sensor newSensor, SensorContext adaptor, SensorOptimizer optimizer) {
    this.wrappedSensor = newSensor;
    this.optimizer = optimizer;
    descriptor = new DefaultSensorDescriptor();
    newSensor.describe(descriptor);
    this.adaptor = adaptor;
  }

  public Sensor wrappedSensor() {
    return wrappedSensor;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return optimizer.shouldExecute(descriptor);
  }

  @Override
  public void analyse(Project module, org.sonar.api.batch.SensorContext context) {
    wrappedSensor.execute(adaptor);
  }

  @Override
  public String toString() {
    return descriptor.name() + (LOG.isDebugEnabled() ? " (wrapped)" : "");
  }
}
