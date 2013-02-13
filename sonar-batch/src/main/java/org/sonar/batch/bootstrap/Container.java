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

import org.sonar.api.platform.ComponentContainer;

/**
 * Container describes group of components - {@link #configure()}.
 * Several containers can be grouped together - {@link #installChild(Container)}.
 * <p/>
 */
public abstract class Container {

  protected ComponentContainer container;

  public ComponentContainer container() {
    return container;
  }

  /**
   * @return this
   */
  public final void init() {
    init(new ComponentContainer());
  }

  /**
   * @return this
   */
  public final void init(ComponentContainer container) {
    this.container = container;
    configure();
  }

  /**
   * Installs container into new scope - see http://picocontainer.org/scopes.html
   *
   * @return installed module
   */
  public final Container installChild(Container child) {
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
  public final Container start() {
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
  public final Container stop() {
    try {
      doStop();
      container.stopComponents();
      container.removeChild();
    } catch (Exception e) {
      // ignore
    }
    return this;
  }

  protected void doStop() {
    // empty method to be overridden
  }

  /**
   * Implementation of this method must not contain conditional logic and just should contain several invocations on
   * {@link #container}.
   */
  protected abstract void configure();

}
