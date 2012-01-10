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
package org.sonar.batch.bootstrap;

import org.picocontainer.ComponentAdapter;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;

import java.util.List;

/**
 * Module describes group of components - {@link #configure()}.
 * Several modules can be grouped together - {@link #install(Module)}, {@link #installChild(Module)}.
 * <p/>
 */
public abstract class Module {

  ComponentContainer container;

  /**
   * @return this
   */
  public final Module init() {
    return init(new ComponentContainer());
  }

  /**
   * @return this
   */
  private Module init(ComponentContainer container) {
    this.container = container;
    configure();
    return this;
  }

  /**
   * Installs module into this module.
   *
   * @return this
   */
  public final Module install(Module module) {
    module.init(container);
    return this;
  }

  /**
   * Installs module into new scope - see http://picocontainer.org/scopes.html
   *
   * @return installed module
   */
  public final Module installChild(Module child) {
    ComponentContainer childContainer = container.createChild();
    child.init(childContainer);
    return child;
  }

  public final void uninstallChild() {
    container.removeChild();
  }

  /**
   * @return this
   */
  public final Module start() {
    container.startComponents();
    doStart();
    return this;
  }

  protected void doStart() {
    // empty method to be overridden
  }

  /**
   * @return this
   */
  public final Module stop() {
    try {
      doStop();
      container.stopComponents();
    } catch (Exception e) {
      // ignore
    }
    return this;
  }

  protected void doStop() {
    // empty method to be overridden
  }

  /**
   * Implementation of this method must not contain conditional logic and just should contain several invocations of
   * {@link #addCoreSingleton(Object)}, {@link #addExtension(org.sonar.api.platform.PluginMetadata, Object)} or {@link #addAdapter(ComponentAdapter)}.
   */
  protected abstract void configure();

  protected final void addCoreSingleton(Object component) {
    container.addSingleton(component);
  }

  protected final void declareExtension(PluginMetadata plugin, Object extension) {
    container.declareExtension(plugin, extension);
  }

  protected final void addExtension(PluginMetadata plugin, Object extension) {
    container.addExtension(plugin, extension);
  }

  protected final void addAdapter(ComponentAdapter<?> componentAdapter) {
    container.addPicoAdapter(componentAdapter);
  }

  public final <T> T getComponentByType(Class<T> componentType) {
    return container.getComponentByType(componentType);
  }

  public final Object getComponentByKey(Object key) {
    return container.getComponentByKey(key);
  }

  public final <T> List<T> getComponents(Class<T> componentType) {
    return container.getComponentsByType(componentType);
  }
}
