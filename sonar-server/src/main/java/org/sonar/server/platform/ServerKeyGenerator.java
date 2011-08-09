/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.platform;

import com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.Logs;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

public class ServerKeyGenerator {

  /**
   * Increment this version each time the algorithm is changed. Do not exceed 9.
   */
  static final String VERSION = "1";

  private static final int CHECKSUM_SIZE = 9;

  public String generate(String organization) {
    return generate(organization, null);
  }

  public String generate(String organization, String previousKey) {
    List<ServerKey> serverKeys = generateForOrganization(organization);
    String external = null;
    String best = null;
    for (ServerKey serverKey : serverKeys) {
      if (StringUtils.equals(previousKey, serverKey.getKey())) {
        best = serverKey.getKey();
      }
      if (serverKey.isExternal()) {
        // External addresses are prefered to internal addresses.
        external = serverKey.getKey();
      }
    }
    if (best == null) {
      if (external!=null) {
        best = external;
      } else if (!serverKeys.isEmpty()) {
        best = serverKeys.get(0).getKey();
      }
    }
    log(previousKey, best);
    return best;
  }

  private void log(String previousKey, String newKey) {
    if (StringUtils.isNotBlank(newKey) && StringUtils.isNotBlank(previousKey) && !previousKey.equals(newKey)) {
      LoggerFactory.getLogger(getClass()).warn("Server key has changed. Licensed plugins may be disabled. "
          + "Please check the organization name (Settings page) and the server IP addresses.");
    }
    if (StringUtils.isNotBlank(newKey)) {
      Logs.INFO.info("Server key: " + newKey);

    } else if (StringUtils.isNotBlank(previousKey)) {
      LoggerFactory.getLogger(getClass()).warn("Server key has been removed. Licensed plugins may be disabled. "
          + "Please check the organization name (Settings page) and the server IP addresses.");
    }
  }

  List<ServerKey> generateForOrganization(String organization) {
    List<ServerKey> keys = Lists.newArrayList();
    if (StringUtils.isNotBlank(organization)) {
      try {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
          NetworkInterface networkInterface = networkInterfaces.nextElement();
          Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
          while (addresses.hasMoreElements()) {
            ServerKey key = new ServerKey(organization, addresses.nextElement());
            if (key.isValid()) {
              keys.add(key);
            }
          }
        }
      } catch (SocketException e) {
        LoggerFactory.getLogger(getClass()).error("Fail to generate server key. Network interfaces can't be browsed.", e);
      }
    }
    return keys;
  }

  static class ServerKey {
    private String organization;
    private InetAddress address;

    ServerKey(String organization, InetAddress address) {
      this.organization = organization;
      this.address = address;
    }

    boolean isExternal() {
      return !address.isLoopbackAddress() && !address.isSiteLocalAddress() && !address.isLinkLocalAddress();
    }

    boolean isValid() {
      // Loopback addresses are in the range 127/8.
      // Link local addresses are in the range 169.254/16 (IPv4) or fe80::/10 (IPv6). They are "autoconfiguration" addresses.
      // They can assigned pseudorandomly, so they don't guarantee to be the same between two server startups.
      return !address.isLoopbackAddress() && !address.isLinkLocalAddress();
    }

    String getKey() {
      String key = new StringBuilder().append(organization).append("-").append(address.getHostAddress()).toString();
      return VERSION + DigestUtils.shaHex(key.getBytes()).substring(0, CHECKSUM_SIZE);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ServerKey serverKey = (ServerKey) o;
      if (!address.equals(serverKey.address)) {
        return false;
      }
      if (!organization.equals(serverKey.organization)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = organization.hashCode();
      result = 31 * result + address.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return getKey();
    }
  }
}
