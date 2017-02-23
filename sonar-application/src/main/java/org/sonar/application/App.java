/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Lifecycle;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.Stoppable;
import org.sonar.process.monitor.JavaCommand;
import org.sonar.process.monitor.Monitor;

import static org.sonar.process.Lifecycle.State;
import static org.sonar.process.ProcessId.APP;

/**
 * Entry-point of process that starts and monitors ElasticSearch, the Web Server and the Compute Engine.
 */
public class App implements Stoppable {

  private final Properties commandLineArguments;
  private final Function<Properties, Props> propsSupplier;
  private final JavaCommandFactory javaCommandFactory;
  private final Monitor monitor;
  private final Supplier<List<JavaCommand>> javaCommandSupplier;

  private App(Properties commandLineArguments) {
    this.commandLineArguments = commandLineArguments;
    this.propsSupplier = properties -> new PropsBuilder(properties, new JdbcSettings()).build();
    this.javaCommandFactory = new JavaCommandFactoryImpl();
    Props props = propsSupplier.apply(commandLineArguments);

    AppFileSystem appFileSystem = new AppFileSystem(props);
    appFileSystem.verifyProps();
    AppLogging logging = new AppLogging();
    logging.configure(props);

    // used by orchestrator
    boolean watchForHardStop = props.valueAsBoolean(ProcessProperties.ENABLE_STOP_COMMAND, false);
    this.monitor = Monitor.newMonitorBuilder()
      .setProcessNumber(APP.getIpcIndex())
      .setFileSystem(appFileSystem)
      .setWatchForHardStop(watchForHardStop)
      .setWaitForOperational()
      .addListener(new AppLifecycleListener())
      .build();
    this.javaCommandSupplier = new ReloadableCommandSupplier(props, appFileSystem::ensureUnchangedConfiguration);
  }

  @VisibleForTesting
  App(Properties commandLineArguments, Function<Properties, Props> propsSupplier, Monitor monitor, CheckFSConfigOnReload checkFsConfigOnReload, JavaCommandFactory javaCommandFactory) {
    this.commandLineArguments = commandLineArguments;
    this.propsSupplier = propsSupplier;
    this.javaCommandFactory = javaCommandFactory;
    this.monitor = monitor;
    this.javaCommandSupplier = new ReloadableCommandSupplier(propsSupplier.apply(commandLineArguments), checkFsConfigOnReload);
  }

  public void start() throws InterruptedException {
    monitor.start(javaCommandSupplier);
    monitor.awaitTermination();
  }

  private List<JavaCommand> createCommands(Props props) {
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    List<JavaCommand> commands = new ArrayList<>(3);
    if (isProcessEnabled(props, ProcessProperties.CLUSTER_SEARCH_DISABLED)) {
      commands.add(javaCommandFactory.createESCommand(props, homeDir));
    }

    if (isProcessEnabled(props, ProcessProperties.CLUSTER_WEB_DISABLED)) {
      commands.add(javaCommandFactory.createWebCommand(props, homeDir));
    }

    if (isProcessEnabled(props, ProcessProperties.CLUSTER_CE_DISABLED)) {
      commands.add(javaCommandFactory.createCeCommand(props, homeDir));
    }

    return commands;
  }

  private static boolean isProcessEnabled(Props props, String disabledPropertyKey) {
    return !props.valueAsBoolean(ProcessProperties.CLUSTER_ENABLED) ||
      !props.valueAsBoolean(disabledPropertyKey);
  }

  static String starPath(File homeDir, String relativePath) {
    File dir = new File(homeDir, relativePath);
    return FilenameUtils.concat(dir.getAbsolutePath(), "*");
  }

  public static void main(String[] args) throws InterruptedException {
    CommandLineParser cli = new CommandLineParser();
    Properties rawProperties = cli.parseArguments(args);

    App app = new App(rawProperties);
    app.start();
  }

  @Override
  public void stopAsync() {
    if (monitor != null) {
      monitor.stop();
    }
  }

  private static class AppLifecycleListener implements Lifecycle.LifecycleListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    @Override
    public void successfulTransition(State from, State to) {
      if (to == State.OPERATIONAL) {
        LOGGER.info("SonarQube is up");
      }
    }
  }

  @FunctionalInterface
  interface CheckFSConfigOnReload extends Consumer<Props> {

  }

  private class ReloadableCommandSupplier implements Supplier<List<JavaCommand>> {
    private final Props initialProps;
    private final CheckFSConfigOnReload checkFsConfigOnReload;
    private boolean initialPropsConsumed = false;

    ReloadableCommandSupplier(Props initialProps, CheckFSConfigOnReload checkFsConfigOnReload) {
      this.initialProps = initialProps;
      this.checkFsConfigOnReload = checkFsConfigOnReload;
    }

    @Override
    public List<JavaCommand> get() {
      if (!initialPropsConsumed) {
        initialPropsConsumed = true;
        return createCommands(this.initialProps);
      }
      return recreateCommands();
    }

    private List<JavaCommand> recreateCommands() {
      Props reloadedProps = propsSupplier.apply(commandLineArguments);
      AppFileSystem appFileSystem = new AppFileSystem(reloadedProps);
      appFileSystem.verifyProps();
      checkFsConfigOnReload.accept(reloadedProps);
      AppLogging logging = new AppLogging();
      logging.configure(reloadedProps);

      return createCommands(reloadedProps);
    }
  }
}
