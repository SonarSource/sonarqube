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
package org.sonar.core.platform;

import com.google.common.collect.Iterables;
import org.picocontainer.Characteristics;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.PropertyDefinitions;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class ComponentContainer implements BatchComponent, ServerComponent {

  // no need for multiple children
  ComponentContainer parent, child;
  MutablePicoContainer pico;
  PropertyDefinitions propertyDefinitions;
  ComponentKeys componentKeys;

  /**
   * Create root container
   */
  public ComponentContainer() {
    this.parent = null;
    this.child = null;
    this.pico = createPicoContainer();
    this.componentKeys = new ComponentKeys();
    propertyDefinitions = new PropertyDefinitions();
    addSingleton(propertyDefinitions);
    addSingleton(this);
  }

  /**
   * Create child container
   */
  protected ComponentContainer(ComponentContainer parent) {
    this.parent = parent;
    this.pico = parent.pico.makeChildContainer();
    this.parent.child = this;
    this.propertyDefinitions = parent.propertyDefinitions;
    this.componentKeys = new ComponentKeys();
    addSingleton(this);
  }

  public void execute() {
    boolean threw = true;
    try {
      startComponents();
      threw = false;
    } finally {
      stopComponents(threw);
    }
  }

  /**
   * This method MUST NOT be renamed start() because the container is registered itself in picocontainer. Starting
   * a component twice is not authorized.
   */
  public ComponentContainer startComponents() {
    try {
      doBeforeStart();
      pico.start();
      doAfterStart();
      return this;
    } catch (Exception e) {
      throw PicoUtils.propagate(e);
    }
  }

  /**
   * This method aims to be overridden
   */
  protected void doBeforeStart() {
    // nothing
  }

  /**
   * This method aims to be overridden
   */
  protected void doAfterStart() {
    // nothing
  }

  /**
   * This method MUST NOT be renamed stop() because the container is registered itself in picocontainer. Starting
   * a component twice is not authorized.
   */
  public ComponentContainer stopComponents() {
    return stopComponents(false);
  }

  public ComponentContainer stopComponents(boolean swallowException) {
    try {
      pico.stop();
      pico.dispose();

    } catch (RuntimeException e) {
      if (!swallowException) {
        throw PicoUtils.propagate(e);
      }
    } finally {
      removeChild();
      if (parent != null) {
        parent.removeChild();
      }
    }
    return this;
  }

  /**
   * @since 3.5
   */
  public ComponentContainer add(Object... objects) {
    for (Object object : objects) {
      if (object instanceof ComponentAdapter) {
        addPicoAdapter((ComponentAdapter) object);
      } else if (object instanceof Iterable) {
        add(Iterables.toArray((Iterable) object, Object.class));
      } else {
        addSingleton(object);
      }
    }
    return this;
  }

  public ComponentContainer addSingletons(Collection components) {
    for (Object component : components) {
      addSingleton(component);
    }
    return this;
  }

  public ComponentContainer addSingleton(Object component) {
    return addComponent(component, true);
  }

  /**
   * @param singleton return always the same instance if true, else a new instance
   *                  is returned each time the component is requested
   */
  public ComponentContainer addComponent(Object component, boolean singleton) {
    Object key = componentKeys.of(component);
    if (component instanceof ComponentAdapter) {
      pico.addAdapter((ComponentAdapter) component);
    } else {
      try {
        pico.as(singleton ? Characteristics.CACHE : Characteristics.NO_CACHE).addComponent(key, component);
      } catch (Throwable t) {
        throw new IllegalStateException("Unable to register component " + getName(component), t);
      }
      declareExtension(null, component);
    }
    return this;
  }

  public ComponentContainer addExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    Object key = componentKeys.of(extension);
    try {
      pico.as(Characteristics.CACHE).addComponent(key, extension);
    } catch (Throwable t) {
      throw new IllegalStateException("Unable to register extension " + getName(extension), t);
    }
    declareExtension(pluginInfo, extension);
    return this;
  }

  private String getName(Object extension) {
    if (extension instanceof Class) {
      return ((Class) extension).getName();
    }
    return getName(extension.getClass());
  }

  public void declareExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    propertyDefinitions.addComponent(extension, pluginInfo != null ? pluginInfo.getName() : "");
  }

  public ComponentContainer addPicoAdapter(ComponentAdapter adapter) {
    pico.addAdapter(adapter);
    return this;
  }

  public <T> T getComponentByType(Class<T> tClass) {
    return pico.getComponent(tClass);
  }

  public Object getComponentByKey(Object key) {
    return pico.getComponent(key);
  }

  public <T> List<T> getComponentsByType(Class<T> tClass) {
    return pico.getComponents(tClass);
  }

  public ComponentContainer removeChild() {
    if (child != null) {
      pico.removeChildContainer(child.pico);
      child = null;
    }
    return this;
  }

  public ComponentContainer createChild() {
    return new ComponentContainer(this);
  }

  public static MutablePicoContainer createPicoContainer() {
    ReflectionLifecycleStrategy lifecycleStrategy = new ReflectionLifecycleStrategy(new NullComponentMonitor(), "start", "stop", "close");
    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, null);
  }

  public ComponentContainer getParent() {
    return parent;
  }

  public ComponentContainer getChild() {
    return child;
  }

  public MutablePicoContainer getPicoContainer() {
    return pico;
  }
}
