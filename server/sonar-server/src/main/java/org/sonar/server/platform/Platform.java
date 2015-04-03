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
package org.sonar.server.platform;

import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.persistence.DatabaseVersion;

import javax.annotation.CheckForNull;
import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Properties;

/**
 * @since 2.2
 */
public class Platform {

  private static final Platform INSTANCE = new Platform();

  private ServerComponents serverComponents;
  private ComponentContainer level1Container, level2Container, level3Container, level4Container;
  private ComponentContainer currentContainer;
  private boolean dbConnected = false;
  private boolean started = false;

  public Platform() {
  }

  public static Platform getInstance() {
    return INSTANCE;
  }

  /**
   * shortcut for ruby code
   */
  public static Server getServer() {
    return (Server) getInstance().getComponent(Server.class);
  }

  /**
   * Used by ruby code
   */
  @CheckForNull
  public static <T> T component(Class<T> type) {
    if (INSTANCE.started) {
      return INSTANCE.getContainer().getComponentByType(type);
    }
    return null;
  }

  public void init(Properties properties, ServletContext servletContext) {
    serverComponents = new ServerComponents(this, properties, servletContext);
    if (!dbConnected) {
      startLevel1Container();
      startLevel2Container();
      dbConnected = true;
    }
  }

  // Platform is injected in Pico, so do not rename this method "start"
  public void doStart() {
    if (!started && getDatabaseStatus() == DatabaseVersion.Status.UP_TO_DATE) {
      startLevel34Containers();
      started = true;
    }
  }

  public boolean isStarted() {
    return started;
  }

  /**
   * Start level 1 only
   */
  private void startLevel1Container() {
    level1Container = new ComponentContainer();
    level1Container.addSingletons(serverComponents.level1Components());
    level1Container.startComponents();
    currentContainer = level1Container;
  }

  /**
   * Start level 2 only
   */
  private void startLevel2Container() {
    level2Container = level1Container.createChild();
    level2Container.addSingletons(serverComponents.level2Components());
    level2Container.startComponents();
    currentContainer = level2Container;
  }

  /**
   * Start level 3 and greater
   */
  private void startLevel34Containers() {
    level3Container = level2Container.createChild();
    level3Container.addSingletons(serverComponents.level3Components());
    level3Container.startComponents();
    currentContainer = level3Container;

    level4Container = level3Container.createChild();
    serverComponents.startLevel4Components(level4Container);
    currentContainer = level4Container;
    executeStartupTasks();
  }

  public void executeStartupTasks() {
    serverComponents.executeStartupTasks(level4Container);
  }

  public void restart() {
    // Do not need to initialize database connection, so level 1 is skipped
    if (level2Container != null) {
      level2Container.stopComponents();
      currentContainer = level1Container;
    }
    startLevel2Container();
    startLevel34Containers();
  }

  private DatabaseVersion.Status getDatabaseStatus() {
    DatabaseVersion version = getContainer().getComponentByType(DatabaseVersion.class);
    return version.getStatus();
  }

  // Do not rename "stop"
  public void doStop() {
    if (level1Container != null) {
      try {
        level1Container.stopComponents();
        level1Container = null;
        currentContainer = null;
        dbConnected = false;
        started = false;
      } catch (Exception e) {
        Loggers.get(getClass()).debug("Fail to stop server - ignored", e);
      }
    }
  }

  public void addComponents(Collection components) {
    serverComponents.addComponents(components);
  }

  public ComponentContainer getContainer() {
    return currentContainer;
  }

  public Object getComponent(Object key) {
    return getContainer().getComponentByKey(key);
  }
}
