/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import static java.lang.String.format;
import static java.util.Collections.list;
import static org.apache.commons.lang.StringUtils.isBlank;

public final class NetworkUtils {

  private static final RandomPortFinder RANDOM_PORT_FINDER = new RandomPortFinder();

  private NetworkUtils() {
    // only statics
  }

  public static int freePort() {
    return RANDOM_PORT_FINDER.getNextAvailablePort();
  }

  /**
   * Identifying the localhost machine
   * It will try to retrieve the hostname and the IPv4 addresses
   *
   * @return "hostname (ipv4_1, ipv4_2...)"
   */
  public static String getHostName() {
    String hostname;
    String ips;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "unresolved hostname";
    }

    try {
      ips = list(NetworkInterface.getNetworkInterfaces()).stream()
        .flatMap(netif ->
          list(netif.getInetAddresses()).stream()
            .filter(inetAddress ->
              // Removing IPv6 for the time being
              inetAddress instanceof Inet4Address &&
                // Removing loopback addresses, useless for identifying a server
                !inetAddress.isLoopbackAddress() &&
                // Removing interfaces without IPs
                !isBlank(inetAddress.getHostAddress())
            )
            .map(InetAddress::getHostAddress)
        )
        .filter(p -> !isBlank(p))
        .collect(Collectors.joining(","));
    } catch (SocketException e) {
      ips = "unresolved IPs";
    }

    return format("%s (%s)", hostname, ips);
  }

  static class RandomPortFinder {
    private static final int MAX_TRY = 10;
    // Firefox blocks some reserved ports : http://www-archive.mozilla.org/projects/netlib/PortBanning.html
    private static final int[] BLOCKED_PORTS = {2049, 4045, 6000};

    public int getNextAvailablePort() {
      for (int i = 0; i < MAX_TRY; i++) {
        try {
          int port = getRandomUnusedPort();
          if (isValidPort(port)) {
            return port;
          }
        } catch (Exception e) {
          throw new IllegalStateException("Can't find an open network port", e);
        }
      }

      throw new IllegalStateException("Can't find an open network port");
    }

    public int getRandomUnusedPort() throws IOException {
      ServerSocket socket = null;
      try {
        socket = new ServerSocket();
        socket.bind(new InetSocketAddress("localhost", 0));
        return socket.getLocalPort();
      } catch (IOException e) {
        throw new IllegalStateException("Can not find a free network port", e);
      } finally {
        IOUtils.closeQuietly(socket);
      }
    }

    public static boolean isValidPort(int port) {
      return port > 1023 && !ArrayUtils.contains(BLOCKED_PORTS, port);
    }
  }
}
