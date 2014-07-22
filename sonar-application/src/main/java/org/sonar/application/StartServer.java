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

import org.sonar.process.Monitor;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessWrapper;

import javax.annotation.Nullable;

public class StartServer {
  private Monitor monitor;
  private final Thread shutdownHook;
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;

  public StartServer() throws Exception {
    Installation installation = new Installation();

    shutdownHook = new Thread(new Runnable() {
      @Override
      public void run() {
        stop();
      }
    });

    Runtime.getRuntime().addShutdownHook(shutdownHook);

    monitor = new Monitor();

    String opts = installation.prop("sonar.es.javaOpts", "-server -Xmx256m -Xms128m -Xss256k -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly");
    elasticsearch = new ProcessWrapper("ES")
      .setWorkDir(installation.homeDir())
      .setJmxPort(NetworkUtils.freePort())
      .addJavaOpts(opts)
      .addJavaOpts("-Djava.io.tmpdir=" + installation.tempDir().getAbsolutePath())
      .setEnvProperty("SONAR_HOME", installation.homeDir().getAbsolutePath())
      .setClassName("org.sonar.search.ElasticSearch")
      .setProperties(installation.props())
      .addClasspath(installation.starPath("lib/common"))
      .addClasspath(installation.starPath("lib/search"))
      .execute();
    monitor.registerProcess(elasticsearch);


    opts = installation.prop("sonar.web.javaOpts", "-Xmx768m -server -XX:MaxPermSize=160m -XX:+HeapDumpOnOutOfMemoryError");
    server = new ProcessWrapper("SQ")
      .setWorkDir(installation.homeDir())
      .setJmxPort(NetworkUtils.freePort())
      .addJavaOpts(opts)
      .addJavaOpts("-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.management.enabled=false")
      .addJavaOpts("-Djava.io.tmpdir=" + installation.tempDir().getAbsolutePath())
      .setClassName("org.sonar.server.app.ServerProcess")
      .setEnvProperty("SONAR_HOME", installation.homeDir().getAbsolutePath())
      .setProperties(installation.props())
      .addClasspath(installation.starPath("extensions/jdbc-driver/mysql"))
      .addClasspath(installation.starPath("extensions/jdbc-driver/mssql"))
      .addClasspath(installation.starPath("extensions/jdbc-driver/oracle"))
      .addClasspath(installation.starPath("extensions/jdbc-driver/postgresql"))
      .addClasspath(installation.starPath("lib/common"))
      .addClasspath(installation.starPath("lib/server"))
      .execute();
    monitor.registerProcess(server);

    monitor.start();
    try {
      monitor.join();
    } finally {
      stop();
    }
  }

  public void stop() {
    if (monitor != null) {
      monitor.interrupt();
      terminateAndWait(elasticsearch);
      terminateAndWait(server);
      monitor = null;
    }
  }

  private void terminateAndWait(@Nullable ProcessWrapper process) {
    if (process != null) {
      process.terminate();
      try {
        process.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) throws Exception {
    new StartServer();
  }
}
