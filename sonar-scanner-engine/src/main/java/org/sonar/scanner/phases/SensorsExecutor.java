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

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.resources.Project;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.events.EventBus;

@ScannerSide
public class SensorsExecutor {
  private final ScannerExtensionDictionnary selector;
  private final DefaultInputModule module;
  private final EventBus eventBus;
  private final SensorStrategy strategy;
  private final boolean isRoot;

  public SensorsExecutor(ScannerExtensionDictionnary selector, DefaultInputModule module, InputModuleHierarchy hierarchy, EventBus eventBus, SensorStrategy strategy) {
    this.selector = selector;
    this.module = module;
    this.eventBus = eventBus;
    this.strategy = strategy;
    this.isRoot = hierarchy.isRoot(module);
  }

  public void execute(SensorContext context) {
    Collection<Sensor> perModuleSensors = selector.selectSensors(module, false);
    Collection<Sensor> globalSensors;
    if (isRoot) {
      boolean orig = strategy.isGlobal();
      strategy.setGlobal(true);
      globalSensors = selector.selectSensors(module, true);
      strategy.setGlobal(orig);
    } else {
      globalSensors = Collections.emptyList();
    }

    Collection<Sensor> allSensors = new ArrayList<>(perModuleSensors);
    allSensors.addAll(globalSensors);
    eventBus.fireEvent(new SensorsPhaseEvent(Lists.newArrayList(allSensors), true));

    execute(context, perModuleSensors);

    if (isRoot) {
      boolean orig = strategy.isGlobal();
      strategy.setGlobal(true);
      execute(context, globalSensors);
      strategy.setGlobal(orig);
    }

    eventBus.fireEvent(new SensorsPhaseEvent(Lists.newArrayList(allSensors), false));
  }

  private void execute(SensorContext context, Collection<Sensor> sensors) {
    for (Sensor sensor : sensors) {
      executeSensor(context, sensor);
    }
  }

  private void executeSensor(SensorContext context, Sensor sensor) {
    eventBus.fireEvent(new SensorExecutionEvent(sensor, true));
    sensor.analyse(new Project(module), context);
    eventBus.fireEvent(new SensorExecutionEvent(sensor, false));
  }
}
