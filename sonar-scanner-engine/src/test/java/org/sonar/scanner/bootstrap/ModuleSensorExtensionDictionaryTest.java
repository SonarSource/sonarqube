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
package org.sonar.scanner.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.MutableFileSystem;
import org.sonar.scanner.sensor.ModuleSensorContext;
import org.sonar.scanner.sensor.ModuleSensorExtensionDictionary;
import org.sonar.scanner.sensor.ModuleSensorOptimizer;
import org.sonar.scanner.sensor.ModuleSensorWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleSensorExtensionDictionaryTest {
  private final ModuleSensorOptimizer sensorOptimizer = mock(ModuleSensorOptimizer.class);
  private final MutableFileSystem fileSystem = mock(MutableFileSystem.class);
  private final BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);

  @Before
  public void setUp() {
    when(sensorOptimizer.shouldExecute(any(DefaultSensorDescriptor.class))).thenReturn(true);
  }

  private ModuleSensorExtensionDictionary newSelector(Class type, Object... instances) {
    ExtensionContainer iocContainer = mock(ExtensionContainer.class);
    when(iocContainer.getComponentsByType(type)).thenReturn(Arrays.asList(instances));
    return new ModuleSensorExtensionDictionary(iocContainer, mock(ModuleSensorContext.class), sensorOptimizer, fileSystem, branchConfiguration);
  }

  @Test
  public void testGetFilteredExtensionWithExtensionMatcher() {
    final Sensor sensor1 = new FakeSensor();
    final Sensor sensor2 = new FakeSensor();

    ModuleSensorExtensionDictionary selector = newSelector(Sensor.class, sensor1, sensor2);
    Collection<Sensor> sensors = selector.select(Sensor.class, true, extension -> extension.equals(sensor1));
    assertThat(sensors).contains(sensor1);
    assertEquals(1, sensors.size());
  }

  @Test
  public void testGetFilteredExtensions() {
    Sensor sensor1 = new FakeSensor();
    Sensor sensor2 = new FakeSensor();

    ModuleSensorExtensionDictionary selector = newSelector(Sensor.class, sensor1, sensor2);
    Collection<Sensor> sensors = selector.select(Sensor.class, false, null);

    assertThat(sensors).containsOnly(sensor1, sensor2);
  }

  @Test
  public void shouldSearchInParentContainers() {
    Sensor a = new FakeSensor();
    Sensor b = new FakeSensor();
    Sensor c = new FakeSensor();

    ExtensionContainer grandParent = mock(ExtensionContainer.class);
    when(grandParent.getComponentsByType(Sensor.class)).thenReturn(List.of(a));

    ExtensionContainer parent = mock(ExtensionContainer.class);
    when(parent.getComponentsByType(Sensor.class)).thenReturn(List.of(b));
    when(parent.getParent()).thenReturn(grandParent);

    ExtensionContainer child = mock(ExtensionContainer.class);
    when(child.getComponentsByType(Sensor.class)).thenReturn(List.of(c));
    when(child.getParent()).thenReturn(parent);

    ModuleSensorExtensionDictionary dictionnary = new ModuleSensorExtensionDictionary(child, mock(ModuleSensorContext.class), mock(ModuleSensorOptimizer.class),
      fileSystem, branchConfiguration);
    assertThat(dictionnary.select(Sensor.class, true, null)).containsOnly(a, b, c);
  }

  @Test
  public void sortExtensionsByDependency() {
    Object a = new MethodDependentOf(null);
    Object b = new MethodDependentOf(a);
    Object c = new MethodDependentOf(b);

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, b, c, a);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(3);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
    assertThat(extensions.get(2)).isEqualTo(c);
  }

  @Test
  public void useMethodAnnotationsToSortExtensions() {
    Object a = new GeneratesSomething("foo");
    Object b = new MethodDependentOf("foo");

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, a, b);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(Marker.class, b, a);
    extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void methodDependsUponCollection() {
    Object a = new GeneratesSomething("foo");
    Object b = new MethodDependentOf(Arrays.asList("foo"));

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, a, b);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(Marker.class, b, a);
    extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void methodDependsUponArray() {
    Object a = new GeneratesSomething("foo");
    Object b = new MethodDependentOf(new String[] {"foo"});

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, a, b);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(Marker.class, b, a);
    extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void useClassAnnotationsToSortExtensions() {
    Object a = new ClassDependedUpon();
    Object b = new ClassDependsUpon();

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, a, b);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(Marker.class, b, a);
    extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void useClassAnnotationsOnInterfaces() {
    Object a = new InterfaceDependedUpon() {
    };
    Object b = new InterfaceDependsUpon() {
    };

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, a, b);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(Marker.class, b, a);
    extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void inheritAnnotations() {
    Object a = new SubClass("foo");
    Object b = new MethodDependentOf("foo");

    ModuleSensorExtensionDictionary selector = newSelector(Marker.class, b, a);
    List<Object> extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // change initial order
    selector = newSelector(Marker.class, a, b);
    extensions = new ArrayList<>(selector.select(Marker.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test(expected = IllegalStateException.class)
  public void annotatedMethodsCanNotBePrivate() {
    ModuleSensorExtensionDictionary selector = new ModuleSensorExtensionDictionary(mock(ExtensionContainer.class), mock(ModuleSensorContext.class),
      sensorOptimizer, fileSystem, branchConfiguration);
    Object wrong = new Object() {
      @DependsUpon
      private Object foo() {
        return "foo";
      }
    };
    selector.evaluateAnnotatedClasses(wrong, DependsUpon.class);
  }

  @Test
  public void dependsUponPhaseForSensors() {
    PreSensor pre = new PreSensor();
    NormalSensor normal = new NormalSensor();
    PostSensor post = new PostSensor();

    ModuleSensorExtensionDictionary selector = newSelector(Sensor.class, normal, post, pre);
    assertThat(selector.selectSensors(false)).extracting("wrappedSensor").containsExactly(pre, normal, post);
  }

  @Test
  public void dependsUponInheritedPhase() {
    PreSensorSubclass pre = new PreSensorSubclass();
    NormalSensor normal = new NormalSensor();
    PostSensorSubclass post = new PostSensorSubclass();

    ModuleSensorExtensionDictionary selector = newSelector(Sensor.class, normal, post, pre);
    List extensions = new ArrayList<>(selector.select(Sensor.class, true, null));

    assertThat(extensions).containsExactly(pre, normal, post);
  }

  @Test
  public void selectSensors() {
    FakeSensor nonGlobalSensor = new FakeSensor();
    FakeGlobalSensor globalSensor = new FakeGlobalSensor();
    ModuleSensorExtensionDictionary selector = newSelector(Sensor.class, nonGlobalSensor, globalSensor);

    // verify non-global sensor
    Collection<ModuleSensorWrapper> extensions = selector.selectSensors(false);
    assertThat(extensions).hasSize(1);
    assertThat(extensions).extracting("wrappedSensor").containsExactly(nonGlobalSensor);

    // verify global sensor
    extensions = selector.selectSensors(true);
    assertThat(extensions).extracting("wrappedSensor").containsExactly(globalSensor);
  }

  interface Marker {

  }

  static class FakeSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {

    }

    @Override
    public void execute(SensorContext context) {

    }
  }

  static class FakeGlobalSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
      descriptor.global();
    }

    @Override
    public void execute(SensorContext context) {
    }

  }

  @ScannerSide static
  class MethodDependentOf implements Marker {
    private Object dep;

    MethodDependentOf(Object o) {
      this.dep = o;
    }

    @DependsUpon
    public Object dependsUponObject() {
      return dep;
    }
  }

  @ScannerSide
  @DependsUpon("flag") static
  class ClassDependsUpon implements Marker {
  }

  @ScannerSide
  @DependedUpon("flag") static
  class ClassDependedUpon implements Marker {
  }

  @ScannerSide
  @DependsUpon("flag")
  interface InterfaceDependsUpon extends Marker {
  }

  @ScannerSide
  @DependedUpon("flag")
  interface InterfaceDependedUpon extends Marker {
  }

  @ScannerSide static
  class GeneratesSomething implements Marker {
    private Object gen;

    GeneratesSomething(Object o) {
      this.gen = o;
    }

    @DependedUpon
    public Object generates() {
      return gen;
    }
  }

  class SubClass extends GeneratesSomething implements Marker {
    SubClass(Object o) {
      super(o);
    }
  }

  static class NormalSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(SensorContext context) {
    }

  }

  @Phase(name = Phase.Name.PRE) static
  class PreSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(SensorContext context) {
    }

  }

  class PreSensorSubclass extends PreSensor {

  }

  @Phase(name = Phase.Name.POST) static
  class PostSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(SensorContext context) {
    }

  }

  class PostSensorSubclass extends PostSensor {

  }

}
