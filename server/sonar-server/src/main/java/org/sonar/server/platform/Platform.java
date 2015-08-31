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

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.servlet.ServletContext;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.version.DatabaseVersion;
import org.sonar.server.platform.platformlevel.PlatformLevel;
import org.sonar.server.platform.platformlevel.PlatformLevel1;
import org.sonar.server.platform.platformlevel.PlatformLevel2;
import org.sonar.server.platform.platformlevel.PlatformLevel3;
import org.sonar.server.platform.platformlevel.PlatformLevel4;
import org.sonar.server.platform.platformlevel.PlatformLevelSafeMode;
import org.sonar.server.platform.platformlevel.PlatformLevelStartup;

/**
 * @since 2.2
 */
public class Platform {

  private static final Logger LOGGER = Loggers.get(Platform.class);

  private static final Platform INSTANCE = new Platform();

  private Properties properties;
  private ServletContext servletContext;
  private PlatformLevel level1;
  private PlatformLevel level2;
  private PlatformLevel levelSafeMode;
  private PlatformLevel level3;
  private PlatformLevel level4;
  private PlatformLevel currentLevel;
  private boolean dbConnected = false;
  private boolean started = false;
  private final List<Object> level4AddedComponents = Lists.newArrayList();
  private final Profiler profiler = Profiler.createIfTrace(Loggers.get(Platform.class));

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
    this.properties = properties;
    this.servletContext = servletContext;
    if (!dbConnected) {
      startLevel1Container();
      startLevel2Container();
      currentLevel = level2;
      dbConnected = true;
    }
  }

  // Platform is injected in Pico, so do not rename this method "start"
  public void doStart() {
    doStart(Startup.ALL);
  }

  protected void doStart(Startup startup) {
    if (started && !isInSafeMode()) {
      return;
    }

    if (requireSafeMode()) {
      LOGGER.info("DB needs migration, entering safe mode");
      startSafeModeContainer();
      currentLevel = levelSafeMode;
      started = true;
    } else {
      startLevel34Containers();
      executeStartupTasks(startup);
      // switch current container last to avoid giving access to a partially initialized container
      currentLevel = level4;
      started = true;

      // stop safemode container if it existed
      stopSafeModeContainer();
    }
  }

  public void restart() {
    restart(Startup.ALL);
  }

  protected void restart(Startup startup) {

    // switch currentLevel on level1 now to avoid exposing a container in the process of stopping
    currentLevel = level1;

    // stop containers
    stopSafeModeContainer();
    stopLevel234Containers();

    // no need to initialize database connection, so level 1 is skipped
    startLevel2Container();
    startLevel34Containers();
    currentLevel = level4;
    executeStartupTasks(startup);
  }

  private boolean requireSafeMode() {
    return getDatabaseStatus() != DatabaseVersion.Status.UP_TO_DATE;
  }

  public boolean isStarted() {
    return status() == Status.UP;
  }

  public boolean isInSafeMode() {
    return status() == Status.SAFEMODE;
  }

  public Status status() {
    if (!started) {
      return Status.BOOTING;
    }
    if (levelSafeMode != null && currentLevel == levelSafeMode) {
      return Status.SAFEMODE;
    }
    if (currentLevel == level4) {
      return Status.UP;
    }
    return Status.BOOTING;
  }

  /**
   * Starts level 1
   */
  private void startLevel1Container() {
    level1 = start(new PlatformLevel1(this, properties, servletContext));
  }

  /**
   * Starts level 2
   */
  private void startLevel2Container() {
    level2 = start(new PlatformLevel2(level1));
  }

  /**
   * Starts level 3 and 4
   */
  private void startLevel34Containers() {
    level3 = start(new PlatformLevel3(level2));
    level4 = start(new PlatformLevel4(level3, level4AddedComponents));
  }

  public void executeStartupTasks() {
    executeStartupTasks(Startup.ALL);
  }

  private void executeStartupTasks(Startup startup) {
    if (startup.ordinal() >= Startup.ALL.ordinal()) {
      new PlatformLevelStartup(level4)
        .configure()
        .start()
        .stop()
        .destroy();
    }
  }

  private void startSafeModeContainer() {
    levelSafeMode = start(new PlatformLevelSafeMode(level2));
  }

  private PlatformLevel start(PlatformLevel platformLevel) {
    profiler.start();
    platformLevel.configure();
    profiler.stopTrace(String.format("%s configured", platformLevel.getName()));
    profiler.start();
    platformLevel.start();
    profiler.stopTrace(String.format("%s started", platformLevel.getName()));

    return platformLevel;
  }

  /**
   * Stops level 1
   */
  private void stopLevel1Container() {
    if (level1 != null) {
      level1.stop();
      level1 = null;
    }
  }

  /**
   * Stops level 2, 3 and 4 containers cleanly if they exists.
   * Call this method before {@link #startLevel1Container()} to avoid duplicate attempt to stop safemode container
   * components (since calling stop on a container calls stop on its children too, see
   * {@link ComponentContainer#stopComponents()}).
   */
  private void stopLevel234Containers() {
    if (level2 != null) {
      level2.stop();
      level2 = null;
      level3 = null;
      level4 = null;
    }
  }

  /**
   * Stops safemode container cleanly if it exists.
   * Call this method before {@link #stopLevel234Containers()} and {@link #stopLevel1Container()} to avoid duplicate
   * attempt to stop safemode container components (since calling stop on a container calls stops on its children too,
   * see {@link ComponentContainer#stopComponents()}).
   */
  private void stopSafeModeContainer() {
    if (levelSafeMode != null) {
      levelSafeMode.stop();
      levelSafeMode = null;
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
      currentLevel = null;
      dbConnected = false;
      started = false;
    } catch (Exception e) {
      LOGGER.error("Fail to stop server - ignored", e);
    }
  }

  public void addComponents(Collection<?> components) {
    level4AddedComponents.addAll(components);
  }

  public ComponentContainer getContainer() {
    return currentLevel.getContainer();
  }

  public Object getComponent(Object key) {
    return getContainer().getComponentByKey(key);
  }

  public enum Status {
    BOOTING, SAFEMODE, UP
  }

  public enum Startup {
    NO_STARTUP_TASKS, ALL
  }
}
