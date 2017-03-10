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

package org.sonar.application.cluster;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessProperties;

/**
 * Properties of the cluster configuration
 */
public final class ClusterProperties {
  static final String DEFAULT_PORT = "9003";
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProperties.class);

  private final int port;
  private final boolean enabled;
  private final List<String> members;
  private final List<String> interfaces;
  private final String name;

  ClusterProperties(AppSettings appSettings) {
    port = appSettings.getProps().valueAsInt(ProcessProperties.CLUSTER_PORT);
    enabled = appSettings.getProps().valueAsBoolean(ProcessProperties.CLUSTER_ENABLED);
    interfaces = extractInterfaces(
      appSettings.getProps().value(ProcessProperties.CLUSTER_INTERFACES, "")
    );
    name = appSettings.getProps().value(ProcessProperties.CLUSTER_NAME);
    members = extractMembers(
      appSettings.getProps().value(ProcessProperties.CLUSTER_MEMBERS, "")
    );
  }

  int getPort() {
    return port;
  }

  boolean isEnabled() {
    return enabled;
  }

  List<String> getMembers() {
    return members;
  }

  List<String> getInterfaces() {
    return interfaces;
  }

  String getName() {
    return name;
  }

  void validate() {
    if (!enabled) {
      return;
    }
    // Name is required in cluster mode
    checkArgument(
      StringUtils.isNotEmpty(name),
      "Cluster have been enabled but a %s has not been defined.",
      ProcessProperties.CLUSTER_NAME
    );

    // Test validity of port
    checkArgument(
      port > 0 && port < 65_536,
      "Cluster port have been set to %d which is outside the range [1-65535].",
      port
    );

    // Test the interfaces parameter
    try {
      List<String> localInterfaces = findAllLocalIPs();

      interfaces.forEach(
        inet -> checkArgument(
          StringUtils.isEmpty(inet) || localInterfaces.contains(inet),
          "Interface %s is not available on this machine.",
          inet
        )
      );
    } catch (SocketException e) {
      LOGGER.warn("Unable to retrieve network interfaces. Interfaces won't be checked", e);
    }
  }

  private static List<String> extractMembers(final String members) {
    List<String> result = new ArrayList<>();
    for (String member : members.split(",")) {
      if (StringUtils.isNotEmpty(member)) {
        if (!member.contains(":")) {
          result.add(
            String.format("%s:%s", member, DEFAULT_PORT)
          );
        } else {
          result.add(member);
        }
      }
    }
    return result;
  }

  private static List<String> extractInterfaces(final String interfaces) {
    List<String> result = new ArrayList<>();
    for (String iface : interfaces.split(",")) {
      if (StringUtils.isNotEmpty(iface)) {
        result.add(iface);
      }
    }
    return result;
  }

  private static List<String> findAllLocalIPs() throws SocketException {
    Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
    List<String> localInterfaces = new ArrayList<>();

    while (netInterfaces.hasMoreElements()) {
      NetworkInterface networkInterface = netInterfaces.nextElement();
      Enumeration<InetAddress> ips = networkInterface.getInetAddresses();
      while (ips.hasMoreElements()) {
        InetAddress ip = ips.nextElement();
        localInterfaces.add(ip.getHostAddress());
      }
    }
    return localInterfaces;
  }

  private static void checkArgument(boolean expression,
                                    @Nullable String messageTemplate,
                                    @Nullable Object... args) {
    if (!expression) {
      throw new IllegalArgumentException(String.format(messageTemplate, args));
    }
  }
}
