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
package org.sonar.start;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.MonitorService;
import org.sonar.process.ProcessWrapper;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class StartServer {

  private final static Logger LOGGER = LoggerFactory.getLogger(StartServer.class);

  private final Env env;
  private final ExecutorService executor;
  private final MonitorService monitor;
  private final String esPort;

  private ProcessWrapper elasticsearch;
  private ProcessWrapper sonarqube;

  public StartServer(Env env) throws IOException {
    this.env = env;
    this.executor = Executors.newFixedThreadPool(2);
    this.monitor = new MonitorService(systemAvailableSocket());
    this.esPort = Integer.toString(NetworkUtils.freePort());

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOGGER.info("Shutting down sonar Node");
        if (elasticsearch != null) {
          elasticsearch.shutdown();
        }
        if (sonarqube != null) {
          sonarqube.shutdown();
        }
        executor.shutdown();
        try {
          executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          LOGGER.warn("Executing terminated", e);
        }
      }
    });
  }

  private DatagramSocket systemAvailableSocket() throws IOException {
    return new DatagramSocket(0);
  }

  public void start() {

    // Start ES
    this.startES();

    // Start SQ
    this.startSQ();

    // And monitor the activity
    monitor.run();
    LOGGER.warn("Shutting down the node...");

    // If monitor is finished, we're done. Cleanup
    executor.shutdownNow();

  }

  private void registerProcess(ProcessWrapper process) {
    //Register processes to monitor
    monitor.register(process);

    // Start our processes
    LOGGER.info("Starting Child processes...");
    executor.submit(process);
  }

  private void startSQ() {
    sonarqube = new ProcessWrapper(
      "org.sonar.application.StartServer",
      new String[]{env.rootDir().getAbsolutePath() + "/lib/server/sonar-application-4.5-SNAPSHOT.jar"},
      ImmutableMap.of("test", "test"),
      "SQ", monitor.getMonitoringPort());

    registerProcess(sonarqube);
  }

  private void startES() {
    elasticsearch = new ProcessWrapper(
      "org.sonar.search.ElasticSearch",
      new String[]{env.rootDir().getAbsolutePath() + "/lib/search/sonar-search-4.5-SNAPSHOT.jar"},
      ImmutableMap.of(
        "esPort", esPort,
        "esHome", env.rootDir().getAbsolutePath()),
      "ES", monitor.getMonitoringPort());

    registerProcess(elasticsearch);
  }

  public static void main(String... args) throws InterruptedException, IOException, URISyntaxException {
    File home = new File(".");
    new StartServer(new Env(home)).start();
  }
}