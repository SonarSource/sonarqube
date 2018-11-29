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
package org.sonar.scanner.bootstrap;

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.sensor.SensorOptimizer;
import org.sonar.scanner.sensor.SensorWrapper;

public class SensorExtensionDictionnary extends AbstractExtensionDictionnary {

  private final SensorContext sensorContext;
  private final SensorOptimizer sensorOptimizer;

  public SensorExtensionDictionnary(ComponentContainer componentContainer, SensorContext sensorContext, SensorOptimizer sensorOptimizer) {
    super(componentContainer);
    this.sensorContext = sensorContext;
    this.sensorOptimizer = sensorOptimizer;
  }

  public Collection<SensorWrapper> selectSensors(boolean global) {
    Collection<Sensor> result = sort(getFilteredExtensions(Sensor.class, null));
    return result.stream()
      .map(s -> new SensorWrapper(s, sensorContext, sensorOptimizer))
      .filter(s -> global == s.isGlobal() && s.shouldExecute())
      .collect(Collectors.toList());
  }
}
