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
package org.sonar.scanner.phases;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.filesystem.MutableFileSystem;
import org.sonar.scanner.sensor.ExecutingSensorContext;
import org.sonar.scanner.sensor.ModuleSensorContext;
import org.sonar.scanner.sensor.ModuleSensorExtensionDictionary;
import org.sonar.scanner.sensor.ModuleSensorOptimizer;
import org.sonar.scanner.sensor.ModuleSensorWrapper;
import org.sonar.scanner.sensor.ModuleSensorsExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ModuleSensorsExecutorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private ModuleSensorsExecutor rootModuleExecutor;
  private ModuleSensorsExecutor subModuleExecutor;

  private final SensorStrategy strategy = new SensorStrategy();

  private final ModuleSensorWrapper perModuleSensor = mock(ModuleSensorWrapper.class);
  private final ModuleSensorWrapper globalSensor = mock(ModuleSensorWrapper.class);
  private final ScannerPluginRepository pluginRepository = mock(ScannerPluginRepository.class);
  private final ModuleSensorContext context = mock(ModuleSensorContext.class);
  private final ModuleSensorOptimizer optimizer = mock(ModuleSensorOptimizer.class);
  private final ScannerPluginRepository pluginRepo = mock(ScannerPluginRepository.class);
  private final MutableFileSystem fileSystem = mock(MutableFileSystem.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
  private final ExecutingSensorContext executingSensorContext = mock(ExecutingSensorContext.class);

  @Before
  public void setUp() throws IOException {
    when(perModuleSensor.isGlobal()).thenReturn(false);
    when(perModuleSensor.shouldExecute()).thenReturn(true);
    when(perModuleSensor.wrappedSensor()).thenReturn(mock(Sensor.class));

    when(globalSensor.isGlobal()).thenReturn(true);
    when(globalSensor.shouldExecute()).thenReturn(true);
    when(globalSensor.wrappedSensor()).thenReturn(mock(Sensor.class));

    ModuleSensorExtensionDictionary selector = mock(ModuleSensorExtensionDictionary.class);
    when(selector.selectSensors(false)).thenReturn(Collections.singleton(perModuleSensor));
    when(selector.selectSensors(true)).thenReturn(Collections.singleton(globalSensor));

    ProjectDefinition childDef = ProjectDefinition.create().setKey("sub").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());
    ProjectDefinition rootDef = ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());

    DefaultInputModule rootModule = TestInputFileBuilder.newDefaultInputModule(rootDef);
    DefaultInputModule subModule = TestInputFileBuilder.newDefaultInputModule(childDef);

    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    when(hierarchy.isRoot(rootModule)).thenReturn(true);

    rootModuleExecutor = new ModuleSensorsExecutor(selector, rootModule, hierarchy, strategy, pluginRepository, executingSensorContext);
    subModuleExecutor = new ModuleSensorsExecutor(selector, subModule, hierarchy, strategy, pluginRepository, executingSensorContext);
  }

  @Test
  public void should_not_execute_global_sensor_for_submodule() {
    subModuleExecutor.execute();

    verify(perModuleSensor).analyse();
    verify(perModuleSensor).wrappedSensor();

    verifyNoInteractions(globalSensor);
    verifyNoMoreInteractions(perModuleSensor, globalSensor);
  }

  @Test
  public void should_execute_all_sensors_for_root_module() {
    rootModuleExecutor.execute();

    verify(globalSensor).wrappedSensor();
    verify(perModuleSensor).wrappedSensor();

    verify(globalSensor).analyse();
    verify(perModuleSensor).analyse();

    verify(executingSensorContext, times(2)).setSensorExecuting(any());
    verify(executingSensorContext, times(2)).clearExecutingSensor();
    verifyNoMoreInteractions(perModuleSensor, globalSensor, executingSensorContext);
  }

  @Test
  @UseDataProvider("sensorsOnlyChangedInPR")
  public void should_restrict_filesystem_when_pull_request_and_expected_sensor(String sensorName) throws IOException {
    when(branchConfiguration.branchType()).thenReturn(BranchType.PULL_REQUEST);
    ModuleSensorsExecutor executor = createModuleExecutor(sensorName);

    executor.execute();

    verify(fileSystem, times(2)).setRestrictToChangedFiles(true);

    assertThat(logTester.logs().stream().anyMatch(
      p -> p.contains(String.format("Sensor %s is restricted to changed files only", sensorName)))
    ).isTrue();
  }

  @Test
  @UseDataProvider("sensorsOnlyChangedInPR")
  public void should_not_restrict_filesystem_when_branch(String sensorName) throws IOException {
    when(branchConfiguration.branchType()).thenReturn(BranchType.BRANCH);
    ModuleSensorsExecutor executor = createModuleExecutor(sensorName);

    executor.execute();

    verify(fileSystem, times(2)).setRestrictToChangedFiles(false);

    assertThat(logTester.logs().stream().anyMatch(
      p -> p.contains(String.format("Sensor %s is restricted to changed files only", sensorName)))
    ).isFalse();
  }

  @Test
  public void should_not_restrict_filesystem_when_pull_request_and_non_expected_sensor() throws IOException {
    String sensorName = "NonRestrictedSensor";
    when(branchConfiguration.branchType()).thenReturn(BranchType.PULL_REQUEST);
    ModuleSensorsExecutor executor = createModuleExecutor(sensorName);

    executor.execute();

    verify(fileSystem, times(2)).setRestrictToChangedFiles(false);

    assertThat(logTester.logs().stream().anyMatch(
      p -> p.contains(String.format("Sensor %s is restricted to changed files only", sensorName)))
    ).isFalse();
  }

  @DataProvider
  public static Object[][] sensorsOnlyChangedInPR() {
    return new Object[][] {DefaultSensorDescriptor.HARDCODED_INDEPENDENT_FILE_SENSORS.toArray()};
  }

  private ModuleSensorsExecutor createModuleExecutor(String sensorName) throws IOException {
    Sensor sensor = new TestSensor(sensorName);
    ModuleSensorWrapper sensorWrapper = new ModuleSensorWrapper(sensor, context, optimizer, fileSystem, branchConfiguration);

    ModuleSensorExtensionDictionary selector = mock(ModuleSensorExtensionDictionary.class);
    when(selector.selectSensors(false)).thenReturn(Collections.singleton(sensorWrapper));
    when(selector.selectSensors(true)).thenReturn(Collections.singleton(sensorWrapper));

    ProjectDefinition rootDef = ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());

    DefaultInputModule rootModule = TestInputFileBuilder.newDefaultInputModule(rootDef);

    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    when(hierarchy.isRoot(rootModule)).thenReturn(true);

    return new ModuleSensorsExecutor(selector, rootModule, hierarchy, strategy, pluginRepo, executingSensorContext);
  }

  private static class TestSensor implements Sensor {

    private final String name;

    public TestSensor(String name) {
      this.name = name;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
      descriptor.name(name);
    }

    @Override
    public void execute(SensorContext context) {

    }
  }
}
