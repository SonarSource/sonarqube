/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class LoopbackAddress {

  private static volatile InetAddress instance;

  private LoopbackAddress() {
    // only static stuff
  }

  /**
   * Quite similar to {@code InetAddress.getLoopbackAddress()} which was introduced in Java 7. This
   * method aims to support Java 6. It returns an IPv4 address, but not IPv6 in order to
   * support {@code -Djava.net.preferIPv4Stack=true} which is recommended for Elasticsearch.
   */
  public static InetAddress get() {
    if (instance == null) {
      try {
        instance = doGet(NetworkInterface.getNetworkInterfaces());
      } catch (SocketException e) {
        throw new IllegalStateException("Fail to browse network interfaces", e);
      }

    }
    return instance;
  }

  static InetAddress doGet(Enumeration<NetworkInterface> ifaces) {
    InetAddress result = null;
    while (ifaces.hasMoreElements() && result == null) {
      NetworkInterface iface = ifaces.nextElement();
      Enumeration<InetAddress> addresses = iface.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress addr = addresses.nextElement();
        if (addr.isLoopbackAddress() && addr instanceof Inet4Address) {
          result = addr;
          break;
        }
      }
    }
    if (result == null) {
      throw new IllegalStateException("Impossible to get a IPv4 loopback address");
    }
    return result;
  }
}
