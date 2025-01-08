/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.NodeInformation;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class PlatformLevel {
  private final String name;
  @Nullable
  protected final PlatformLevel parent;
  private final SpringComponentContainer container;
  private AddIfStartupLeader addIfStartupLeader;
  private AddIfCluster addIfCluster;
  private AddIfStandalone addIfStandalone;

  protected PlatformLevel(String name) {
    this.name = name;
    this.parent = null;
    this.container = createContainer(null);
  }

  protected PlatformLevel(String name, PlatformLevel parent) {
    this.name = checkNotNull(name);
    this.parent = checkNotNull(parent);
    this.container = createContainer(parent.getContainer());
  }

  public SpringComponentContainer getContainer() {
    return container;
  }

  public String getName() {
    return name;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  protected SpringComponentContainer createContainer(@Nullable SpringComponentContainer parent) {
    if (parent == null) {
      return new SpringComponentContainer();
    }
    return parent.createChild();
  }

  public PlatformLevel configure() {
    configureLevel();
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

  protected <T> T get(Class<T> tClass) {
    return container.getComponentByType(tClass);
  }

  protected <T> Optional<T> getOptional(Class<T> tClass) {
    return container.getOptionalComponentByType(tClass);
  }

  protected void add(Object... objects) {
    for (Object object : objects) {
      if (object != null) {
        container.add(object);
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

  protected NodeInformation getWebServer() {
    return Optional.ofNullable(parent)
      .flatMap(p -> p.getOptional(NodeInformation.class))
      .or(() -> getOptional(NodeInformation.class))
      .orElseThrow(() -> new IllegalStateException("WebServer not available in the container"));
  }

  protected abstract class AddIf {
    private final boolean condition;

    protected AddIf(boolean condition) {
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
