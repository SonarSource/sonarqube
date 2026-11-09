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

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.server.network.NetworkInterfaceProvider;
import org.springframework.security.util.matcher.InetAddressMatchers;

import static org.sonar.core.config.SecurityProperties.WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY;

/**
 * Shared address-blocking rules used at both webhook creation/update time ({@code WebhookSupport}) and
 * webhook delivery/redirect time ({@code WebhookCustomDns}), so the two validation points cannot drift apart.
 */
public final class WebhookAddressValidator {

  public static final String INVALID_ADDRESS_MESSAGE = "Invalid URL: when webhook validation is enabled, webhooks cannot send requests to "
    + "the SonarQube server itself, to other local addresses, to private network addresses or to cloud metadata addresses. "
    + "An administrator can allow private network addresses in the Allowed addresses setting (sonar.validateWebhooks.allowedAddresses).";

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

  private static final Logger LOG = LoggerFactory.getLogger(WebhookAddressValidator.class);
  private static final Pattern PREFIX_LENGTH = Pattern.compile("\\d+");
  private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
  private static final int MAX_LOGGED_ENTRY_LENGTH = 64;

  private WebhookAddressValidator() {
    // utility class
  }

  /**
   * Site-local (private network) addresses are blocked unless listed in the webhook allow-list. All other blocked
   * addresses stay blocked whatever the allow-list contains.
   */
  public static boolean isBlockedAddress(InetAddress address, NetworkInterfaceProvider networkInterfaceProvider, Configuration configuration)
    throws SocketException {
    if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
      || address.isMulticastAddress() || BLOCKED_METADATA_ADDRESSES.contains(address.getHostAddress())) {
      return true;
    }
    if (address.isSiteLocalAddress()) {
      return !isAllowed(address, configuration) || isLocalAddress(address, networkInterfaceProvider);
    }
    return isLocalAddress(address, networkInterfaceProvider);
  }

  /**
   * Entries are IP addresses or CIDR ranges, parsed as IP literals only, so reading the allow-list never triggers a DNS
   * resolution. An entry that is not a valid IP address or CIDR range never matches.
   * IPv4-mapped IPv6 CIDR entries are not supported.
   */
  private static boolean isAllowed(InetAddress address, Configuration configuration) {
    return Arrays.stream(configuration.getStringArray(WEBHOOKS_ALLOWED_ADDRESSES_PROPERTY))
      .map(String::trim)
      .filter(entry -> !entry.isEmpty())
      .anyMatch(entry -> isInRange(entry, address));
  }

  private static boolean isInRange(String entry, InetAddress address) {
    String[] parts = entry.split("/", 2);
    if (parts[0].contains("%") || (parts.length == 2 && !PREFIX_LENGTH.matcher(parts[1]).matches())) {
      return ignored(entry);
    }
    try {
      InetAddress network = InetAddresses.forString(parts[0]);
      if (parts.length == 2 && InetAddresses.isMappedIPv4Address(parts[0])) {
        return ignored(entry);
      }
      if (network.getAddress().length != address.getAddress().length) {
        return false;
      }
      return InetAddressMatchers.fromIpAddress(entry).matches(address);
    } catch (IllegalArgumentException e) {
      return ignored(entry);
    }
  }

  private static boolean ignored(String entry) {
    String printable = CONTROL_CHARACTERS.matcher(entry).replaceAll("_");
    if (printable.length() > MAX_LOGGED_ENTRY_LENGTH) {
      printable = printable.substring(0, MAX_LOGGED_ENTRY_LENGTH) + "...";
    }
    LOG.warn("Webhook allowed address '{}' is ignored: it is not a valid IP address or CIDR range", printable);
    return false;
  }

  private static boolean isLocalAddress(InetAddress address, NetworkInterfaceProvider networkInterfaceProvider) throws SocketException {
    return networkInterfaceProvider.getNetworkInterfaceAddresses().stream()
      .anyMatch(a -> a != null && a.equals(address));
  }
}
