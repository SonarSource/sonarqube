/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.picocontainer.behaviors.FieldDecorated.Decorator;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.sensor.SensorOptimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerExtensionDictionnaryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ScannerExtensionDictionnary newSelector(Object... extensions) {
    ComponentContainer iocContainer = new ComponentContainer();
    for (Object extension : extensions) {
      iocContainer.addSingleton(extension);
    }
    SensorOptimizer sensorOptimizer = mock(SensorOptimizer.class);
    when(sensorOptimizer.shouldExecute(any())).thenReturn(true);
    PostJobOptimizer postJobOptimizer = mock(PostJobOptimizer.class);
    when(postJobOptimizer.shouldExecute(any())).thenReturn(true);
    return new ScannerExtensionDictionnary(iocContainer, sensorOptimizer, postJobOptimizer);
  }

  @Test
  public void testGetFilteredExtensionWithExtensionMatcher() {
    final Sensor sensor1 = new FakeSensor();
    final Sensor sensor2 = new FakeSensor();

    ScannerExtensionDictionnary selector = newSelector(sensor1, sensor2);
    Collection<Sensor> sensors = selector.select(Sensor.class, true, new ExtensionMatcher() {
      @Override
      public boolean accept(Object extension) {
        return extension.equals(sensor1);
      }
    });

    assertThat(sensors).contains(sensor1);
    assertEquals(1, sensors.size());
  }

  @Test
  public void testGetFilteredExtensions() {
    Sensor sensor1 = new FakeSensor();
    Sensor sensor2 = new FakeSensor();
    Decorator decorator = mock(Decorator.class);

    ScannerExtensionDictionnary selector = newSelector(sensor1, sensor2, decorator);
    Collection<Sensor> sensors = selector.select(Sensor.class, true, null);

    assertThat(sensors).containsOnly(sensor1, sensor2);
  }

  @Test
  public void shouldSearchInParentContainers() {
    Sensor a = new FakeSensor();
    Sensor b = new FakeSensor();
    Sensor c = new FakeSensor();

    ComponentContainer grandParent = new ComponentContainer();
    grandParent.addSingleton(a);

    ComponentContainer parent = grandParent.createChild();
    parent.addSingleton(b);

    ComponentContainer child = parent.createChild();
    child.addSingleton(c);

    ScannerExtensionDictionnary dictionnary = new ScannerExtensionDictionnary(child, mock(SensorOptimizer.class), mock(PostJobOptimizer.class));
    assertThat(dictionnary.select(Sensor.class, true, null)).containsOnly(a, b, c);
  }

  @Test
  public void sortExtensionsByDependency() {
    Object a = new MethodDependentOf(null);
    Object b = new MethodDependentOf(a);
    Object c = new MethodDependentOf(b);

    ScannerExtensionDictionnary selector = newSelector(b, c, a);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(3);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
    assertThat(extensions.get(2)).isEqualTo(c);
  }

  @Test
  public void useMethodAnnotationsToSortExtensions() {
    Object a = new GeneratesSomething("foo");
    Object b = new MethodDependentOf("foo");

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions.size()).isEqualTo(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void methodDependsUponCollection() {
    Object a = new GeneratesSomething("foo");
    Object b = new MethodDependentOf(Arrays.asList("foo"));

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void methodDependsUponArray() {
    Object a = new GeneratesSomething("foo");
    Object b = new MethodDependentOf(new String[] {"foo"});

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void useClassAnnotationsToSortExtensions() {
    Object a = new ClassDependedUpon();
    Object b = new ClassDependsUpon();

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

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

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void inheritAnnotations() {
    Object a = new SubClass("foo");
    Object b = new MethodDependentOf("foo");

    ScannerExtensionDictionnary selector = newSelector(b, a);
    List<MyExtension> extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // change initial order
    selector = newSelector(a, b);
    extensions = Lists.newArrayList(selector.select(MyExtension.class, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test(expected = IllegalStateException.class)
  public void annotatedMethodsCanNotBePrivate() {
    ScannerExtensionDictionnary selector = newSelector();
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

    ScannerExtensionDictionnary selector = newSelector(normal, post, pre);
    List<Sensor> extensions = Lists.newArrayList(selector.select(Sensor.class, true, null));
    assertThat(extensions).containsExactly(pre, normal, post);
  }

  @Test
  public void dependsUponPhaseForPostJob() {
    PrePostJob pre = new PrePostJob();
    NormalPostJob normal = new NormalPostJob();

    ScannerExtensionDictionnary selector = newSelector(normal, pre);
    List<PostJob> extensions = Lists.newArrayList(selector.select(PostJob.class, true, null));
    assertThat(extensions).containsExactly(pre, normal);
  }

  @Test
  public void dependsUponInheritedPhase() {
    PreSensorSubclass pre = new PreSensorSubclass();
    NormalSensor normal = new NormalSensor();
    PostSensorSubclass post = new PostSensorSubclass();

    ScannerExtensionDictionnary selector = newSelector(normal, post, pre);
    List extensions = Lists.newArrayList(selector.select(Sensor.class, true, null));

    assertThat(extensions).containsExactly(pre, normal, post);
  }

  @Test
  public void selectSensors() {
    FakeSensor sensor = new FakeSensor();
    FakeGlobalSensor globalSensor = new FakeGlobalSensor();
    ScannerExtensionDictionnary selector = newSelector(sensor, globalSensor);

    // verify non-global sensors
    Collection<Sensor> extensions = selector.selectSensors(false);
    assertThat(extensions).containsExactly(sensor);

    // verify global sensors
    extensions = selector.selectSensors(true);
    assertThat(extensions).containsExactly(globalSensor);
  }

  class FakeSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(SensorContext context) {
    }
  }

  class FakeGlobalSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
      descriptor.global();
    }

    @Override
    public void execute(SensorContext context) {
    }

  }

  @ScannerSide
  class MethodDependentOf implements MyExtension {
    private Object dep;

    MethodDependentOf(Object o) {
      this.dep = o;
    }

    @DependsUpon
    public Object dependsUponObject() {
      return dep;
    }
  }

  interface MyExtension {

  }

  @ScannerSide
  @DependsUpon("flag")
  class ClassDependsUpon implements MyExtension {
  }

  @ScannerSide
  @DependedUpon("flag")
  class ClassDependedUpon implements MyExtension {
  }

  @ScannerSide
  @DependsUpon("flag")
  interface InterfaceDependsUpon extends MyExtension {
  }

  @ScannerSide
  @DependedUpon("flag")
  interface InterfaceDependedUpon extends MyExtension {
  }

  @ScannerSide
  class GeneratesSomething implements MyExtension {
    private Object gen;

    GeneratesSomething(Object o) {
      this.gen = o;
    }

    @DependedUpon
    public Object generates() {
      return gen;
    }
  }

  class SubClass extends GeneratesSomething {
    SubClass(Object o) {
      super(o);
    }
  }

  class NormalSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(SensorContext context) {
    }

  }

  @Phase(name = Phase.Name.PRE)
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

  @Phase(name = Phase.Name.POST)
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

  private class FakePostJob implements PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }
  }

  class NormalPostJob implements PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }

  }

  @Phase(name = Phase.Name.PRE)
  class PrePostJob implements PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }

  }

}
