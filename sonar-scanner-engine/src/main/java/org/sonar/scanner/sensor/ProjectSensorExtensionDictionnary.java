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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.bootstrap.AbstractExtensionDictionnary;

public class ProjectSensorExtensionDictionnary extends AbstractExtensionDictionnary {

  private final ProjectSensorContext sensorContext;
  private final ProjectSensorOptimizer sensorOptimizer;

  public ProjectSensorExtensionDictionnary(ComponentContainer componentContainer, ProjectSensorContext sensorContext, ProjectSensorOptimizer sensorOptimizer) {
    super(componentContainer);
    this.sensorContext = sensorContext;
    this.sensorOptimizer = sensorOptimizer;
  }

  public List<ProjectSensorWrapper> selectSensors() {
    Collection<ProjectSensor> result = sort(getFilteredExtensions(ProjectSensor.class, null));
    return result.stream()
      .map(s -> new ProjectSensorWrapper(s, sensorContext, sensorOptimizer))
      .filter(ProjectSensorWrapper::shouldExecute)
      .collect(Collectors.toList());
  }
}
