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

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.logs.Profiler;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.sensor.SensorWrapper;

@ScannerSide
public class SensorsExecutor {
  private static final Logger LOG = Loggers.get(SensorsExecutor.class);
  private static final Profiler profiler = Profiler.create(LOG);
  private final ScannerExtensionDictionnary selector;
  private final SensorStrategy strategy;
  private final ScannerPluginRepository pluginRepo;
  private final boolean isRoot;

  public SensorsExecutor(ScannerExtensionDictionnary selector, DefaultInputModule module, InputModuleHierarchy hierarchy,
    SensorStrategy strategy, ScannerPluginRepository pluginRepo) {
    this.selector = selector;
    this.strategy = strategy;
    this.pluginRepo = pluginRepo;
    this.isRoot = hierarchy.isRoot(module);
  }

  public void execute() {
    Collection<SensorWrapper> moduleSensors = selector.selectSensors(false);
    Collection<SensorWrapper> globalSensors = new ArrayList<>();
    if (isRoot) {
      withGlobalStrategy(() -> globalSensors.addAll(selector.selectSensors(true)));
    }

    printSensors(moduleSensors, globalSensors);
    execute(moduleSensors);

    if (isRoot) {
      withGlobalStrategy(() -> execute(globalSensors));
    }
  }

  private void printSensors(Collection<SensorWrapper> moduleSensors, Collection<SensorWrapper> globalSensors) {
    String sensors = Stream
      .concat(moduleSensors.stream(), globalSensors.stream())
      .map(Object::toString)
      .collect(Collectors.joining(" -> "));
    LOG.debug("Sensors : {}", sensors);
  }

  private void withGlobalStrategy(Runnable r) {
    boolean orig = strategy.isGlobal();
    strategy.setGlobal(true);
    r.run();
    strategy.setGlobal(orig);
  }

  private void execute(Collection<SensorWrapper> sensors) {
    for (SensorWrapper sensor : sensors) {
      String sensorName = getSensorName(sensor);
      profiler.startInfo("Sensor " + sensorName);
      sensor.analyse();
      profiler.stopInfo();
    }
  }

  private String getSensorName(SensorWrapper sensor) {
    ClassLoader cl = getSensorClassLoader(sensor);
    String pluginKey = pluginRepo.getPluginKey(cl);
    if (pluginKey != null) {
      return sensor.toString() + " [" + pluginKey + "]";
    }
    return sensor.toString();
  }

  private static ClassLoader getSensorClassLoader(SensorWrapper sensor) {
    return sensor.wrappedSensor().getClass().getClassLoader();
  }
}
