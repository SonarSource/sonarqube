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
package util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkUtils {
  private static final int MAX_TRY = 10;
  private static final AtomicInteger nextPort = new AtomicInteger(20000);

  private NetworkUtils() {
  }

  public static int getNextAvailablePort() {
    if (isOnTravisCI()) {
      for (int i = 0; i < MAX_TRY; i++) {
        int port = nextPort.getAndIncrement();

        // Check that the port is really available.
        // (On Travis, if the build is single threaded, it should be)
        //
        try {
          Process process = new ProcessBuilder("nc", "-z", "localhost", Integer.toString(port)).start();
          if (process.waitFor() == 1) {
            return port;
          }
        } catch (Exception e) {
          throw new IllegalStateException("Can't test that a network port is available", e);
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

  private static boolean isOnTravisCI() {
    return "true".equals(System.getenv("TRAVIS"));
  }
}
