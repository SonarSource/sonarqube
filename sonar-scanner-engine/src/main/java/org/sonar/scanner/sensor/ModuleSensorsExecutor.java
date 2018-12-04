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

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;

public class ModuleSensorsExecutor {
  private static final Logger LOG = Loggers.get(ModuleSensorsExecutor.class);
  private static final Profiler profiler = Profiler.create(LOG);
  private final ModuleSensorExtensionDictionnary selector;
  private final SensorStrategy strategy;
  private final ScannerPluginRepository pluginRepo;
  private final boolean isRoot;

  public ModuleSensorsExecutor(ModuleSensorExtensionDictionnary selector, DefaultInputModule module, InputModuleHierarchy hierarchy,
                               SensorStrategy strategy, ScannerPluginRepository pluginRepo) {
    this.selector = selector;
    this.strategy = strategy;
    this.pluginRepo = pluginRepo;
    this.isRoot = hierarchy.isRoot(module);
  }

  public void execute() {
    Collection<ModuleSensorWrapper> moduleSensors = new ArrayList<>();
    withModuleStrategy(() -> moduleSensors.addAll(selector.selectSensors(false)));
    Collection<ModuleSensorWrapper> deprecatedGlobalSensors = new ArrayList<>();
    if (isRoot) {
      deprecatedGlobalSensors.addAll(selector.selectSensors(true));
    }

    printSensors(moduleSensors, deprecatedGlobalSensors);
    withModuleStrategy(() -> execute(moduleSensors));

    if (isRoot) {
      execute(deprecatedGlobalSensors);
    }
  }

  private void printSensors(Collection<ModuleSensorWrapper> moduleSensors, Collection<ModuleSensorWrapper> globalSensors) {
    String sensors = Stream
      .concat(moduleSensors.stream(), globalSensors.stream())
      .map(Object::toString)
      .collect(Collectors.joining(" -> "));
    LOG.debug("Sensors : {}", sensors);
  }

  private void withModuleStrategy(Runnable r) {
    boolean orig = strategy.isGlobal();
    strategy.setGlobal(false);
    r.run();
    strategy.setGlobal(orig);
  }

  private void execute(Collection<ModuleSensorWrapper> sensors) {
    for (ModuleSensorWrapper sensor : sensors) {
      String sensorName = getSensorName(sensor);
      profiler.startInfo("Sensor " + sensorName);
      sensor.analyse();
      profiler.stopInfo();
    }
  }

  private String getSensorName(ModuleSensorWrapper sensor) {
    ClassLoader cl = getSensorClassLoader(sensor);
    String pluginKey = pluginRepo.getPluginKey(cl);
    if (pluginKey != null) {
      return sensor.toString() + " [" + pluginKey + "]";
    }
    return sensor.toString();
  }

  private static ClassLoader getSensorClassLoader(ModuleSensorWrapper sensor) {
    return sensor.wrappedSensor().getClass().getClassLoader();
  }
}
