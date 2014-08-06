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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.JmxUtils;
import org.sonar.process.Monitor;
import org.sonar.process.ProcessMXBean;
import org.sonar.process.ProcessUtils;
import org.sonar.process.ProcessWrapper;

public class App implements ProcessMXBean {

  private final Installation installation;

  private Monitor monitor = new Monitor();
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;
  private boolean success = false;

  public App(Installation installation) throws Exception {
    this.installation = installation;
    JmxUtils.registerMBean(this, "SonarQube");
    ProcessUtils.addSelfShutdownHook(this);
  }

  public void start() throws InterruptedException {
    try {
      Logger logger = LoggerFactory.getLogger(getClass());
      monitor.start();

      elasticsearch = new ProcessWrapper(JmxUtils.SEARCH_SERVER_NAME)
        .setWorkDir(installation.homeDir())
        .setJmxPort(Integer.parseInt(installation.prop(DefaultSettings.ES_JMX_PORT_KEY)))
        .addJavaOpts(installation.prop(DefaultSettings.ES_JAVA_OPTS_KEY))
        .addJavaOpts(String.format("-Djava.io.tmpdir=%s", installation.tempDir().getAbsolutePath()))
        .addJavaOpts(String.format("-Dsonar.path.logs=%s", installation.logsDir().getAbsolutePath()))
        .setClassName("org.sonar.search.SearchServer")
        .addProperties(installation.props().rawProperties())
        .addProperty(DefaultSettings.SONAR_NODE_NAME, installation.prop(DefaultSettings.SONAR_NODE_NAME, DefaultSettings.getNonSetNodeName()))
        .addClasspath(installation.starPath("lib/common"))
        .addClasspath(installation.starPath("lib/search"));
      if (elasticsearch.execute()) {
        monitor.registerProcess(elasticsearch);
        if (elasticsearch.waitForReady()) {
          logger.info("search server is up");

          //do not yet start SQ in cluster mode. See SONAR-5483 & SONAR-5391
          if (StringUtils.isEmpty(installation.prop(DefaultSettings.SONAR_CLUSTER_MASTER, null))) {
            server = new ProcessWrapper(JmxUtils.WEB_SERVER_NAME)
              .setWorkDir(installation.homeDir())
              .setJmxPort(Integer.parseInt(installation.prop(DefaultSettings.WEB_JMX_PORT_KEY)))
              .addJavaOpts(installation.prop(DefaultSettings.WEB_JAVA_OPTS_KEY))
              .addJavaOpts(String.format("-Djava.io.tmpdir=%s", installation.tempDir().getAbsolutePath()))
              .addJavaOpts(String.format("-Dsonar.path.logs=%s", installation.logsDir().getAbsolutePath()))
              .setClassName("org.sonar.server.app.WebServer")
              .addProperties(installation.props().rawProperties())
              .addProperty(DefaultSettings.SONAR_NODE_NAME, installation.prop(DefaultSettings.SONAR_NODE_NAME, DefaultSettings.getNonSetNodeName()))
              .addClasspath(installation.starPath("extensions/jdbc-driver/mysql"))
              .addClasspath(installation.starPath("extensions/jdbc-driver/mssql"))
              .addClasspath(installation.starPath("extensions/jdbc-driver/oracle"))
              .addClasspath(installation.starPath("extensions/jdbc-driver/postgresql"))
              .addClasspath(installation.starPath("lib/common"))
              .addClasspath(installation.starPath("lib/server"));
            if (server.execute()) {
              monitor.registerProcess(server);
              if (server.waitForReady()) {
                success = true;
                logger.info("web server is up");
                monitor.join();
              }
            }
          } else {
            success = true;
            monitor.join();
          }
        }
      }
    } finally {
      terminate();
    }
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

  public static void main(String[] args) throws Exception {
    Installation installation = Installation.parseArguments(args);
    new AppLogging().configure(installation);
    App app = new App(installation);

    // start and wait for shutdown command
    app.start();

    LoggerFactory.getLogger(App.class).info("stopped");
    System.exit(app.isSuccess() ? 0 : 1);
  }
}
