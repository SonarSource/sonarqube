/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.api.config.PropertyDefs;

import javax.annotation.Nullable;

import java.util.List;

/**
 * @since 2.12
 */
public class ComponentContainer implements BatchComponent, ServerComponent {

  ComponentContainer parent, child; // no need for multiple children
  MutablePicoContainer pico;
  PropertyDefs propertyDefs;

  /**
   * Create root container
   */
  public ComponentContainer() {
    this.parent = null;
    this.child = null;
    this.pico = createPicoContainer();
    propertyDefs = new PropertyDefs();
    addSingleton(propertyDefs);
    addSingleton(this);
  }

  /**
   * Create child container
   */
  protected ComponentContainer(ComponentContainer parent) {
    this.parent = parent;
    this.pico = parent.pico.makeChildContainer();
    this.parent.child = this;
    this.propertyDefs = parent.propertyDefs;
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

  }

  /**
   * This method aims to be overridden
   */
  protected void doAfterStart() {

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

  public ComponentContainer addSingleton(Object component) {
    return addComponent(component, true);
  }

  /**
   * @param singleton return always the same instance if true, else a new instance
   *                  is returned each time the component is requested
   */
  public ComponentContainer addComponent(Object component, boolean singleton) {
    pico.as(singleton ? Characteristics.CACHE : Characteristics.NO_CACHE).addComponent(getComponentKey(component), component);
    declareExtension(null, component);
    return this;
  }

  public ComponentContainer addExtension(@Nullable PluginMetadata plugin, Object extension) {
    pico.as(Characteristics.CACHE).addComponent(getComponentKey(extension), extension);
    declareExtension(plugin, extension);
    return this;
  }

  public void declareExtension(@Nullable PluginMetadata plugin, Object extension) {
    propertyDefs.addComponent(extension, plugin != null ? plugin.getName() : "");
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

  static MutablePicoContainer createPicoContainer() {
    ReflectionLifecycleStrategy lifecycleStrategy = new ReflectionLifecycleStrategy(new NullComponentMonitor(), "start", "stop", "dispose");
    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, null);
  }

  static Object getComponentKey(Object component) {
    if (component instanceof Class) {
      return component;
    }
    return new StringBuilder().append(component.getClass().getCanonicalName()).append("-").append(component.toString()).toString();
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
