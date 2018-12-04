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
package org.sonar.scanner.phases;

import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.scanner.sensor.ModuleSensorExtensionDictionnary;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.sensor.ModuleSensorsExecutor;
import org.sonar.scanner.sensor.ModuleSensorWrapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ModuleSensorsExecutorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ModuleSensorsExecutor rootModuleExecutor;
  private ModuleSensorsExecutor subModuleExecutor;

  private SensorStrategy strategy = new SensorStrategy();

  private ModuleSensorWrapper perModuleSensor = mock(ModuleSensorWrapper.class);
  private ModuleSensorWrapper globalSensor = mock(ModuleSensorWrapper.class);
  private ScannerPluginRepository pluginRepository = mock(ScannerPluginRepository.class);

  @Before
  public void setUp() throws IOException {
    when(perModuleSensor.isGlobal()).thenReturn(false);
    when(perModuleSensor.shouldExecute()).thenReturn(true);
    when(perModuleSensor.wrappedSensor()).thenReturn(mock(Sensor.class));

    when(globalSensor.isGlobal()).thenReturn(true);
    when(globalSensor.shouldExecute()).thenReturn(true);
    when(globalSensor.wrappedSensor()).thenReturn(mock(Sensor.class));

    ModuleSensorExtensionDictionnary selector = mock(ModuleSensorExtensionDictionnary.class);
    when(selector.selectSensors(false)).thenReturn(Collections.singleton(perModuleSensor));
    when(selector.selectSensors(true)).thenReturn(Collections.singleton(globalSensor));

    ProjectDefinition childDef = ProjectDefinition.create().setKey("sub").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());
    ProjectDefinition rootDef = ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());

    DefaultInputModule rootModule = TestInputFileBuilder.newDefaultInputModule(rootDef);
    DefaultInputModule subModule = TestInputFileBuilder.newDefaultInputModule(childDef);

    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    when(hierarchy.isRoot(rootModule)).thenReturn(true);

    rootModuleExecutor = new ModuleSensorsExecutor(selector, rootModule, hierarchy, strategy, pluginRepository);
    subModuleExecutor = new ModuleSensorsExecutor(selector, subModule, hierarchy, strategy, pluginRepository);
  }

  @Test
  public void should_not_execute_global_sensor_for_submodule() {
    subModuleExecutor.execute();

    verify(perModuleSensor).analyse();
    verify(perModuleSensor).wrappedSensor();

    verifyZeroInteractions(globalSensor);
    verifyNoMoreInteractions(perModuleSensor, globalSensor);
  }

  @Test
  public void should_execute_all_sensors_for_root_module() {
    rootModuleExecutor.execute();

    verify(globalSensor).wrappedSensor();
    verify(perModuleSensor).wrappedSensor();

    verify(globalSensor).analyse();
    verify(perModuleSensor).analyse();

    verifyNoMoreInteractions(perModuleSensor, globalSensor);
  }
}
