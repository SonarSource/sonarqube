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
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class ServerIdGenerator {

  private static final Pattern ORGANIZATION_PATTERN = Pattern.compile("[a-zA-Z0-9]+[a-zA-Z0-9 ]*");

  /**
   * Increment this version each time the algorithm is changed. Do not exceed 9.
   */
  static final String VERSION = "1";

  static final int CHECKSUM_SIZE = 14;

  private final boolean acceptPrivateAddress;

  public ServerIdGenerator() {
    this(false);
  }

  @VisibleForTesting
  ServerIdGenerator(boolean acceptPrivateAddress) {
    this.acceptPrivateAddress = acceptPrivateAddress;
  }

  public boolean validate(String organizationName, String ipAddress, String expectedServerId) {
    String organization = organizationName.trim();
    String ip = ipAddress.trim();
    if (isBlank(ip) || isBlank(organization) || !isValidOrganizationName(organization)) {
      return false;
    }

    InetAddress inetAddress = toValidAddress(ip);

    return inetAddress != null
      && Objects.equals(expectedServerId, toId(organization, inetAddress));
  }

  public String generate(String organizationName, String ipAddress) {
    String organization = organizationName.trim();
    String ip = ipAddress.trim();
    checkArgument(isNotBlank(organization), "Organization name must not be null or empty");
    checkArgument(isValidOrganizationName(organization), "Organization name is invalid. Alpha numeric characters and space only are allowed. '%s' was provided.", organization);
    checkArgument(isNotBlank(ip), "IP must not be null or empty");

    InetAddress inetAddress = toValidAddress(ip);
    checkArgument(inetAddress != null, "Invalid IP '%s'", ip);

    return toId(organization, inetAddress);
  }

  static boolean isValidOrganizationName(String organization) {
    return ORGANIZATION_PATTERN.matcher(organization).matches();
  }

  boolean isFixed(InetAddress address) {
    // Loopback addresses are in the range 127/8.
    // Link local addresses are in the range 169.254/16 (IPv4) or fe80::/10 (IPv6). They are "autoconfiguration" addresses.
    // They can assigned pseudorandomly, so they don't guarantee to be the same between two server startups.
    return acceptPrivateAddress || (!address.isLoopbackAddress() && !address.isLinkLocalAddress());
  }

  static String toId(String organization, InetAddress address) {
    String id = new StringBuilder().append(organization).append("-").append(address.getHostAddress()).toString();
    return VERSION + DigestUtils.sha1Hex(id.getBytes(StandardCharsets.UTF_8)).substring(0, CHECKSUM_SIZE);
  }

  @CheckForNull
  private InetAddress toValidAddress(String ipAddress) {
    if (isNotBlank(ipAddress)) {
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
    List<InetAddress> result = new ArrayList<>();
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
      Loggers.get(ServerIdGenerator.class).error("Fail to browse network interfaces", e);
    }
    return result;
  }
}
