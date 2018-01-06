/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.platformlevel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;
import org.sonar.server.platform.WebServer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

public abstract class PlatformLevel {
  private final String name;
  @Nullable
  private final PlatformLevel parent;
  private final ComponentContainer container;
  private AddIfStartupLeader addIfStartupLeader;
  private AddIfCluster addIfCluster;
  private AddIfStandalone addIfStandalone;

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

  protected <T> T get(Class<T> tClass) {
    return requireNonNull(container.getComponentByType(tClass));
  }

  protected <T> List<T> getAll(Class<T> tClass) {
    return container.getComponentsByType(tClass);
  }

  protected <T> Optional<T> getOptional(Class<T> tClass) {
    return Optional.ofNullable(container.getComponentByType(tClass));
  }

  protected void add(Object... objects) {
    for (Object object : objects) {
      if (object != null) {
        container.addComponent(object, true);
      }
    }
  }

  /**
   * Add a component to container only if the web server is startup leader.
   *
   * @throws IllegalStateException if called from PlatformLevel1, when cluster settings are not loaded
   */
  AddIfStartupLeader addIfStartupLeader(Object... objects) {
    if (addIfStartupLeader == null) {
      this.addIfStartupLeader = new AddIfStartupLeader(getWebServer().isStartupLeader());
    }
    addIfStartupLeader.ifAdd(objects);
    return addIfStartupLeader;
  }

  /**
   * Add a component to container only if clustering is enabled.
   *
   * @throws IllegalStateException if called from PlatformLevel1, when cluster settings are not loaded
   */
  AddIfCluster addIfCluster(Object... objects) {
    if (addIfCluster == null) {
      addIfCluster = new AddIfCluster(!getWebServer().isStandalone());
    }
    addIfCluster.ifAdd(objects);
    return addIfCluster;
  }

  /**
   * Add a component to container only if this is a standalone instance, without clustering.
   *
   * @throws IllegalStateException if called from PlatformLevel1, when cluster settings are not loaded
   */
  AddIfStandalone addIfStandalone(Object... objects) {
    if (addIfStandalone == null) {
      addIfStandalone = new AddIfStandalone(getWebServer().isStandalone());
    }
    addIfStandalone.ifAdd(objects);
    return addIfStandalone;
  }

  private WebServer getWebServer() {
    return getOptional(WebServer.class)
      .orElseThrow(() -> new IllegalStateException("WebServer not available in Pico yet"));
  }

  private abstract class AddIf {
    private final boolean condition;

    private AddIf(boolean condition) {
      this.condition = condition;
    }

    public void ifAdd(Object... objects) {
      if (condition) {
        PlatformLevel.this.add(objects);
      }
    }

    public void otherwiseAdd(Object... objects) {
      if (!condition) {
        PlatformLevel.this.add(objects);
      }
    }
  }

  public final class AddIfStartupLeader extends AddIf {
    private AddIfStartupLeader(boolean condition) {
      super(condition);
    }
  }

  public final class AddIfCluster extends AddIf {
    private AddIfCluster(boolean condition) {
      super(condition);
    }
  }

  public final class AddIfStandalone extends AddIf {
    private AddIfStandalone(boolean condition) {
      super(condition);
    }
  }

  protected void addAll(Collection<?> objects) {
    add(objects.toArray(new Object[objects.size()]));
  }

}
