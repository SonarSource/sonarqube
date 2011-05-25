/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.picocontainer.Characteristics;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.utils.IocContainer;

import java.util.List;

/**
 * Module describes group of components - {@link #configure()}.
 * Several modules can be grouped together - {@link #install(Module)}, {@link #installChild(Module)}.
 * <p/>
 * TODO Move to org.sonar.batch.bootstrap ?
 */
public abstract class Module {

  private MutablePicoContainer container;

  /**
   * @return this
   */
  public final Module init() {
    return init(IocContainer.buildPicoContainer());
  }

  /**
   * @return this
   */
  private Module init(MutablePicoContainer container) {
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
    MutablePicoContainer childContainer = container.makeChildContainer();
    // register container as a component, because it used for example in BatchExtensionDictionnary,
    // but in fact this is anti-pattern - http://picocontainer.codehaus.org/container-dependency-antipattern.html
    childContainer.addComponent(new IocContainer(childContainer));
    childContainer.setName(child.toString());
    child.init(childContainer);
    return child;
  }

  public final void uninstallChild(Module child) {
    container.removeChildContainer(child.container);
  }

  /**
   * @return this
   */
  public final Module start() {
    container.start();
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
      container.stop();
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
   * {@link #addComponent(Object)}, {@link #addComponent(Object, Object)} or {@link #addAdapter(ComponentAdapter)}.
   */
  protected abstract void configure();

  protected final void addComponent(Object component) {
    if (component instanceof Class) {
      container.as(Characteristics.CACHE).addComponent(component);
    } else {
      container.as(Characteristics.CACHE).addComponent(component.getClass().getCanonicalName() + "-" + component.toString(), component);
    }
  }

  protected final void addComponent(Object componentKey, Object component) {
    container.as(Characteristics.CACHE).addComponent(componentKey, component);
  }

  protected final void addAdapter(ComponentAdapter<?> componentAdapter) {
    container.addAdapter(componentAdapter);
  }

  public final <T> T getComponent(Class<T> componentType) {
    return container.getComponent(componentType);
  }

  public final <T> List<T> getComponents(Class<T> componentType) {
    return container.getComponents(componentType);
  }

  /**
   * TODO should not be used and should be removed
   */
  public final MutablePicoContainer getContainer() {
    return container;
  }

}
