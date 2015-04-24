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

import org.sonar.core.platform.ComponentContainer;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
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

  private static final Logger LOGGER = Loggers.get(Platform.class);

  private static final Platform INSTANCE = new Platform();

  private ServerComponents serverComponents;
  private ComponentContainer level1Container, level2Container, safeModeContainer, level3Container, level4Container;
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
      currentContainer = level2Container;
      dbConnected = true;
    }
  }

  // Platform is injected in Pico, so do not rename this method "start"
  public void doStart() {
    if (started && !isInSafeMode()) {
      return;
    }

    if (requireSafeMode()) {
      LOGGER.info("DB needs migration, entering safe mode");
      startSafeModeContainer();
      currentContainer = safeModeContainer;
      started = true;
    } else {
      startLevel34Containers();
      executeStartupTasks();
      // switch current container last to avoid giving access to a partially initialized container
      currentContainer = level4Container;
      started = true;

      // stop safemode container if it existed
      stopSafeModeContainer();
    }
  }

  public void restart() {
    // switch currentContainer on level1 now to avoid exposing a container in the process of stopping
    currentContainer = level1Container;

    // stop containers
    stopSafeModeContainer();
    stopLevel234Containers();

    // no need to initialize database connection, so level 1 is skipped
    startLevel2Container();
    startLevel34Containers();
    executeStartupTasks();
    currentContainer = level4Container;
  }

  private boolean requireSafeMode() {
    return getDatabaseStatus() != DatabaseVersion.Status.UP_TO_DATE;
  }

  public boolean isStarted() {
    return started && !isInSafeMode();
  }

  public boolean isInSafeMode() {
    return started && safeModeContainer != null && currentContainer == safeModeContainer;
  }

  /**
   * Starts level 1
   */
  private void startLevel1Container() {
    level1Container = new ComponentContainer();
    level1Container.addSingletons(serverComponents.level1Components());
    level1Container.startComponents();
  }

  /**
   * Starts level 2
   */
  private void startLevel2Container() {
    level2Container = level1Container.createChild();
    level2Container.addSingletons(serverComponents.level2Components());
    level2Container.startComponents();
  }

  /**
   * Starts level 3 and 4
   */
  private void startLevel34Containers() {
    level3Container = level2Container.createChild();
    level3Container.addSingletons(serverComponents.level3Components());
    level3Container.startComponents();

    level4Container = level3Container.createChild();
    serverComponents.startLevel4Components(level4Container);
  }

  public void executeStartupTasks() {
    serverComponents.executeStartupTasks(level4Container);
  }

  private void startSafeModeContainer() {
    safeModeContainer = level2Container.createChild();
    safeModeContainer.addSingletons(serverComponents.safeModeComponents());
    safeModeContainer.startComponents();
  }

  /**
   * Stops level 1
   */
  private void stopLevel1Container() {
    if (level1Container != null) {
      level1Container.stopComponents();
      level1Container = null;
    }
  }

  /**
   * Stops level 2, 3 and 3 containers cleanly if they exists.
   * Call this method before {@link #startLevel1Container()} to avoid duplicate attempt to stop safemode container
   * components (since calling stop on a container calls stop on its children too, see
   * {@link ComponentContainer#stopComponents()}).
   */
  private void stopLevel234Containers() {
    if (level2Container != null) {
      level2Container.stopComponents();
      level2Container = null;
      level3Container = null;
      level4Container = null;
    }
  }

  /**
   * Stops safemode container cleanly if it exists.
   * Call this method before {@link #stopLevel234Containers()} and {@link #stopLevel1Container()} to avoid duplicate
   * attempt to stop safemode container components (since calling stop on a container calls stops on its children too,
   * see {@link ComponentContainer#stopComponents()}).
   */
  private void stopSafeModeContainer() {
    if (safeModeContainer != null) {
      safeModeContainer.stopComponents();
      safeModeContainer = null;
    }
  }

  private DatabaseVersion.Status getDatabaseStatus() {
    DatabaseVersion version = getContainer().getComponentByType(DatabaseVersion.class);
    return version.getStatus();
  }

  // Do not rename "stop"
  public void doStop() {
    try {
      stopSafeModeContainer();
      stopLevel234Containers();
      stopLevel1Container();
      currentContainer = null;
      dbConnected = false;
      started = false;
    } catch (Exception e) {
      LOGGER.debug("Fail to stop server - ignored", e);
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
