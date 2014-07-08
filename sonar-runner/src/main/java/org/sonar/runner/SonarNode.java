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
package org.sonar.runner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SonarNode {

  private final ExecutorService executor;
  private final SonarProcess sqThread;
  private final SonarProcess esThread;

  public SonarNode() {
    Runtime.getRuntime().addShutdownHook(new ShutDown(this));
    executor = Executors.newFixedThreadPool(2);
    sqThread = new SonarProcess("SQ", 0);
    esThread = new SonarProcess("ES", 0);
  }

  public void start() throws InterruptedException {
    System.out.println("Starting sonarNode... ");
    executor.submit(sqThread);
    executor.submit(esThread);
    while (!executor.isShutdown()) {
      //Monitor my stuff here
      Thread.sleep(1000);
    }
  }

  public void shutdown() throws InterruptedException {
    System.out.println("Stopping sonarNode... ");
    executor.shutdownNow();
    executor.awaitTermination(10, TimeUnit.SECONDS);
  }

  public static class ShutDown extends Thread {
    final SonarNode node;

    public ShutDown(SonarNode node) {
      this.node = node;
    }

    @Override
    public void run() {
      try {
        node.shutdown();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String... args) throws InterruptedException {
    final SonarNode node = new SonarNode();
    node.run();
  }
}
