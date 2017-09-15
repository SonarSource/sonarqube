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

package org.sonar.ce.container;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.net.InetAddress;

/**
 * TODO move outside main sources
 */
public class HazelcastTestHelper {
  private HazelcastTestHelper() {
    // prevents instantiation
  }

  public static HazelcastInstance createHazelcastCluster(String hostname, int port) {
    Config hzConfig = new Config();
    hzConfig.getGroupConfig().setName("sonarqube");

    // Configure the network instance
    NetworkConfig netConfig = hzConfig.getNetworkConfig();
    netConfig
      .setPort(port)
      .setReuseAddress(true);

    netConfig.getInterfaces()
      .setEnabled(true)
      .addInterface(InetAddress.getLoopbackAddress().getHostAddress());

    // Only allowing TCP/IP configuration
    JoinConfig joinConfig = netConfig.getJoin();
    joinConfig.getAwsConfig().setEnabled(false);
    joinConfig.getMulticastConfig().setEnabled(false);
    joinConfig.getTcpIpConfig().setEnabled(true);

    // Tweak HazelCast configuration
    hzConfig
      // Increase the number of tries
      .setProperty("hazelcast.tcp.join.port.try.count", "10")
      // Don't bind on all interfaces
      .setProperty("hazelcast.socket.bind.any", "false")
      // Don't phone home
      .setProperty("hazelcast.phone.home.enabled", "false")
      // Use slf4j for logging
      .setProperty("hazelcast.logging.type", "slf4j");

    // Trying to resolve the hostname
    hzConfig.getMemberAttributeConfig().setStringAttribute("HOSTNAME", hostname);

    // We are not using the partition group of Hazelcast, so disabling it
    hzConfig.getPartitionGroupConfig().setEnabled(false);
    HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
    return hzInstance;
  }
}
