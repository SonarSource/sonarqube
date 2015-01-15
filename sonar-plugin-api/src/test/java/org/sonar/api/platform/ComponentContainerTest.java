/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefinitions;

import java.util.Arrays;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    container.addSingleton(StartableComponent.class);
    container.startComponents();

    assertThat(container.getComponentByType(StartableComponent.class).started).isTrue();
    assertThat(container.getComponentByType(StartableComponent.class).stopped).isFalse();
    verify(container).doBeforeStart();
    verify(container).doAfterStart();

    container.stopComponents();
    assertThat(container.getComponentByType(StartableComponent.class).stopped).isTrue();
  }

  @Test
  public void should_start_and_stop_hierarchy_of_containers() {
    StartableComponent parentComponent = new StartableComponent();
    final StartableComponent childComponent = new StartableComponent();
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
    StartableComponent parentComponent = new StartableComponent();
    final StartableComponent childComponent1 = new StartableComponent();
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
    child.addSingleton(StartableComponent.class);
    child.startComponents();

    assertThat(child.getParent()).isSameAs(parent);
    assertThat(parent.getChild()).isSameAs(child);
    assertThat(child.getComponentByType(ComponentContainer.class)).isSameAs(child);
    assertThat(parent.getComponentByType(ComponentContainer.class)).isSameAs(parent);
    assertThat(child.getComponentByType(StartableComponent.class)).isNotNull();
    assertThat(parent.getComponentByType(StartableComponent.class)).isNull();

    parent.stopComponents();
  }

  @Test
  public void testRemoveChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    assertThat(parent.getChild()).isSameAs(child);

    parent.removeChild();
    assertThat(parent.getChild()).isNull();
  }

  @Test
  public void shouldForwardStartAndStopToDescendants() {
    ComponentContainer grandParent = new ComponentContainer();
    ComponentContainer parent = grandParent.createChild();
    ComponentContainer child = parent.createChild();
    child.addSingleton(StartableComponent.class);

    grandParent.startComponents();

    StartableComponent component = child.getComponentByType(StartableComponent.class);
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
    PluginMetadata plugin = mock(PluginMetadata.class);
    container.declareExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNull();
  }

  @Test
  public void shouldDeclareExtensionWhenAdding() {
    ComponentContainer container = new ComponentContainer();
    PluginMetadata plugin = mock(PluginMetadata.class);
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
    PluginMetadata plugin = mock(PluginMetadata.class);

    container.startComponents();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to register extension org.sonar.api.platform.ComponentContainerTest$UnstartableComponent");

    container.addExtension(plugin, UnstartableComponent.class);

  }

  @Test
  public void test_start_failure() {
    ComponentContainer container = new ComponentContainer();
    StartableComponent startable = new StartableComponent();
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
  public void test_stop_failure() {
    ComponentContainer container = new ComponentContainer();
    StartableComponent startable = new StartableComponent();
    container.add(startable, UnstoppableComponent.class);

    try {
      container.execute();
      fail();
    } catch (Exception e) {
      assertThat(startable.started).isTrue();

      // container should stop the components that have already been started
      // ... but that's not the case
    }
  }

  @Test
  public void stop_exception_should_not_hide_start_exception() {
    ComponentContainer container = new ComponentContainer();
    container.add(UnstartableComponent.class, UnstoppableComponent.class);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to start");
    container.execute();
  }

  @Test
  public void should_execute_components() {
    ComponentContainer container = new ComponentContainer();
    StartableComponent component = new StartableComponent();
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
  public void should_close_components_without_lifecycle() throws Exception {
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
  public void should_close_components_with_lifecycle() throws Exception {
    ComponentContainer container = new ComponentContainer();
    StartableCloseableComponent component = new StartableCloseableComponent();
    container.add(component);

    container.execute();

    assertThat(component.isStopped).isTrue();
    assertThat(component.isClosed).isTrue();
    assertThat(component.isClosedAfterStop).isTrue();
  }

  public static class StartableComponent {
    public boolean started = false, stopped = false;

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

  public static class UnstoppableComponent {
    public void start() {
    }

    public void stop() {
      throw new IllegalStateException("Fail to stop");
    }
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
    public boolean isClosed = false, isStopped = false, isClosedAfterStop = false;

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
