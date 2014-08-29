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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.JmxUtils;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitor;
import org.sonar.process.ProcessLogging;
import org.sonar.process.ProcessMXBean;
import org.sonar.process.ProcessUtils;
import org.sonar.process.ProcessWrapper;
import org.sonar.process.Props;
import org.sonar.search.SearchServer;

import java.io.File;
import java.util.Properties;

/**
 * Entry-point of process that starts and monitors elasticsearch and web servers
 */
public class App implements ProcessMXBean {

  private Monitor monitor = new Monitor();
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;
  private boolean success = false;

  public App() {
    JmxUtils.registerMBean(this, "SonarQube");
    ProcessUtils.addSelfShutdownHook(this);
  }

  public void start(Props props) throws InterruptedException {
    try {
      Logger logger = LoggerFactory.getLogger(getClass());

      monitor.start();

      File homeDir = props.nonNullValueAsFile("sonar.path.home");
      File tempDir = props.nonNullValueAsFile("sonar.path.temp");
      elasticsearch = new ProcessWrapper(JmxUtils.SEARCH_SERVER_NAME);
      elasticsearch
        .setWorkDir(homeDir)
        .setJmxPort(props.valueAsInt(DefaultSettings.SEARCH_JMX_PORT))
        .addJavaOpts(props.value(DefaultSettings.SEARCH_JAVA_OPTS))
        .setTempDirectory(tempDir.getAbsoluteFile())
        .setClassName("org.sonar.search.SearchServer")
        .addProperties(props.rawProperties())
        .addClasspath("./lib/common/*")
        .addClasspath("./lib/search/*");
      if (elasticsearch.execute()) {
        monitor.monitor(elasticsearch);
        if (elasticsearch.waitForReady()) {
          logger.info("search server is up");

          // do not yet start SQ in cluster mode. See SONAR-5483 & SONAR-5391
          if (StringUtils.isEmpty(props.value(DefaultSettings.CLUSTER_MASTER))) {
            server = new ProcessWrapper(JmxUtils.WEB_SERVER_NAME)
              .setWorkDir(homeDir)
              .setJmxPort(props.valueAsInt(DefaultSettings.WEB_JMX_PORT))
              .addJavaOpts(props.nonNullValue(DefaultSettings.WEB_JAVA_OPTS))
              .setTempDirectory(tempDir.getAbsoluteFile())
              // required for logback tomcat valve
              .setLogDir(props.nonNullValueAsFile("sonar.path.logs"))
              .setClassName("org.sonar.server.app.WebServer")
              .addProperties(props.rawProperties())
              .addClasspath("./lib/common/*")
              .addClasspath("./lib/server/*");
            String driverPath = props.value(JdbcSettings.PROPERTY_DRIVER_PATH);
            if (driverPath != null) {
              server.addClasspath(driverPath);
            }
            if (server.execute()) {
              monitor.monitor(server);
              if (server.waitForReady()) {
                success = true;
                logger.info("web server is up");
              }
            }
          } else {
            success = true;
          }
        }
      }
    } finally {
      monitor.join();
      terminate();
    }
  }

  static String starPath(File homeDir, String relativePath) {
    File dir = new File(homeDir, relativePath);
    return FilenameUtils.concat(dir.getAbsolutePath(), "*");
  }

  @Override
  public boolean isReady() {
    return monitor.isAlive();
  }

  @Override
  public long ping() {
    return System.currentTimeMillis();
  }

  @Override
  public void terminate() {
    if (monitor != null && monitor.isAlive()) {
      monitor.terminate();
      monitor.interrupt();
      monitor = null;
    }
    if (server != null) {
      server.terminate();
      server = null;
    }
    if (elasticsearch != null) {
      elasticsearch.terminate();
      elasticsearch = null;
    }
  }

  private boolean isSuccess() {
    return success;
  }

  public static void main(String[] args) {
    new MinimumViableSystem().check();
    CommandLineParser cli = new CommandLineParser();
    Properties rawProperties = cli.parseArguments(args);
    Props props;

    try {
      props = new PropsBuilder(rawProperties, new JdbcSettings()).build();
      new ProcessLogging().configure(props, "/org/sonar/application/logback.xml");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    App app = new App();
    ProcessUtils.addSelfShutdownHook(app);
    try {
      // start and wait for shutdown command
      if (props.contains(SearchServer.ES_CLUSTER_INET)) {
        LoggerFactory.getLogger(App.class).info("SonarQube slave configured to join SonarQube master : {}", props.value(SearchServer.ES_CLUSTER_INET));
    }
      app.start(props);
    } catch (InterruptedException e) {
      LoggerFactory.getLogger(App.class).info("interrupted");
    } finally {
      LoggerFactory.getLogger(App.class).info("stopped");
      System.exit(app.isSuccess() ? 0 : 1);
    }
  }
}
