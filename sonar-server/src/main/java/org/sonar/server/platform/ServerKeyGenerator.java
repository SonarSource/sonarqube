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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.Logs;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Enumeration;

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

  public String generate(String organization, String baseUrl) {
    return generate(organization, baseUrl, null);
  }

  public String generate(String organization, String baseUrl, String previousKey) {
    String key = null;
    if (StringUtils.isNotBlank(organization) && StringUtils.isNotBlank(baseUrl)) {
      InetAddress address = extractAddressFromUrl(baseUrl);
      if (address != null && isFixed(address) && isOwner(address)) {
        key = toKey(organization, address);
      }
    }
    log(previousKey, key);
    return key;
  }

  boolean isOwner(InetAddress address) {
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress ownedAddress = addresses.nextElement();
          if (ownedAddress.equals(address)) {
            return true;
          }
        }
      }
    } catch (SocketException e) {
      LoggerFactory.getLogger(ServerKeyGenerator.class).error("Fail to verify server key. Network interfaces can't be browsed.", e);
    }
    return false;
  }

  boolean isFixed(InetAddress address) {
    // Loopback addresses are in the range 127/8.
    // Link local addresses are in the range 169.254/16 (IPv4) or fe80::/10 (IPv6). They are "autoconfiguration" addresses.
    // They can assigned pseudorandomly, so they don't guarantee to be the same between two server startups.
    return acceptPrivateAddress || (!address.isLoopbackAddress() && !address.isLinkLocalAddress());
  }

  void log(String previousKey, String newKey) {
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

  InetAddress extractAddressFromUrl(String baseUrl) {
    if (StringUtils.isBlank(baseUrl)) {
      return null;
    }
    try {
      URL url = new URL(baseUrl);
      return InetAddress.getByName(url.getHost());

    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Server base URL is malformed: " + baseUrl, e);

    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Server base URL is unknown: " + baseUrl, e);
    }
  }

  String toKey(String organization, InetAddress address) {
    String key = new StringBuilder().append(organization).append("-").append(address.getHostAddress()).toString();
    try {
      return VERSION + DigestUtils.shaHex(key.getBytes("UTF-8")).substring(0, CHECKSUM_SIZE);

    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Organization is not UTF-8 encoded: " + organization, e);
    }
  }
}
