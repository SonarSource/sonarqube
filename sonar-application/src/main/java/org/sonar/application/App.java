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
package org.sonar.application;

import org.slf4j.LoggerFactory;
import org.sonar.application.command.CommandFactory;
import org.sonar.application.command.CommandFactoryImpl;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.AppSettingsLoader;
import org.sonar.application.config.AppSettingsLoaderImpl;
import org.sonar.core.extension.ServiceLoaderWrapper;
import org.sonar.process.System2;
import org.sonar.process.SystemExit;

import static org.sonar.application.config.SonarQubeVersionHelper.getSonarqubeVersion;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;

public class App {

  private final SystemExit systemExit = new SystemExit();
  private StopRequestWatcher stopRequestWatcher = null;
  private StopRequestWatcher hardStopRequestWatcher = null;

  public void start(String[] cliArguments) {
    AppSettingsLoader settingsLoader = new AppSettingsLoaderImpl(System2.INSTANCE, cliArguments, new ServiceLoaderWrapper());
    AppSettings settings = settingsLoader.load();
    // order is important - logging must be configured before any other components (AppFileSystem, ...)
    AppLogging logging = new AppLogging(settings);
    logging.configure();
    AppFileSystem fileSystem = new AppFileSystem(settings);

    try (AppState appState = new AppStateFactory(settings).create()) {
      appState.registerSonarQubeVersion(getSonarqubeVersion());
      appState.registerClusterName(settings.getProps().nonNullValue(CLUSTER_NAME.getKey()));
      AppReloader appReloader = new AppReloaderImpl(settingsLoader, fileSystem, appState, logging);
      fileSystem.reset();
      CommandFactory commandFactory = new CommandFactoryImpl(settings.getProps(), fileSystem.getTempDir(), System2.INSTANCE);

      try (ProcessLauncher processLauncher = new ProcessLauncherImpl(fileSystem.getTempDir())) {
        Scheduler scheduler = new SchedulerImpl(settings, appReloader, commandFactory, processLauncher, appState);

        scheduler.schedule();

        stopRequestWatcher = StopRequestWatcherImpl.create(settings, scheduler, fileSystem);
        hardStopRequestWatcher = HardStopRequestWatcherImpl.create(scheduler, fileSystem);

        // intercepts CTRL-C
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(scheduler));

        stopRequestWatcher.startWatching();
        hardStopRequestWatcher.startWatching();

        scheduler.awaitTermination();
        hardStopRequestWatcher.stopWatching();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LoggerFactory.getLogger(App.class).error("Startup interrupted", e);
    } catch (Exception e) {
      LoggerFactory.getLogger(App.class).error("Startup failure", e);
    }

    systemExit.exit(0);
  }

  public static void main(String[] args) {
    new App().start(args);
  }

  private class ShutdownHook extends Thread {
    private final Scheduler scheduler;

    public ShutdownHook(Scheduler scheduler) {
      super("Shutdown Hook");
      this.scheduler = scheduler;
    }

    @Override
    public void run() {
      systemExit.setInShutdownHook();
      stopRequestWatcher.stopWatching();
      hardStopRequestWatcher.stopWatching();

      // blocks until everything is corrected terminated
      scheduler.stop();
    }
  }
}
