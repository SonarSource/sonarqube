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

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.filesystem.MutableFileSystem;

public abstract class AbstractSensorWrapper<G extends ProjectSensor> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSensorWrapper.class);

  private final G wrappedSensor;
  private final SensorContext context;
  private final MutableFileSystem fileSystem;
  private final DefaultSensorDescriptor descriptor;
  private final AbstractSensorOptimizer optimizer;
  private final boolean isPullRequest;

  public AbstractSensorWrapper(G sensor, SensorContext context, AbstractSensorOptimizer optimizer, MutableFileSystem fileSystem, BranchConfiguration branchConfiguration) {
    this.wrappedSensor = sensor;
    this.optimizer = optimizer;
    this.context = context;
    this.descriptor = new DefaultSensorDescriptor();
    this.fileSystem = fileSystem;
    this.isPullRequest = branchConfiguration.branchType() == BranchType.PULL_REQUEST;
    sensor.describe(this.descriptor);
    if (descriptor.name() == null) {
      descriptor.name(sensor.getClass().getName());
    }
  }

  public boolean shouldExecute() {
    return optimizer.shouldExecute(descriptor);
  }

  public void analyse() {
    boolean sensorIsRestricted = descriptor.isProcessesFilesIndependently() && isPullRequest;
    if (sensorIsRestricted) {
      LOGGER.info("Sensor {} is restricted to changed files only", descriptor.name());
    }
    fileSystem.setRestrictToChangedFiles(sensorIsRestricted);
    wrappedSensor.execute(context);
  }

  public G wrappedSensor() {
    return wrappedSensor;
  }

  @Override
  public String toString() {
    return descriptor.name();
  }

  public boolean isGlobal() {
    return descriptor.isGlobal();
  }
}
