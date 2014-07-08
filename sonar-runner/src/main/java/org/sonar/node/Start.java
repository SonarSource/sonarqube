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
package org.sonar.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Since 4.5
 */
public class Start {

  private final static Logger LOGGER = LoggerFactory.getLogger(Start.class);


  private static DatagramSocket systemAvailableSocket() throws IOException {
    return new DatagramSocket(0);
  }

  public static void main(String... args) throws InterruptedException, IOException {

    final ExecutorService executor = Executors.newFixedThreadPool(2);
    final Launcher sonarQube = new Launcher("SQ", systemAvailableSocket());
    final Launcher elasticsearch = new Launcher("ES", systemAvailableSocket());

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOGGER.info("Shutting down sonar Node");
        sonarQube.interrupt();
        elasticsearch.interrupt();
        executor.shutdownNow();
      }
    });

    LOGGER.info("Starting SQ Node...");
    executor.submit(sonarQube);

    LOGGER.info("Starting ES Node...");
    executor.submit(elasticsearch);

    while (!executor.isTerminated()) {
      Thread.sleep(1000);
    }
  }
}
