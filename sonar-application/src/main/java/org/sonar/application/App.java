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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Monitor;
import org.sonar.process.Process;
import org.sonar.process.ProcessMXBean;
import org.sonar.process.ProcessWrapper;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import java.lang.management.ManagementFactory;

public class App implements ProcessMXBean {

  static final String PROCESS_NAME = "SonarQube";

  static final String SONAR_WEB_PROCESS = "web";
  static final String SONAR_SEARCH_PROCESS = "search";


  private final Installation installation;
  private Monitor monitor;
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;

  public App(Installation installation) throws Exception {
    this.installation = installation;

    Thread shutdownHook = new Thread(new Runnable() {
      @Override
      public void run() {
        terminate();
      }
    });
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      mbeanServer.registerMBean(this, Process.objectNameFor(PROCESS_NAME));
    } catch (InstanceAlreadyExistsException e) {
      throw new IllegalStateException("Process already exists in current JVM", e);
    } catch (MBeanRegistrationException e) {
      throw new IllegalStateException("Could not register process as MBean", e);
    } catch (NotCompliantMBeanException e) {
      throw new IllegalStateException("Process is not a compliant MBean", e);
    }

    monitor = new Monitor();
  }

  public void start() {

    Logger logger = LoggerFactory.getLogger(getClass());

    logger.info("Starting search server");
    elasticsearch = new ProcessWrapper(SONAR_SEARCH_PROCESS)
      .setWorkDir(installation.homeDir())
      .setJmxPort(Integer.parseInt(installation.prop(DefaultSettings.ES_JMX_PORT_KEY)))
      .addJavaOpts(installation.prop(DefaultSettings.ES_JAVA_OPTS_KEY))
      .addJavaOpts(String.format("-Djava.io.tmpdir=%s", installation.tempDir().getAbsolutePath()))
      .addJavaOpts(String.format("-D%s=%s", DefaultSettings.PATH_LOGS_KEY, installation.logsDir().getAbsolutePath()))
      .setClassName("org.sonar.search.ElasticSearch")
      .setProperties(installation.props().cryptedProperties())
      .addClasspath(installation.starPath("lib/common"))
      .addClasspath(installation.starPath("lib/search"))
      .execute();
    monitor.registerProcess(elasticsearch);
    logger.info("Search server is ready");

    logger.info("Starting web server");
    server = new ProcessWrapper(SONAR_WEB_PROCESS)
      .setWorkDir(installation.homeDir())
      .setJmxPort(Integer.parseInt(installation.prop(DefaultSettings.WEB_JMX_PORT_KEY)))
      .addJavaOpts(installation.prop(DefaultSettings.WEB_JAVA_OPTS_KEY))
      .addJavaOpts(DefaultSettings.WEB_JAVA_OPTS_APPENDED_VAL)
      .addJavaOpts(String.format("-Djava.io.tmpdir=%s", installation.tempDir().getAbsolutePath()))
      .addJavaOpts(String.format("-D%s=%s", DefaultSettings.PATH_LOGS_KEY, installation.logsDir().getAbsolutePath()))
      .setClassName("org.sonar.server.app.ServerProcess")
      .setProperties(installation.props().cryptedProperties())
      .addClasspath(installation.starPath("extensions/jdbc-driver/mysql"))
      .addClasspath(installation.starPath("extensions/jdbc-driver/mssql"))
      .addClasspath(installation.starPath("extensions/jdbc-driver/oracle"))
      .addClasspath(installation.starPath("extensions/jdbc-driver/postgresql"))
      .addClasspath(installation.starPath("lib/common"))
      .addClasspath(installation.starPath("lib/server"))
      .execute();
    monitor.registerProcess(server);
    logger.info("Web server is ready");

    monitor.start();

    try {
      monitor.join();
    } catch (InterruptedException e) {
      // TODO ignore ?

    } finally {
      logger.debug("Closing App because monitor is gone.");
      terminate();
    }
  }

  @Override
  public void terminate() {
    Logger logger = LoggerFactory.getLogger(getClass());

    if (monitor != null) {
      monitor.interrupt();
      monitor = null;
      if (elasticsearch != null) {
        elasticsearch.terminate();
      }
      if (server != null) {
        server.terminate();
      }
      logger.info("Stopping SonarQube main process");
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

  public static void main(String[] args) throws Exception {
    Installation installation = new Installation();
    new AppLogging().configure(installation);
    new App(installation).start();
  }
}
