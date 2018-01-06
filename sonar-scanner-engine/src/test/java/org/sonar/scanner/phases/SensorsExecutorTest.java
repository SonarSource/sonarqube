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

import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.resources.Project;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.events.EventBus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SensorsExecutorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SensorsExecutor rootModuleExecutor;
  private SensorsExecutor subModuleExecutor;
  private SensorContext context;

  private SensorStrategy strategy = new SensorStrategy();

  private TestSensor perModuleSensor = new TestSensor(strategy);
  private TestSensor globalSensor = new TestSensor(strategy);

  static class TestSensor implements Sensor {
    final SensorStrategy strategy;

    boolean called;
    boolean global;

    TestSensor(SensorStrategy strategy) {
      this.strategy = strategy;
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }

    @Override
    public void analyse(Project module, SensorContext context) {
      called = true;
      global = strategy.isGlobal();
    }
  }

  @Before
  public void setUp() throws IOException {
    context = mock(SensorContext.class);

    ScannerExtensionDictionnary selector = mock(ScannerExtensionDictionnary.class);
    when(selector.selectSensors(any(DefaultInputModule.class), eq(false))).thenReturn(Collections.singleton(perModuleSensor));
    when(selector.selectSensors(any(DefaultInputModule.class), eq(true))).thenReturn(Collections.singleton(globalSensor));

    ProjectDefinition childDef = ProjectDefinition.create().setKey("sub").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());
    ProjectDefinition rootDef = ProjectDefinition.create().setKey("root").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder());

    DefaultInputModule rootModule = TestInputFileBuilder.newDefaultInputModule(rootDef);
    DefaultInputModule subModule = TestInputFileBuilder.newDefaultInputModule(childDef);

    InputModuleHierarchy hierarchy = mock(InputModuleHierarchy.class);
    when(hierarchy.isRoot(rootModule)).thenReturn(true);

    rootModuleExecutor = new SensorsExecutor(selector, rootModule, hierarchy, mock(EventBus.class), strategy);
    subModuleExecutor = new SensorsExecutor(selector, subModule, hierarchy, mock(EventBus.class), strategy);
  }

  @Test
  public void should_not_execute_global_sensor_for_submodule() {
    subModuleExecutor.execute(context);

    assertThat(perModuleSensor.called).isTrue();
    assertThat(perModuleSensor.global).isFalse();
    assertThat(globalSensor.called).isFalse();
  }

  @Test
  public void should_execute_all_sensors_for_root_module() {
    rootModuleExecutor.execute(context);

    assertThat(perModuleSensor.called).isTrue();
    assertThat(perModuleSensor.global).isFalse();
    assertThat(globalSensor.called).isTrue();
    assertThat(globalSensor.global).isTrue();
  }
}
