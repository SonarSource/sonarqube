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
package org.sonar.application;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.ProcessCommands;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.StopWatcher;
import org.sonar.process.Stoppable;
import org.sonar.process.monitor.JavaCommand;
import org.sonar.process.monitor.Monitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Entry-point of process that starts and monitors elasticsearch and web servers
 */
public class App implements Stoppable {

  private final Monitor monitor;
  private StopWatcher stopWatcher = null;

  public App() {
    this(Monitor.create());
  }

  App(Monitor monitor) {
    this.monitor = monitor;
  }

  public void start(Props props) {
    if (props.valueAsBoolean(ProcessProperties.ENABLE_STOP_COMMAND, false)) {
      File tempDir = props.nonNullValueAsFile(ProcessProperties.PATH_TEMP);
      ProcessCommands commands = new DefaultProcessCommands(tempDir, 0);
      stopWatcher = new StopWatcher(commands, this);
      stopWatcher.start();
    }
    monitor.start(createCommands(props));
    monitor.awaitTermination();
  }

  List<JavaCommand> createCommands(Props props) {
    List<JavaCommand> commands = new ArrayList<>();
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    File tempDir = props.nonNullValueAsFile(ProcessProperties.PATH_TEMP);
    JavaCommand elasticsearch = new JavaCommand("search");
    elasticsearch
      .setWorkDir(homeDir)
      .addJavaOptions("-Djava.awt.headless=true")
      .addJavaOptions(props.nonNullValue(ProcessProperties.SEARCH_JAVA_OPTS))
      .addJavaOptions(props.nonNullValue(ProcessProperties.SEARCH_JAVA_ADDITIONAL_OPTS))
      .setTempDir(tempDir.getAbsoluteFile())
      .setClassName("org.sonar.search.SearchServer")
      .setArguments(props.rawProperties())
      .addClasspath("./lib/common/*")
      .addClasspath("./lib/search/*");
    commands.add(elasticsearch);

    // do not yet start SQ on elasticsearch slaves
    if (StringUtils.isBlank(props.value(ProcessProperties.CLUSTER_MASTER_HOST))) {
      JavaCommand webServer = new JavaCommand("web")
        .setWorkDir(homeDir)
        .addJavaOptions(ProcessProperties.WEB_ENFORCED_JVM_ARGS)
        .addJavaOptions(props.nonNullValue(ProcessProperties.WEB_JAVA_OPTS))
        .addJavaOptions(props.nonNullValue(ProcessProperties.WEB_JAVA_ADDITIONAL_OPTS))
        .setTempDir(tempDir.getAbsoluteFile())
          // required for logback tomcat valve
        .setEnvVariable(ProcessProperties.PATH_LOGS, props.nonNullValue(ProcessProperties.PATH_LOGS))
        .setClassName("org.sonar.server.app.WebServer")
        .setArguments(props.rawProperties())
        .addClasspath("./lib/common/*")
        .addClasspath("./lib/server/*");
      String driverPath = props.value(ProcessProperties.JDBC_DRIVER_PATH);
      if (driverPath != null) {
        webServer.addClasspath(driverPath);
      }
      commands.add(webServer);
    }
    return commands;
  }

  static String starPath(File homeDir, String relativePath) {
    File dir = new File(homeDir, relativePath);
    return FilenameUtils.concat(dir.getAbsolutePath(), "*");
  }

  public static void main(String[] args) throws Exception {
    new MinimumViableSystem().checkJavaVersion();
    CommandLineParser cli = new CommandLineParser();
    Properties rawProperties = cli.parseArguments(args);
    Props props = new PropsBuilder(rawProperties, new JdbcSettings()).build();
    AppLogging logging = new AppLogging();
    logging.configure(props);

    App app = new App();
    app.start(props);
  }

  StopWatcher getStopWatcher() {
    return stopWatcher;
  }

  @Override
  public void stopAsync() {
    monitor.stopAsync();
  }
}
