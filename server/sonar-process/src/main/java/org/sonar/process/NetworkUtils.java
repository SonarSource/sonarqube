/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Predicate;

public interface NetworkUtils {

  int getNextAvailablePort(InetAddress address);

  /**
   * Identifying the localhost machine
   * It will try to retrieve the hostname
   *
   * @return "hostname"
   */
  String getHostname();

  /**
   * Converts a text representation of an IP address or host name to
   * a {@link InetAddress}.
   * If text value references an IPv4 or IPv6 address, then DNS is
   * not used.
   */
  InetAddress toInetAddress(String hostOrAddress) throws UnknownHostException;

  boolean isLocalInetAddress(InetAddress address) throws SocketException;

  boolean isLoopbackInetAddress(InetAddress address);

  /**
   * Returns the machine {@link InetAddress} that matches the specified
   * predicate. If multiple addresses match then a single one
   * is picked in a non deterministic way.
   */
  Optional<InetAddress> getLocalInetAddress(Predicate<InetAddress> predicate);

  /**
   * Returns a local {@link InetAddress} that is IPv4 and not
   * loopback. If multiple addresses match then a single one
   * is picked in a non deterministic way.
   */
  default Optional<InetAddress> getLocalNonLoopbackIpv4Address() {
    return getLocalInetAddress(a -> !a.isLoopbackAddress() && a instanceof Inet4Address);
  }
}
