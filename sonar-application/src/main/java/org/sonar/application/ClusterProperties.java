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

package org.sonar.application;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;

/**
 * Properties of the cluster configuration
 */
final class ClusterProperties {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProperties.class);

  private final int port;
  private final boolean enabled;
  private final boolean portAutoincrement;
  private final List<String> members;
  private final List<String> interfaces;
  private final String name;
  private final String logLevel;

  ClusterProperties(@Nonnull Props props) {
    port = props.valueAsInt(ClusterParameters.PORT.getName(), ClusterParameters.PORT.getDefaultValueAsInt());
    enabled = props.valueAsBoolean(ClusterParameters.ENABLED.getName(), ClusterParameters.ENABLED.getDefaultValueAsBoolean());
    portAutoincrement = props.valueAsBoolean(ClusterParameters.PORT_AUTOINCREMENT.getName(), ClusterParameters.PORT_AUTOINCREMENT.getDefaultValueAsBoolean());
    interfaces = extractInterfaces(
      props.value(ClusterParameters.INTERFACES.getName(), ClusterParameters.INTERFACES.getDefaultValue())
    );
    name = props.value(ClusterParameters.NAME.getName(), ClusterParameters.NAME.getDefaultValue());
    logLevel = props.value(ClusterParameters.HAZELCAST_LOG_LEVEL.getName(), ClusterParameters.HAZELCAST_LOG_LEVEL.getDefaultValue());
    members = extractMembers(
      props.value(ClusterParameters.MEMBERS.getName(), ClusterParameters.MEMBERS.getDefaultValue())
    );
  }

  void populateProps(@Nonnull Props props) {
    props.set(ClusterParameters.PORT.getName(), Integer.toString(port));
    props.set(ClusterParameters.ENABLED.getName(), Boolean.toString(enabled));
    props.set(ClusterParameters.PORT_AUTOINCREMENT.getName(), Boolean.toString(portAutoincrement));
    props.set(ClusterParameters.INTERFACES.getName(), interfaces.stream().collect(Collectors.joining(",")));
    props.set(ClusterParameters.NAME.getName(), name);
    props.set(ClusterParameters.HAZELCAST_LOG_LEVEL.getName(), logLevel);
    props.set(ClusterParameters.MEMBERS.getName(), members.stream().collect(Collectors.joining(",")));
  }

  int getPort() {
    return port;
  }

  boolean isEnabled() {
    return enabled;
  }

  boolean isPortAutoincrement() {
    return portAutoincrement;
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

  String getLogLevel() {
    return logLevel;
  }

  void validate() {
    if (!enabled) {
      return;
    }
    // Name is required in cluster mode
    Preconditions.checkArgument(
      StringUtils.isNotEmpty(name),
      "Cluster have been enabled but a %s has not been defined.",
      ClusterParameters.NAME.getName()
    );

    // Test validity of port
    Preconditions.checkArgument(
      port > 0 && port < 65_536,
      "Cluster port have been set to %s which is outside the range [1-65535].",
      port
    );

    // Test the interfaces parameter
    try {
      List<String> localInterfaces = findAllLocalIPs();

      interfaces.forEach(
        inet -> Preconditions.checkArgument(
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
            String.format("%s:%s", member, ClusterParameters.PORT.getDefaultValue())
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
}
