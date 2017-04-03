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
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.list;
import static org.apache.commons.lang.StringUtils.isBlank;

public final class NetworkUtils {

  private static final Set<Integer> ALREADY_ALLOCATED = new HashSet<>();
  private static final int MAX_TRIES = 50;

  private NetworkUtils() {
    // prevent instantiation
  }

  public static int getNextAvailablePort(InetAddress address) {
    return getNextAvailablePort(address, PortAllocator.INSTANCE);
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
        .flatMap(netif -> list(netif.getInetAddresses()).stream()
          .filter(inetAddress ->
          // Removing IPv6 for the time being
          inetAddress instanceof Inet4Address &&
          // Removing loopback addresses, useless for identifying a server
            !inetAddress.isLoopbackAddress() &&
            // Removing interfaces without IPs
            !isBlank(inetAddress.getHostAddress()))
          .map(InetAddress::getHostAddress))
        .filter(p -> !isBlank(p))
        .collect(Collectors.joining(","));
    } catch (SocketException e) {
      ips = "unresolved IPs";
    }

    return format("%s (%s)", hostname, ips);
  }

  /**
   * Warning - the allocated ports are kept in memory and are never clean-up. Besides the memory consumption,
   * that means that ports already allocated are never freed. As a consequence
   * no more than ~64512 calls to this method are allowed.
   */
  static int getNextAvailablePort(InetAddress address, PortAllocator portAllocator) {
    for (int i = 0; i < MAX_TRIES; i++) {
      int port = portAllocator.getAvailable(address);
      if (isValidPort(port)) {
        ALREADY_ALLOCATED.add(port);
        return port;
      }
    }
    throw new IllegalStateException("Fail to find an available port on " + address);
  }

  private static boolean isValidPort(int port) {
    return port > 1023 && !ALREADY_ALLOCATED.contains(port);
  }

  static class PortAllocator {
    private static final PortAllocator INSTANCE = new PortAllocator();

    int getAvailable(InetAddress address) {
      try (ServerSocket socket = new ServerSocket(0, 50, address)) {
        return socket.getLocalPort();
      } catch (IOException e) {
        throw new IllegalStateException("Fail to find an available port on " + address, e);
      }
    }
  }
}
