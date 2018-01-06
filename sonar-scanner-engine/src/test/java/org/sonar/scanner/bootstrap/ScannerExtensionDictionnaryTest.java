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

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.BuildBreaker;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.resources.Project;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.sensor.DefaultSensorContext;
import org.sonar.scanner.sensor.SensorOptimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ScannerExtensionDictionnaryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ScannerExtensionDictionnary newSelector(Object... extensions) {
    ComponentContainer iocContainer = new ComponentContainer();
    for (Object extension : extensions) {
      iocContainer.addSingleton(extension);
    }
    return new ScannerExtensionDictionnary(iocContainer, mock(DefaultSensorContext.class), mock(SensorOptimizer.class),
      mock(PostJobContext.class),
      mock(PostJobOptimizer.class));
  }

  @Test
  public void testGetFilteredExtensionWithExtensionMatcher() {
    final Sensor sensor1 = new FakeSensor();
    final Sensor sensor2 = new FakeSensor();

    ScannerExtensionDictionnary selector = newSelector(sensor1, sensor2);
    Collection<Sensor> sensors = selector.select(Sensor.class, null, true, new ExtensionMatcher() {
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
    Collection<Sensor> sensors = selector.select(Sensor.class, null, true, null);

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

    ScannerExtensionDictionnary dictionnary = new ScannerExtensionDictionnary(child, mock(DefaultSensorContext.class),
      mock(SensorOptimizer.class), mock(PostJobContext.class),
      mock(PostJobOptimizer.class));
    assertThat(dictionnary.select(Sensor.class, null, true, null)).containsOnly(a, b, c);
  }

  @Test
  public void sortExtensionsByDependency() {
    BatchExtension a = new MethodDependentOf(null);
    BatchExtension b = new MethodDependentOf(a);
    BatchExtension c = new MethodDependentOf(b);

    ScannerExtensionDictionnary selector = newSelector(b, c, a);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(3);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
    assertThat(extensions.get(2)).isEqualTo(c);
  }

  @Test
  public void useMethodAnnotationsToSortExtensions() {
    BatchExtension a = new GeneratesSomething("foo");
    BatchExtension b = new MethodDependentOf("foo");

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions.size()).isEqualTo(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void methodDependsUponCollection() {
    BatchExtension a = new GeneratesSomething("foo");
    BatchExtension b = new MethodDependentOf(Arrays.asList("foo"));

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void methodDependsUponArray() {
    BatchExtension a = new GeneratesSomething("foo");
    BatchExtension b = new MethodDependentOf(new String[] {"foo"});

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void useClassAnnotationsToSortExtensions() {
    BatchExtension a = new ClassDependedUpon();
    BatchExtension b = new ClassDependsUpon();

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void useClassAnnotationsOnInterfaces() {
    BatchExtension a = new InterfaceDependedUpon() {
    };
    BatchExtension b = new InterfaceDependsUpon() {
    };

    ScannerExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test
  public void checkProject() throws IOException {
    BatchExtension ok = new CheckProjectOK();
    BatchExtension ko = new CheckProjectKO();

    ScannerExtensionDictionnary selector = newSelector(ok, ko);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class,
      new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder())), true, null));

    assertThat(extensions).hasSize(1);
    assertThat(extensions.get(0)).isInstanceOf(CheckProjectOK.class);
  }

  @Test
  public void inheritAnnotations() {
    BatchExtension a = new SubClass("foo");
    BatchExtension b = new MethodDependentOf("foo");

    ScannerExtensionDictionnary selector = newSelector(b, a);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);

    // change initial order
    selector = newSelector(a, b);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true, null));

    assertThat(extensions).hasSize(2);
    assertThat(extensions.get(0)).isEqualTo(a);
    assertThat(extensions.get(1)).isEqualTo(b);
  }

  @Test(expected = IllegalStateException.class)
  public void annotatedMethodsCanNotBePrivate() {
    ScannerExtensionDictionnary selector = newSelector();
    BatchExtension wrong = new BatchExtension() {
      @DependsUpon
      private Object foo() {
        return "foo";
      }
    };
    selector.evaluateAnnotatedClasses(wrong, DependsUpon.class);
  }

  @Test
  public void dependsUponPhaseForNewSensors() {
    PreSensor pre = new PreSensor();
    NormalSensor normal = new NormalSensor();
    PostSensor post = new PostSensor();

    ScannerExtensionDictionnary selector = newSelector(normal, post, pre);
    List<org.sonar.api.batch.sensor.Sensor> extensions = Lists.newArrayList(selector.select(org.sonar.api.batch.sensor.Sensor.class, null, true, null));
    assertThat(extensions).containsExactly(pre, normal, post);

    List<Sensor> oldExtensions = Lists.newArrayList(selector.select(Sensor.class, null, true, null));
    assertThat(oldExtensions).extracting("wrappedSensor").containsExactly(pre, normal, post);
  }

  @Test
  public void dependsUponPhaseForNewPostJob() {
    PrePostJob pre = new PrePostJob();
    NormalPostJob normal = new NormalPostJob();

    ScannerExtensionDictionnary selector = newSelector(normal, pre);
    List<org.sonar.api.batch.postjob.PostJob> extensions = Lists.newArrayList(selector.select(org.sonar.api.batch.postjob.PostJob.class, null, true, null));
    assertThat(extensions).containsExactly(pre, normal);

    List<PostJob> oldExtensions = Lists.newArrayList(selector.select(PostJob.class, null, true, null));
    assertThat(oldExtensions).extracting("wrappedPostJob").containsExactly(pre, normal);
  }

  @Test
  public void dependsUponInheritedPhase() {
    PreSensorSubclass pre = new PreSensorSubclass();
    NormalSensor normal = new NormalSensor();
    PostSensorSubclass post = new PostSensorSubclass();

    ScannerExtensionDictionnary selector = newSelector(normal, post, pre);
    List extensions = Lists.newArrayList(selector.select(org.sonar.api.batch.sensor.Sensor.class, null, true, null));

    assertThat(extensions).containsExactly(pre, normal, post);
  }

  @Test
  public void buildStatusCheckersAreExecutedAfterOtherPostJobs() {
    BuildBreaker checker = new BuildBreaker() {
      public void executeOn(Project project, SensorContext context) {
      }
    };

    ScannerExtensionDictionnary selector = newSelector(new FakePostJob(), checker, new FakePostJob());
    List extensions = Lists.newArrayList(selector.select(PostJob.class, null, true, null));

    assertThat(extensions).hasSize(3);
    assertThat(extensions.get(2)).isEqualTo(checker);
  }

  @Test
  public void selectSensors() {
    FakeSensor oldSensor = new FakeSensor();
    FakeNewSensor nonGlobalSensor = new FakeNewSensor();
    FakeNewGlobalSensor globalSensor = new FakeNewGlobalSensor();
    ScannerExtensionDictionnary selector = newSelector(oldSensor, nonGlobalSensor, globalSensor);

    // verify non-global sensors
    Collection<Sensor> extensions = selector.selectSensors(null, false);
    assertThat(extensions).hasSize(2);
    assertThat(extensions).contains(oldSensor);
    extensions.remove(oldSensor);
    assertThat(extensions).extracting("wrappedSensor").containsExactly(nonGlobalSensor);

    // verify global sensors
    extensions = selector.selectSensors(null, true);
    assertThat(extensions).extracting("wrappedSensor").containsExactly(globalSensor);
  }

  class FakeSensor implements Sensor {

    public void analyse(Project project, SensorContext context) {

    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  class FakeNewSensor implements org.sonar.api.batch.sensor.Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext context) {
    }

  }

  class FakeNewGlobalSensor implements org.sonar.api.batch.sensor.Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
      descriptor.global();
    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext context) {
    }

  }

  class MethodDependentOf implements BatchExtension {
    private Object dep;

    MethodDependentOf(Object o) {
      this.dep = o;
    }

    @DependsUpon
    public Object dependsUponObject() {
      return dep;
    }
  }

  @DependsUpon("flag")
  class ClassDependsUpon implements BatchExtension {
  }

  @DependedUpon("flag")
  class ClassDependedUpon implements BatchExtension {
  }

  @DependsUpon("flag")
  interface InterfaceDependsUpon extends BatchExtension {
  }

  @DependedUpon("flag")
  interface InterfaceDependedUpon extends BatchExtension {
  }

  class GeneratesSomething implements BatchExtension {
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

  class NormalSensor implements org.sonar.api.batch.sensor.Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext context) {
    }

  }

  @Phase(name = Phase.Name.PRE)
  class PreSensor implements org.sonar.api.batch.sensor.Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext context) {
    }

  }

  class PreSensorSubclass extends PreSensor {

  }

  @Phase(name = Phase.Name.POST)
  class PostSensor implements org.sonar.api.batch.sensor.Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
    }

    @Override
    public void execute(org.sonar.api.batch.sensor.SensorContext context) {
    }

  }

  class PostSensorSubclass extends PostSensor {

  }

  class CheckProjectOK implements BatchExtension, CheckProject {
    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  class CheckProjectKO implements BatchExtension, CheckProject {
    public boolean shouldExecuteOnProject(Project project) {
      return false;
    }
  }

  private class FakePostJob implements PostJob {
    public void executeOn(Project project, SensorContext context) {
    }
  }

  class NormalPostJob implements org.sonar.api.batch.postjob.PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }

  }

  @Phase(name = Phase.Name.PRE)
  class PrePostJob implements org.sonar.api.batch.postjob.PostJob {

    @Override
    public void describe(PostJobDescriptor descriptor) {
    }

    @Override
    public void execute(PostJobContext context) {
    }

  }

}
