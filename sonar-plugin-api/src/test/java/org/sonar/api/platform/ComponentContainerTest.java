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
package org.sonar.api.platform;

import org.junit.Test;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefinitions;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ComponentContainerTest {

  @Test
  public void shouldRegisterItself() {
    ComponentContainer container = new ComponentContainer();
    assertTrue(container.getComponentByType(ComponentContainer.class) == container);
  }

  @Test
  public void testStartAndStop() {
    ComponentContainer container = new ComponentContainer();
    container.addSingleton(StartableComponent.class);
    container.startComponents();

    assertThat(container.getComponentByType(StartableComponent.class).started, is(true));
    assertThat(container.getComponentByType(StartableComponent.class).stopped, is(false));

    container.stopComponents();
    assertThat(container.getComponentByType(StartableComponent.class).stopped, is(true));
  }

  @Test
  public void testChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    child.addSingleton(StartableComponent.class);
    child.startComponents();

    assertTrue(child.getParent() == parent);
    assertTrue(parent.getChild() == child);
    assertTrue(child.getComponentByType(ComponentContainer.class) == child);
    assertTrue(parent.getComponentByType(ComponentContainer.class) == parent);
    assertThat(child.getComponentByType(StartableComponent.class), notNullValue());
    assertThat(parent.getComponentByType(StartableComponent.class), nullValue());

    parent.stopComponents();
  }

  @Test
  public void testRemoveChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    assertTrue(parent.getChild() == child);

    parent.removeChild();
    assertThat(parent.getChild(), nullValue());
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
    assertThat(propertyDefinitions.getProperty("foo"), notNullValue());
    assertThat(propertyDefinitions.getProperty("foo").defaultValue(), is("bar"));
  }

  @Test
  public void shouldDeclareExtensionWithoutAddingIt() {
    ComponentContainer container = new ComponentContainer();
    PluginMetadata plugin = mock(PluginMetadata.class);
    container.declareExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.getProperty("foo"), notNullValue());
    assertThat(container.getComponentByType(ComponentWithProperty.class), nullValue());
  }

  @Test
  public void shouldDeclareExtensionWhenAdding() {
    ComponentContainer container = new ComponentContainer();
    PluginMetadata plugin = mock(PluginMetadata.class);
    container.addExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.getProperty("foo"), notNullValue());
    assertThat(container.getComponentByType(ComponentWithProperty.class), notNullValue());
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

  @Property(key = "foo", defaultValue = "bar", name = "Foo")
  public static class ComponentWithProperty {

  }
}
