/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.webhook;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;
import org.sonar.server.network.NetworkInterfaceProvider;

/**
 * Shared address-blocking rules used at both webhook creation/update time ({@code WebhookSupport}) and
 * webhook delivery/redirect time ({@code WebhookCustomDns}), so the two validation points cannot drift apart.
 */
public final class WebhookAddressValidator {

  public static final String INVALID_ADDRESS_MESSAGE = "Invalid URL: loopback, wildcard, link-local, site-local, multicast, cloud metadata "
    + "addresses and addresses of the SonarQube server itself are not allowed for webhooks.";

  /**
   * Well-known cloud provider metadata endpoints that fall outside the JDK's {@link InetAddress} address-class
   * predicates (e.g. IPv6 unique-local addresses, RFC 6598 shared address space), and are therefore not caught by
   * {@code isLoopbackAddress()}/{@code isLinkLocalAddress()}/{@code isMulticastAddress()}.
   */
  private static final Set<String> BLOCKED_METADATA_ADDRESSES = Set.of(
    // AWS IMDS (IPv6)
    "fd00:ec2:0:0:0:0:0:254",
    // Alibaba Cloud metadata
    "100.100.100.200"
  );

  private WebhookAddressValidator() {
    // utility class
  }

  public static boolean isBlockedAddress(InetAddress address, NetworkInterfaceProvider networkInterfaceProvider) throws SocketException {
    return address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
      || address.isSiteLocalAddress() || address.isMulticastAddress() || BLOCKED_METADATA_ADDRESSES.contains(address.getHostAddress())
      || isLocalAddress(address, networkInterfaceProvider);
  }

  private static boolean isLocalAddress(InetAddress address, NetworkInterfaceProvider networkInterfaceProvider) throws SocketException {
    return networkInterfaceProvider.getNetworkInterfaceAddresses().stream()
      .anyMatch(a -> a != null && a.equals(address));
  }
}
