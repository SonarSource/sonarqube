/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.platform.platformlevel.PlatformLevel;
import org.sonar.server.platform.platformlevel.PlatformLevel1;
import org.sonar.server.platform.platformlevel.PlatformLevel2;
import org.sonar.server.platform.platformlevel.PlatformLevel3;
import org.sonar.server.platform.platformlevel.PlatformLevel4;
import org.sonar.server.platform.platformlevel.PlatformLevelSafeMode;
import org.sonar.server.platform.platformlevel.PlatformLevelStartup;
import org.sonar.server.platform.web.ApiV2Servlet;

import static jakarta.servlet.DispatcherType.ASYNC;
import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.REQUEST;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.springframework.web.filter.UrlHandlerFilter.trailingSlashHandler;

/**
 * @since 2.2
 */
public class PlatformImpl implements Platform {

  private static final Logger LOGGER = Loggers.get(Platform.class);

  private static final PlatformImpl INSTANCE = new PlatformImpl();

  private AutoStarter autoStarter = null;
  private Properties properties = null;
  private ServletContext servletContext = null;
  private PlatformLevel level1 = null;
  private PlatformLevel level2 = null;
  private PlatformLevel levelSafeMode = null;
  private PlatformLevel level3 = null;
  private PlatformLevel level4 = null;
  private PlatformLevel currentLevel = null;
  private boolean dbConnected = false;
  private boolean started = false;
  private final List<Object> level4AddedComponents = new ArrayList<>();
  private final Profiler profiler = Profiler.createIfTrace(Loggers.get(PlatformImpl.class));
  private ApiV2Servlet servlet = null;

  public static PlatformImpl getInstance() {
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

  @Override
  public void doStart() {
    if (started && !isInSafeMode()) {
      return;
    }

    boolean dbRequiredMigration = dbRequiresMigration();
    startSafeModeContainer();
    currentLevel = levelSafeMode;
    if (!started) {
      registerSpringMvcServlet();
      this.servlet.initDispatcherSafeMode(levelSafeMode);
    }
    started = true;

    // if AutoDbMigration kicked in or no DB migration was required, startup can be resumed in another thread
    if (dbRequiresMigration()) {
      DocumentationLinkGenerator docLinkGenerator = currentLevel.getContainer().getComponentByType(DocumentationLinkGenerator.class);
      String documentationLink = docLinkGenerator.getDocumentationLink("/server-upgrade-and-maintenance/upgrade/roadmap/");
      LOGGER.info("Database needs to be migrated. Please refer to {}", documentationLink);
    } else {
      this.autoStarter = createAutoStarter();

      this.autoStarter.execute(new AutoStarterRunnable(autoStarter) {
        @Override
        public void doRun() {
          if (dbRequiredMigration) {
            LOGGER.info("Database has been automatically updated");
          }
          runIfNotAborted(PlatformImpl.this::startLevel34Containers);

          runIfNotAborted(() -> servlet.initDispatcherLevel4(level4));
          runIfNotAborted(PlatformImpl.this::executeStartupTasks);

          // switch current container last to avoid giving access to a partially initialized container
          runIfNotAborted(() -> {
            currentLevel = level4;
            LOGGER.info("{} is operational", WEB_SERVER.getHumanReadableName());
          });

          // stop safemode container if it existed
          runIfNotAborted(PlatformImpl.this::stopSafeModeContainer);
        }
      });
    }
  }

  private void registerSpringMvcServlet() {
    servlet = new ApiV2Servlet();
    ServletRegistration.Dynamic app = this.servletContext.addServlet("app", servlet);
    app.addMapping("/api/v2/*");
    app.setLoadOnStartup(1);
    registerTrailingSlashFilter();
  }

  private void registerTrailingSlashFilter() {
    var filter = trailingSlashHandler("/api/v2/**")
      .wrapRequest()
      .build();
    var filterRegistration = this.servletContext.addFilter("trailingSlashFilter", filter);
    filterRegistration.addMappingForUrlPatterns(
      java.util.EnumSet.of(REQUEST, ERROR, ASYNC),
      false,
      "/api/v2/*"
    );
  }

  private AutoStarter createAutoStarter() {
    ProcessCommandWrapper processCommandWrapper = getContainer().getComponentByType(ProcessCommandWrapper.class);
    return new AsynchronousAutoStarter(processCommandWrapper);
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

  @Override
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

  private void executeStartupTasks() {
    new PlatformLevelStartup(level4)
      .configure()
      .start()
      .stop();
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
   * {@link SpringComponentContainer#stopComponents()}).
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
   * see {@link SpringComponentContainer#stopComponents()}).
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

  @Override
  public ExtensionContainer getContainer() {
    return currentLevel.getContainer();
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
      processCommandWrapper.requestHardStop();
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
