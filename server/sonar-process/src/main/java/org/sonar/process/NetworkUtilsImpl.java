/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class NetworkUtilsImpl implements NetworkUtils {

  private static final Set<Integer> PORTS_ALREADY_ALLOCATED = new HashSet<>();
  private static final int PORT_MAX_TRIES = 50;
  private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilsImpl.class);

  public static final NetworkUtils INSTANCE = new NetworkUtilsImpl();

  NetworkUtilsImpl() {
    // prevent instantiation
  }

  @Override
  public int getNextLoopbackAvailablePort() {
    return getNextAvailablePort(InetAddress.getLoopbackAddress(), PortAllocator.INSTANCE);
  }

  @Override
  public OptionalInt getNextAvailablePort(String hostOrAddress) {
    return OptionalInt.of(toInetAddress(hostOrAddress)
      .map(t -> getNextAvailablePort(t, PortAllocator.INSTANCE))
      .orElseThrow(() -> new IllegalArgumentException(format("Can not resolve address %s", hostOrAddress))));
  }

  /**
   * Warning - the allocated ports are kept in memory and are never clean-up. Besides the memory consumption,
   * that means that ports already allocated are never freed. As a consequence
   * no more than ~64512 calls to this method are allowed.
   */
  @VisibleForTesting
  static int getNextAvailablePort(InetAddress address, PortAllocator portAllocator) {
    for (int i = 0; i < PORT_MAX_TRIES; i++) {
      int port = portAllocator.getAvailable(address);
      if (isValidPort(port)) {
        PORTS_ALREADY_ALLOCATED.add(port);
        return port;
      }
    }
    throw new IllegalStateException("Fail to find an available port on " + address);
  }

  private static boolean isValidPort(int port) {
    return port > 1023 && !PORTS_ALREADY_ALLOCATED.contains(port);
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

  @Override
  public String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LOG.trace("Failed to get hostname", e);
      return "unresolved hostname";
    }
  }

  @Override
  public Optional<InetAddress> toInetAddress(String hostOrAddress) {
    try {
      if (InetAddresses.isInetAddress(hostOrAddress)) {
        return Optional.of(InetAddresses.forString(hostOrAddress));
      }
      return Optional.of(InetAddress.getByName(hostOrAddress));
    } catch (UnknownHostException e) {
      LOG.trace("toInetAddress({}) failed", hostOrAddress, e);
      return Optional.empty();
    }
  }

  @Override
  public boolean isLocal(String hostOrAddress) {
    try {
      Optional<InetAddress> inetAddress = toInetAddress(hostOrAddress);
      if (inetAddress.isPresent()) {
        var addr = inetAddress.get();
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
          return true;
        }
        return NetworkInterface.getByInetAddress(addr) != null;
      }
      return false;
    } catch (SocketException e) {
      LOG.trace("isLocalInetAddress({}) failed", hostOrAddress, e);
      return false;
    }
  }

  @Override
  public boolean isLoopback(String hostOrAddress) {
    return toInetAddress(hostOrAddress)
      .filter(InetAddress::isLoopbackAddress)
      .isPresent();
  }

  @Override
  public boolean isIpv6Address(String hostOrAddress) {
    Optional<InetAddress> inetAddress = toInetAddress(hostOrAddress);
    boolean ipv6Preferred = Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Addresses"));

    if (inetAddress.isEmpty() || inetAddress.get().isLoopbackAddress()) {
      return ipv6Preferred;
    }
    return inetAddress.get() instanceof Inet6Address;
  }

  @Override
  public Optional<InetAddress> getLocalInetAddress(Predicate<InetAddress> predicate) {
    try {
      return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
        .flatMap(ni -> Collections.list(ni.getInetAddresses()).stream())
        .filter(a -> a.getHostAddress() != null)
        .filter(predicate)
        .findFirst();
    } catch (SocketException e) {
      LOG.trace("getLocalInetAddress(Predicate<InetAddress>) failed", e);
      throw new IllegalStateException("Can not retrieve network interfaces", e);
    }
  }
}
