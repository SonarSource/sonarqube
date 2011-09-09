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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

/**
 * @since 2.11
 */
public class ServerKeyGenerator {

  /**
   * Increment this version each time the algorithm is changed. Do not exceed 9.
   */
  static final String VERSION = "1";

  static final int CHECKSUM_SIZE = 9;

  private final boolean acceptPrivateAddress;

  public ServerKeyGenerator() {
    this(false);
  }

  ServerKeyGenerator(boolean acceptPrivateAddress) {
    this.acceptPrivateAddress = acceptPrivateAddress;
  }

  public String generate(String organization, String ipAddress) {
    String key = null;
    if (StringUtils.isNotBlank(organization) && StringUtils.isNotBlank(ipAddress)) {
      InetAddress inetAddress = toValidAddress(ipAddress);
      if (inetAddress != null) {
        key = toKey(organization, inetAddress);
      }
    }
    return key;
  }

  boolean isFixed(InetAddress address) {
    // Loopback addresses are in the range 127/8.
    // Link local addresses are in the range 169.254/16 (IPv4) or fe80::/10 (IPv6). They are "autoconfiguration" addresses.
    // They can assigned pseudorandomly, so they don't guarantee to be the same between two server startups.
    return acceptPrivateAddress || (!address.isLoopbackAddress() && !address.isLinkLocalAddress());
  }

  String toKey(String organization, InetAddress address) {
    String key = new StringBuilder().append(organization).append("-").append(address.getHostAddress()).toString();
    try {
      return VERSION + DigestUtils.shaHex(key.getBytes("UTF-8")).substring(0, CHECKSUM_SIZE);

    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Organization is not UTF-8 encoded: " + organization, e);
    }
  }

  public InetAddress toValidAddress(String ipAddress) {
    if (StringUtils.isNotBlank(ipAddress)) {
      List<InetAddress> validAddresses = getAvailableAddresses();
      try {
        InetAddress address = InetAddress.getByName(ipAddress);
        if (validAddresses.contains(address)) {
          return address;
        }
      } catch (UnknownHostException e) {
        // ignore, not valid property
      }
    }
    return null;
  }

  public List<InetAddress> getAvailableAddresses() {
    List<InetAddress> result = Lists.newArrayList();
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress ownedAddress = addresses.nextElement();
          if (isFixed(ownedAddress)) {
            result.add(ownedAddress);
          }
        }
      }
    } catch (SocketException e) {
      LoggerFactory.getLogger(ServerKeyGenerator.class).error("Fail to browse network interfaces", e);
    }
    return result;
  }
}
