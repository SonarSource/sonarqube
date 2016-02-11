/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.platformlevel;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class PlatformLevel {
  private final String name;
  @Nullable
  private final PlatformLevel parent;
  private final ComponentContainer container;

  public PlatformLevel(String name) {
    this.name = name;
    this.parent = null;
    this.container = createContainer(null);
  }

  public PlatformLevel(String name, @Nonnull PlatformLevel parent) {
    this.name = checkNotNull(name);
    this.parent = checkNotNull(parent);
    this.container = createContainer(parent.container);
  }

  public ComponentContainer getContainer() {
    return container;
  }

  public String getName() {
    return name;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  protected ComponentContainer createContainer(@Nullable ComponentContainer parent) {
    if (parent == null) {
      return new ComponentContainer();
    }
    return parent.createChild();
  }

  public PlatformLevel configure() {
    configureLevel();

    List<Module> modules = container.getComponentsByType(Module.class);
    for (Module module : modules) {
      module.configure(container);
    }

    return this;
  }

  protected abstract void configureLevel();

  /**
   * Intended to be override by subclasses if needed
   */
  public PlatformLevel start() {
    container.startComponents();

    return this;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  public PlatformLevel stop() {
    container.stopComponents();

    return this;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  public PlatformLevel destroy() {
    if (parent != null) {
      parent.container.removeChild(container);
    }
    return this;
  }

  protected void add(@Nullable Object object, boolean singleton) {
    if (object != null) {
      container.addComponent(object, singleton);
    }
  }

  protected <T> T getComponentByType(Class<T> tClass) {
    return container.getComponentByType(tClass);
  }

  protected void add(Object... objects) {
    for (Object object : objects) {
      if (object != null) {
        container.addComponent(object, true);
      }
    }
  }

  protected void addAll(Collection<?> objects) {
    add(objects.toArray(new Object[objects.size()]));
  }

}
