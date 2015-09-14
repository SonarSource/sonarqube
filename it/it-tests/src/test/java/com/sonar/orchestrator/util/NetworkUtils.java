/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkUtils {
  private static final AtomicInteger nextPort = new AtomicInteger(20000);

  private NetworkUtils() {
  }

  public static int getNextAvailablePort() {
    System.out.println("=== Override method provided by orchestrator");

    if ("true".equals(System.getenv("TRAVIS"))) {
      for (int i = 0; i < 10; i++) {
        int port = nextPort.getAndIncrement();

        try {
          System.out.println("=== Trying port " + port);
          Process process = new ProcessBuilder("nc", "-z", "localhost", Integer.toString(port)).start();
          if (process.waitFor() == 1) {
            System.out.println("=== Using port " + port);
            return port;
          }
        } catch (Exception e) {
          // Ignore. will try again
          System.out.println(e);
        }
      }

      throw new IllegalStateException("Can't find a free network port");
    }

    try (ServerSocket socket = new ServerSocket()) {
      socket.bind(new InetSocketAddress("localhost", 0));
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new IllegalStateException("Can't find a free network port", e);
    }
  }
}
