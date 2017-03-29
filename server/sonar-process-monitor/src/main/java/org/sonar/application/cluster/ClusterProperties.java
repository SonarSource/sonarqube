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
  private final List<String> hosts;
  private final List<String> networkInterfaces;
  private final String name;

  ClusterProperties(AppSettings appSettings) {
    port = appSettings.getProps().valueAsInt(ProcessProperties.CLUSTER_PORT);
    enabled = appSettings.getProps().valueAsBoolean(ProcessProperties.CLUSTER_ENABLED);
    networkInterfaces = extractNetworkInterfaces(
      appSettings.getProps().value(ProcessProperties.CLUSTER_NETWORK_INTERFACES, "")
    );
    name = appSettings.getProps().nonNullValue(ProcessProperties.CLUSTER_NAME);
    hosts = extractHosts(
      appSettings.getProps().value(ProcessProperties.CLUSTER_HOSTS, "")
    );
  }

  int getPort() {
    return port;
  }

  boolean isEnabled() {
    return enabled;
  }

  List<String> getHosts() {
    return hosts;
  }

  List<String> getNetworkInterfaces() {
    return networkInterfaces;
  }

  String getName() {
    return name;
  }

  void validate() {
    if (!enabled) {
      return;
    }

    // Test validity of port
    checkArgument(
      port > 0 && port < 65_536,
      "Cluster port have been set to %d which is outside the range [1-65535].",
      port
    );

    // Test the networkInterfaces parameter
    try {
      List<String> localInterfaces = findAllLocalIPs();

      networkInterfaces.forEach(
        inet -> checkArgument(
          StringUtils.isEmpty(inet) || localInterfaces.contains(inet),
          "Interface %s is not available on this machine.",
          inet
        )
      );
    } catch (SocketException e) {
      LOGGER.warn("Unable to retrieve network networkInterfaces. Interfaces won't be checked", e);
    }
  }

  private static List<String> extractHosts(final String hosts) {
    List<String> result = new ArrayList<>();
    for (String host : hosts.split(",")) {
      if (StringUtils.isNotEmpty(host)) {
        if (!host.contains(":")) {
          result.add(
            String.format("%s:%s", host, DEFAULT_PORT)
          );
        } else {
          result.add(host);
        }
      }
    }
    return result;
  }

  private static List<String> extractNetworkInterfaces(final String networkInterfaces) {
    List<String> result = new ArrayList<>();
    for (String iface : networkInterfaces.split(",")) {
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
