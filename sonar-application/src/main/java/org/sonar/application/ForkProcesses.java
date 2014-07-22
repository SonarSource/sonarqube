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

public class ForkProcesses {
  private Monitor monitor;
  private final Thread shutdownHook;
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;

  public ForkProcesses() throws Exception {
    Installation installation = new Installation();

    String esPort = installation.prop("sonar.es.node.port", null);
    if (esPort == null) {
      esPort = String.valueOf(NetworkUtils.freePort());
      installation.setProp("sonar.es.node.port", esPort);
    }
    String esCluster = installation.prop("sonar.es.cluster.name", null);
    if(esCluster == null){
      installation.setProp("sonar.es.cluster.name", "sonarqube");
    }
    installation.setProp("sonar.es.type", "TRANSPORT");

    shutdownHook = new Thread(new Runnable() {
      @Override
      public void run() {
        monitor.interrupt();
        terminateAndWait(elasticsearch);
        terminateAndWait(server);
      }
    });

    Runtime.getRuntime().addShutdownHook(shutdownHook);

    monitor = new Monitor();

    elasticsearch = new ProcessWrapper(
      installation.homeDir().getAbsolutePath(),
      installation.prop("sonar.es.javaOpts", "-server -Xmx256m -Xms128m -Xss256k -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly"),
      "org.sonar.search.ElasticSearch",
      installation.props(),
      "ES",
      installation.starPath("lib/search"));
    monitor.registerProcess(elasticsearch);


    server = new ProcessWrapper(
      installation.homeDir().getAbsolutePath(),
      installation.prop("sonar.web.javaOpts", "-Xmx768m -server -XX:MaxPermSize=160m -Djava.awt.headless=true -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Djruby.management.enabled=false"),
      "org.sonar.server.app.ServerProcess",
      installation.props(),
      "SQ",
      installation.starPath("lib"));
    monitor.registerProcess(server);

    monitor.start();
    try {
      monitor.join();
    } catch (InterruptedException e) {
      stop(true);
    }
    stop(true);
  }

  public void stop(boolean waitForCompletion) {
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
    shutdownHook.start();
    if (waitForCompletion) {
      try {
        shutdownHook.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  private void terminateAndWait(@Nullable ProcessWrapper process) {
    if (process != null && process.getThread() != null) {
      process.terminate();
    }
  }

  public static void main(String[] args) throws Exception {
    new ForkProcesses();
  }
}
