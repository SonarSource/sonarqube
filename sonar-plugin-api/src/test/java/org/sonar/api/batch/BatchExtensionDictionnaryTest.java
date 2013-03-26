/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;

public class BatchExtensionDictionnaryTest {

  private BatchExtensionDictionnary newSelector(BatchExtension... extensions) {
    ComponentContainer iocContainer = new ComponentContainer();
    for (BatchExtension extension : extensions) {
      iocContainer.addSingleton(extension);
    }
    return new BatchExtensionDictionnary(iocContainer);
  }

  @Test
  public void testGetFilteredExtensions() {
    Sensor sensor1 = new FakeSensor(), sensor2 = new FakeSensor();
    Decorator decorator = mock(Decorator.class);

    BatchExtensionDictionnary selector = newSelector(sensor1, sensor2, decorator);
    Collection<Sensor> sensors = selector.select(Sensor.class, null, true);

    assertThat(sensors, hasItem(sensor1));
    assertThat(sensors, hasItem(sensor2));
    assertEquals(2, sensors.size());
  }

  @Test
  public void testGetFilteredExtensionWithExtensionMatcher() {
    final Sensor sensor1 = new FakeSensor(), sensor2 = new FakeSensor();

    BatchExtensionDictionnary selector = newSelector(sensor1, sensor2);
    Collection<Sensor> sensors = selector.select(Sensor.class, null, true, new ExtensionMatcher() {
      @Override
      public boolean accept(Object extension) {
        return extension.equals(sensor1);
      }
    });

    assertThat(sensors, hasItem(sensor1));
    assertEquals(1, sensors.size());
  }

  @Test
  public void shouldSearchInParentContainers() {
    BatchExtension a = new FakeSensor();
    BatchExtension b = new FakeSensor();
    BatchExtension c = new FakeSensor();

    ComponentContainer grandParent = new ComponentContainer();
    grandParent.addSingleton(a);

    ComponentContainer parent = grandParent.createChild();
    parent.addSingleton(b);

    ComponentContainer child = parent.createChild();
    child.addSingleton(c);

    BatchExtensionDictionnary dictionnary = new BatchExtensionDictionnary(child);
    assertThat(dictionnary.select(BatchExtension.class).size(), is(3));
    assertThat(dictionnary.select(BatchExtension.class), hasItems(a, b, c));
  }

  @Test
  public void sortExtensionsByDependency() {
    BatchExtension a = new MethodDependentOf(null);
    BatchExtension b = new MethodDependentOf(a);
    BatchExtension c = new MethodDependentOf(b);

    BatchExtensionDictionnary selector = newSelector(b, c, a);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertThat(extensions.size(), is(3));
    assertThat(extensions.get(0), is(a));
    assertThat(extensions.get(1), is(b));
    assertThat(extensions.get(2), is(c));
  }

  @Test
  public void useMethodAnnotationsToSortExtensions() {
    BatchExtension a = new GeneratesSomething("foo");
    BatchExtension b = new MethodDependentOf("foo");

    BatchExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertThat(extensions.size(), is(2));
    assertThat(extensions.get(0), is(a));
    assertThat(extensions.get(1), is(b));

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(2, extensions.size());
    assertEquals(a, extensions.get(0));
    assertEquals(b, extensions.get(1));
  }

  @Test
  public void useClassAnnotationsToSortExtensions() {
    BatchExtension a = new ClassDependedUpon();
    BatchExtension b = new ClassDependsUpon();

    BatchExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertThat(extensions.size(), is(2));
    assertThat(extensions.get(0), is(a));
    assertThat(extensions.get(1), is(b));

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(2, extensions.size());
    assertEquals(a, extensions.get(0));
    assertEquals(b, extensions.get(1));
  }

  @Test
  public void useClassAnnotationsOnInterfaces() {
    BatchExtension a = new InterfaceDependedUpon() {
    };
    BatchExtension b = new InterfaceDependsUpon() {
    };

    BatchExtensionDictionnary selector = newSelector(a, b);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertThat(extensions.size(), is(2));
    assertThat(extensions.get(0), is(a));
    assertThat(extensions.get(1), is(b));

    // different initial order
    selector = newSelector(b, a);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(2, extensions.size());
    assertEquals(a, extensions.get(0));
    assertEquals(b, extensions.get(1));
  }

  @Test
  public void checkProject() {
    BatchExtension ok = new CheckProjectOK();
    BatchExtension ko = new CheckProjectKO();

    BatchExtensionDictionnary selector = newSelector(ok, ko);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, new Project("key"), true));

    assertThat(extensions.size(), is(1));
    assertThat(extensions.get(0), is(CheckProjectOK.class));
  }

  @Test
  public void inheritAnnotations() {
    BatchExtension a = new SubClass("foo");
    BatchExtension b = new MethodDependentOf("foo");

    BatchExtensionDictionnary selector = newSelector(b, a);
    List<BatchExtension> extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(2, extensions.size());
    assertEquals(a, extensions.get(0));
    assertEquals(b, extensions.get(1));

    // change initial order
    selector = newSelector(a, b);
    extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(2, extensions.size());
    assertEquals(a, extensions.get(0));
    assertEquals(b, extensions.get(1));
  }

  @Test(expected = IllegalStateException.class)
  public void annotatedMethodsCanNotBePrivate() {
    BatchExtensionDictionnary selector = newSelector();
    BatchExtension wrong = new BatchExtension() {
      @DependsUpon
      private Object foo() {
        return "foo";
      }
    };
    selector.evaluateAnnotatedClasses(wrong, DependsUpon.class);
  }

  @Test
  public void dependsUponPhase() {
    BatchExtension pre = new PreSensor();
    BatchExtension analyze = new GeneratesSomething("something");
    BatchExtension post = new PostSensor();

    BatchExtensionDictionnary selector = newSelector(analyze, post, pre);
    List extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(3, extensions.size());
    assertEquals(pre, extensions.get(0));
    assertEquals(analyze, extensions.get(1));
    assertEquals(post, extensions.get(2));
  }

  @Test
  public void dependsUponInheritedPhase() {
    BatchExtension pre = new PreSensorSubclass();
    BatchExtension analyze = new GeneratesSomething("something");
    BatchExtension post = new PostSensorSubclass();

    BatchExtensionDictionnary selector = newSelector(analyze, post, pre);
    List extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(3, extensions.size());
    assertEquals(pre, extensions.get(0));
    assertEquals(analyze, extensions.get(1));
    assertEquals(post, extensions.get(2));
  }

  @Test
  public void buildStatusCheckersAreExecutedAfterOtherPostJobs() {
    BuildBreaker checker = new BuildBreaker() {
      public void executeOn(Project project, SensorContext context) {
      }
    };

    BatchExtensionDictionnary selector = newSelector(new FakePostJob(), checker, new FakePostJob());
    List extensions = Lists.newArrayList(selector.select(BatchExtension.class, null, true));

    assertEquals(3, extensions.size());
    assertEquals(checker, extensions.get(2));
  }

  class FakeSensor implements Sensor {

    public void analyse(Project project, SensorContext context) {

    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
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

  @Phase(name = Phase.Name.PRE)
  class PreSensor implements BatchExtension {

  }

  class PreSensorSubclass extends PreSensor {

  }

  @Phase(name = Phase.Name.POST)
  class PostSensor implements BatchExtension {

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
}
