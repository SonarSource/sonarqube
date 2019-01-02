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
package org.sonar.core.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefinitions;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ComponentContainerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldRegisterItself() {
    ComponentContainer container = new ComponentContainer();
    assertThat(container.getComponentByType(ComponentContainer.class)).isSameAs(container);
  }

  @Test
  public void should_start_and_stop() {
    ComponentContainer container = spy(new ComponentContainer());
    container.addSingleton(StartableStoppableComponent.class);
    container.startComponents();

    assertThat(container.getComponentByType(StartableStoppableComponent.class).started).isTrue();
    assertThat(container.getComponentByType(StartableStoppableComponent.class).stopped).isFalse();
    verify(container).doBeforeStart();
    verify(container).doAfterStart();

    container.stopComponents();
    assertThat(container.getComponentByType(StartableStoppableComponent.class).stopped).isTrue();
  }

  @Test
  public void should_start_and_stop_hierarchy_of_containers() {
    StartableStoppableComponent parentComponent = new StartableStoppableComponent();
    final StartableStoppableComponent childComponent = new StartableStoppableComponent();
    ComponentContainer parentContainer = new ComponentContainer() {
      @Override
      public void doAfterStart() {
        ComponentContainer childContainer = new ComponentContainer(this);
        childContainer.add(childComponent);
        childContainer.execute();
      }
    };
    parentContainer.add(parentComponent);
    parentContainer.execute();
    assertThat(parentComponent.started).isTrue();
    assertThat(parentComponent.stopped).isTrue();
    assertThat(childComponent.started).isTrue();
    assertThat(childComponent.stopped).isTrue();
  }

  @Test
  public void should_stop_hierarchy_of_containers_on_failure() {
    StartableStoppableComponent parentComponent = new StartableStoppableComponent();
    final StartableStoppableComponent childComponent1 = new StartableStoppableComponent();
    final UnstartableComponent childComponent2 = new UnstartableComponent();
    ComponentContainer parentContainer = new ComponentContainer() {
      @Override
      public void doAfterStart() {
        ComponentContainer childContainer = new ComponentContainer(this);
        childContainer.add(childComponent1);
        childContainer.add(childComponent2);
        childContainer.execute();
      }
    };
    parentContainer.add(parentComponent);
    try {
      parentContainer.execute();
      fail();
    } catch (Exception e) {
      assertThat(parentComponent.started).isTrue();
      assertThat(parentComponent.stopped).isTrue();
      assertThat(childComponent1.started).isTrue();
      assertThat(childComponent1.stopped).isTrue();
    }
  }

  @Test
  public void testChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    child.addSingleton(StartableStoppableComponent.class);
    child.startComponents();

    assertThat(child.getParent()).isSameAs(parent);
    assertThat(parent.getChildren()).containsOnly(child);
    assertThat(child.getComponentByType(ComponentContainer.class)).isSameAs(child);
    assertThat(parent.getComponentByType(ComponentContainer.class)).isSameAs(parent);
    assertThat(child.getComponentByType(StartableStoppableComponent.class)).isNotNull();
    assertThat(parent.getComponentByType(StartableStoppableComponent.class)).isNull();

    parent.stopComponents();
  }

  @Test
  public void testRemoveChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    assertThat(parent.getChildren()).containsOnly(child);

    parent.removeChild(child);
    assertThat(parent.getChildren()).isEmpty();
  }

  @Test
  public void support_multiple_children() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();
    ComponentContainer child1 = parent.createChild();
    child1.startComponents();
    ComponentContainer child2 = parent.createChild();
    child2.startComponents();
    assertThat(parent.getChildren()).containsOnly(child1, child2);

    child1.stopComponents();
    assertThat(parent.getChildren()).containsOnly(child2);

    parent.stopComponents();
    assertThat(parent.getChildren()).isEmpty();
  }

  @Test
  public void shouldForwardStartAndStopToDescendants() {
    ComponentContainer grandParent = new ComponentContainer();
    ComponentContainer parent = grandParent.createChild();
    ComponentContainer child = parent.createChild();
    child.addSingleton(StartableStoppableComponent.class);

    grandParent.startComponents();

    StartableStoppableComponent component = child.getComponentByType(StartableStoppableComponent.class);
    assertTrue(component.started);

    parent.stopComponents();
    assertTrue(component.stopped);
  }

  @Test
  public void shouldDeclareComponentProperties() {
    ComponentContainer container = new ComponentContainer();
    container.addSingleton(ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(propertyDefinitions.get("foo").defaultValue()).isEqualTo("bar");
  }

  @Test
  public void shouldDeclareExtensionWithoutAddingIt() {
    ComponentContainer container = new ComponentContainer();
    PluginInfo plugin = mock(PluginInfo.class);
    container.declareExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNull();
  }

  @Test
  public void shouldDeclareExtensionWhenAdding() {
    ComponentContainer container = new ComponentContainer();
    PluginInfo plugin = mock(PluginInfo.class);
    container.addExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNotNull();
    assertThat(container.getComponentByKey(ComponentWithProperty.class)).isNotNull();
  }

  @Test
  public void test_add_class() {
    ComponentContainer container = new ComponentContainer();
    container.add(ComponentWithProperty.class, SimpleComponent.class);
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNotNull();
    assertThat(container.getComponentByType(SimpleComponent.class)).isNotNull();
  }

  @Test
  public void test_add_collection() {
    ComponentContainer container = new ComponentContainer();
    container.add(Arrays.asList(ComponentWithProperty.class, SimpleComponent.class));
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNotNull();
    assertThat(container.getComponentByType(SimpleComponent.class)).isNotNull();
  }

  @Test
  public void test_add_adapter() {
    ComponentContainer container = new ComponentContainer();
    container.add(new SimpleComponentProvider());
    assertThat(container.getComponentByType(SimpleComponent.class)).isNotNull();
  }

  @Test
  public void should_sanitize_pico_exception_on_start_failure() {
    ComponentContainer container = new ComponentContainer();
    container.add(UnstartableComponent.class);

    // do not expect a PicoException
    thrown.expect(IllegalStateException.class);
    container.startComponents();
  }

  @Test
  public void display_plugin_name_when_failing_to_add_extension() {
    ComponentContainer container = new ComponentContainer();
    PluginInfo plugin = mock(PluginInfo.class);

    container.startComponents();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to register extension org.sonar.core.platform.ComponentContainerTest$UnstartableComponent");

    container.addExtension(plugin, UnstartableComponent.class);

  }

  @Test
  public void test_start_failure() {
    ComponentContainer container = new ComponentContainer();
    StartableStoppableComponent startable = new StartableStoppableComponent();
    container.add(startable, UnstartableComponent.class);

    try {
      container.execute();
      fail();
    } catch (Exception e) {
      assertThat(startable.started).isTrue();

      // container stops the components that have already been started
      assertThat(startable.stopped).isTrue();
    }
  }

  @Test
  public void stop_container_does_not_fail_and_all_stoppable_components_are_stopped_even_if_one_or_more_stop_method_call_fail() {
    ComponentContainer container = new ComponentContainer();
    container.add(FailingStopWithISEComponent.class, FailingStopWithISEComponent2.class, FailingStopWithErrorComponent.class, FailingStopWithErrorComponent2.class);
    container.startComponents();
    StartableStoppableComponent[] components = {
      container.getComponentByType(FailingStopWithISEComponent.class),
      container.getComponentByType(FailingStopWithISEComponent2.class),
      container.getComponentByType(FailingStopWithErrorComponent.class),
      container.getComponentByType(FailingStopWithErrorComponent2.class)
    };

    container.stopComponents();

    Arrays.stream(components).forEach(startableComponent -> assertThat(startableComponent.stopped).isTrue());
  }

  @Test
  public void stop_container_stops_all_stoppable_components_even_in_case_of_OOM_in_any_stop_method() {
    ComponentContainer container = new ComponentContainer();
    container.add(FailingStopWithOOMComponent.class, FailingStopWithOOMComponent2.class);
    container.startComponents();
    StartableStoppableComponent[] components = {
      container.getComponentByType(FailingStopWithOOMComponent.class),
      container.getComponentByType(FailingStopWithOOMComponent2.class)
    };

  }

  @Test
  public void stop_exception_should_not_hide_start_exception() {
    ComponentContainer container = new ComponentContainer();
    container.add(UnstartableComponent.class, FailingStopWithISEComponent.class);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to start");
    container.execute();
  }

  @Test
  public void should_execute_components() {
    ComponentContainer container = new ComponentContainer();
    StartableStoppableComponent component = new StartableStoppableComponent();
    container.add(component);

    container.execute();

    assertThat(component.started).isTrue();
    assertThat(component.stopped).isTrue();
  }

  /**
   * Method close() must be called even if the methods start() or stop()
   * are not defined.
   */
  @Test
  public void should_close_components_without_lifecycle() {
    ComponentContainer container = new ComponentContainer();
    CloseableComponent component = new CloseableComponent();
    container.add(component);

    container.execute();

    assertThat(component.isClosed).isTrue();
  }

  /**
   * Method close() must be executed after stop()
   */
  @Test
  public void should_close_components_with_lifecycle() {
    ComponentContainer container = new ComponentContainer();
    StartableCloseableComponent component = new StartableCloseableComponent();
    container.add(component);

    container.execute();

    assertThat(component.isStopped).isTrue();
    assertThat(component.isClosed).isTrue();
    assertThat(component.isClosedAfterStop).isTrue();
  }

  public static class StartableStoppableComponent {
    public boolean started = false;
    public boolean stopped = false;

    public void start() {
      started = true;
    }

    public void stop() {
      stopped = true;
    }
  }

  public static class UnstartableComponent {
    public void start() {
      throw new IllegalStateException("Fail to start");
    }

    public void stop() {

    }
  }

  public static class FailingStopWithISEComponent extends StartableStoppableComponent {
    public void stop() {
      super.stop();
      throw new IllegalStateException("Faking IllegalStateException thrown by stop method of " + getClass().getSimpleName());
    }
  }

  public static class FailingStopWithISEComponent2 extends FailingStopWithErrorComponent {
  }

  public static class FailingStopWithErrorComponent extends StartableStoppableComponent {
    public void stop() {
      super.stop();
      throw new Error("Faking Error thrown by stop method of " + getClass().getSimpleName());
    }
  }

  public static class FailingStopWithErrorComponent2 extends FailingStopWithErrorComponent {
  }

  public static class FailingStopWithOOMComponent extends StartableStoppableComponent {
    public void stop() {
      super.stop();
      consumeAvailableMemory();
    }

    private static List<Object> consumeAvailableMemory() {
      List<Object> holder = new ArrayList<>();
      while (true) {
        holder.add(new byte[128 * 1024]);
      }
    }
  }

  public static class FailingStopWithOOMComponent2 extends FailingStopWithOOMComponent {

  }

  @Property(key = "foo", defaultValue = "bar", name = "Foo")
  public static class ComponentWithProperty {

  }

  public static class SimpleComponent {

  }

  public static class SimpleComponentProvider extends ProviderAdapter {
    public SimpleComponent provide() {
      return new SimpleComponent();
    }
  }

  public static class CloseableComponent implements AutoCloseable {
    public boolean isClosed = false;

    @Override
    public void close() throws Exception {
      isClosed = true;
    }
  }

  public static class StartableCloseableComponent implements AutoCloseable {
    public boolean isClosed = false;
    public boolean isStopped = false;
    public boolean isClosedAfterStop = false;

    public void stop() {
      isStopped = true;
    }

    @Override
    public void close() throws Exception {
      isClosed = true;
      isClosedAfterStop = isStopped;
    }
  }
}
