/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
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

  private final Supplier<AutoStarter> autoStarterSupplier;
  private AutoStarter autoStarter = null;
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

  private Platform() {
    this.autoStarterSupplier = () -> {
      ProcessCommandWrapper processCommandWrapper = getContainer().getComponentByType(ProcessCommandWrapper.class);
      return new AsynchronousAutoStarter(processCommandWrapper);
    };
  }

  protected Platform(Supplier<AutoStarter> autoStarterSupplier) {
    this.autoStarterSupplier = autoStarterSupplier;
  }

  public static Platform getInstance() {
    return INSTANCE;
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

    boolean dbRequiredMigration = dbRequiresMigration();
    startSafeModeContainer();
    currentLevel = levelSafeMode;
    started = true;

    // if AutoDbMigration kicked in or no DB migration was required, startup can be resumed in another thread
    if (dbRequiresMigration()) {
      LOGGER.info("Database needs migration");
    } else {
      this.autoStarter = autoStarterSupplier.get();
      this.autoStarter.execute(new AutoStarterRunnable(autoStarter) {
        @Override
        public void doRun() {
          if (dbRequiredMigration) {
            LOGGER.info("Database has been automatically updated");
          }
          runIfNotAborted(Platform.this::startLevel34Containers);

          runIfNotAborted(() -> executeStartupTasks(startup));
          // switch current container last to avoid giving access to a partially initialized container
          runIfNotAborted(() -> {
            currentLevel = level4;
            LOGGER.info("WebServer is operational");
          });

          // stop safemode container if it existed
          runIfNotAborted(Platform.this::stopSafeModeContainer);
        }
      });
    }
  }

  private boolean dbRequiresMigration() {
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
    PlatformLevel current = this.currentLevel;
    PlatformLevel levelSafe = this.levelSafeMode;
    if (levelSafe != null && current == levelSafe) {
      return isRunning(this.autoStarter) ? Status.STARTING : Status.SAFEMODE;
    }
    if (current == level4) {
      return Status.UP;
    }
    return Status.BOOTING;
  }

  private static boolean isRunning(@Nullable AutoStarter autoStarter) {
    return autoStarter != null && autoStarter.isRunning();
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
      stopAutoStarter();
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

  private void stopAutoStarter() {
    if (autoStarter != null) {
      autoStarter.abort();
      autoStarter = null;
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
    BOOTING, SAFEMODE, STARTING, UP
  }

  public enum Startup {
    NO_STARTUP_TASKS, ALL
  }

  public interface AutoStarter {
    /**
     * Let the autostarted execute the provided code.
     */
    void execute(Runnable startCode);

    /**
     * This method is called by executed start code (see {@link #execute(Runnable)} has finished with a failure.
     */
    void failure(Throwable t);

    /**
     * This method is called by executed start code (see {@link #execute(Runnable)} has finished successfully.
     */
    void success();

    /**
     * Indicates whether the AutoStarter is running.
     */
    boolean isRunning();

    /**
     * Requests the startcode (ie. the argument of {@link #execute(Runnable)}) aborts its processing (if it supports it).
     */
    void abort();

    /**
     * Indicates whether {@link #abort()} was invoked.
     * <p>
     * This method can be used by the start code to check whether it should proceed running or stop.
     * </p>
     */
    boolean isAborting();

    /**
     * Called when abortion is complete.
     * <p>
     * Start code support abortion should call this method once is done processing and if it stopped on abortion.
     * </p>
     */
    void aborted();
  }

  private abstract static class AutoStarterRunnable implements Runnable {
    private final AutoStarter autoStarter;

    AutoStarterRunnable(AutoStarter autoStarter) {
      this.autoStarter = autoStarter;
    }

    @Override
    public void run() {
      try {
        doRun();
      } catch (Throwable t) {
        autoStarter.failure(t);
      } finally {
        if (autoStarter.isAborting()) {
          autoStarter.aborted();
        } else {
          autoStarter.success();
        }
      }
    }

    abstract void doRun();

    void runIfNotAborted(Runnable r) {
      if (!autoStarter.isAborting()) {
        r.run();
      }
    }
  }

  private static final class AsynchronousAutoStarter implements AutoStarter {
    private final ProcessCommandWrapper processCommandWrapper;
    private boolean running = true;
    private boolean abort = false;

    private AsynchronousAutoStarter(ProcessCommandWrapper processCommandWrapper) {
      this.processCommandWrapper = processCommandWrapper;
    }

    @Override
    public void execute(Runnable startCode) {
      new Thread(startCode, "SQ starter").start();
    }

    @Override
    public void failure(Throwable t) {
      LOGGER.error("Background initialization failed. Stopping SonarQube", t);
      processCommandWrapper.requestStop();
      this.running = false;
    }

    @Override
    public void success() {
      LOGGER.debug("Background initialization of SonarQube done");
      this.running = false;
    }

    @Override
    public void aborted() {
      LOGGER.debug("Background initialization of SonarQube aborted");
      this.running = false;
    }

    @Override
    public boolean isRunning() {
      return running;
    }

    @Override
    public void abort() {
      this.abort = true;
    }

    @Override
    public boolean isAborting() {
      return this.abort;
    }
  }
}
