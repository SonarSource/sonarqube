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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Monitor;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class StartServer {

  private final static Logger LOGGER = LoggerFactory.getLogger(StartServer.class);

  public final static String SONAR_HOME = "SONAR_HOME";

  private Monitor monitor;

  private final Env env;
  private final String esPort;
  private final Map<String, String> properties;
  private final Thread shutdownHook;

  private ProcessWrapper elasticsearch;
  private ProcessWrapper sonarqube;

  public StartServer(Env env, String... args) throws IOException {
    this.env = env;
    this.esPort = Integer.toString(NetworkUtils.freePort());
    this.properties = new HashMap<String, String>();

    monitor = new Monitor();

    if (Arrays.binarySearch(args, "--debug") > -1) {
      properties.put("esDebug", "true");
    }

    shutdownHook = new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("Before");
        stop();
        System.out.println("After");
      }
    });

    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public void shutdown() {
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
    this.stop();
  }

  public void stop() {
    LOGGER.info("Shutting down all node services");
    terminateAndWait(elasticsearch);
    terminateAndWait(sonarqube);

    //TODO should not have to explicitly exit...
    System.exit(1);
  }

  private void terminateAndWait(ProcessWrapper process) {
    if (process != null && process.getThread() != null) {
      LOGGER.info("Shutting down {} service", process.getName());
      process.terminate();
    }
  }

  public void start() {

    String workingDirectory = env.rootDir().getAbsolutePath();

    // Start ES
    elasticsearch = new ProcessWrapper(
      env.rootDir().getAbsolutePath(),
      "org.sonar.search.ElasticSearch",
      ImmutableMap.of(
        "esDebug", properties.containsKey("esDebug") ? properties.get("esDebug") : "false",
        "esPort", esPort,
        "esHome", env.rootDir().getAbsolutePath()),
      "ES",
      env.rootDir().getAbsolutePath() + "/lib/search/sonar-search-4.5-SNAPSHOT.jar");

    monitor.registerProcess(elasticsearch);
//
//    while (!elasticsearch.isReady()) {
//      LOGGER.info("Waiting for ES");
//      try {
//        Thread.sleep(1000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//    }


    monitor.start();
    try {
      monitor.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Start SQ
//    sonarqube = new ProcessWrapper(
//      workingDirectory,
//      "org.sonar.application.SonarServer",
//      ImmutableMap.of(
//        "SONAR_HOME", workingDirectory,
//        "esPort", esPort,
//        "test", "test"),
//      "SQ",
//      env.rootDir().getAbsolutePath() + "/lib/server/sonar-application-4.5-SNAPSHOT.jar");

//    // And monitor the activity
//    try {
//      monitor.join();
//    } catch (InterruptedException e) {
//      LOGGER.warn("Shutting down the node...");
//    }
    shutdown();
  }


  public static void main(String... args) throws InterruptedException, IOException, URISyntaxException {

    String home = System.getenv(SONAR_HOME);
    //String home = "/Volumes/data/sonar/sonarqube/sonar-start/target/sonarqube-4.5-SNAPSHOT";

    //Check if we have a SONAR_HOME
    if (StringUtils.isEmpty(home)) {
      home = new File(".").getAbsolutePath();
    }

    new StartServer(new Env(home), args).start();
  }
}