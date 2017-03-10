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

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.net.InetSocketAddress;
import java.util.Collection;

public class HazelcastHelper {
  static HazelcastInstance createHazelcastNode(AppStateClusterImpl appStateCluster) {
    Config hzConfig = new Config()
      .setInstanceName(appStateCluster.hzInstance.getName() + "_1");

    // Configure the network instance
    NetworkConfig netConfig = hzConfig.getNetworkConfig();
    netConfig.setPort(9003).setPortAutoIncrement(true);
    Collection<String> interfaces = appStateCluster.hzInstance.getConfig().getNetworkConfig().getInterfaces().getInterfaces();
    if (!interfaces.isEmpty()) {
      netConfig.getInterfaces().addInterface(
        interfaces.iterator().next()
      );
    }

    // Only allowing TCP/IP configuration
    JoinConfig joinConfig = netConfig.getJoin();
    joinConfig.getAwsConfig().setEnabled(false);
    joinConfig.getMulticastConfig().setEnabled(false);
    joinConfig.getTcpIpConfig().setEnabled(true);

    InetSocketAddress socketAddress = (InetSocketAddress) appStateCluster.hzInstance.getLocalEndpoint().getSocketAddress();
    joinConfig.getTcpIpConfig().addMember(
      String.format("%s:%d",
        socketAddress.getHostString(),
        socketAddress.getPort()
      )
    );

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

    // We are not using the partition group of Hazelcast, so disabling it
    hzConfig.getPartitionGroupConfig().setEnabled(false);

    return Hazelcast.newHazelcastInstance(hzConfig);
  }

  static HazelcastInstance createHazelcastClient(AppStateClusterImpl appStateCluster) {
    ClientConfig clientConfig = new ClientConfig();
    InetSocketAddress socketAddress = (InetSocketAddress) appStateCluster.hzInstance.getLocalEndpoint().getSocketAddress();

    clientConfig.getNetworkConfig().getAddresses().add(
      String.format("%s:%d",
        socketAddress.getHostString(),
        socketAddress.getPort()
      ));
    clientConfig.getGroupConfig().setName(appStateCluster.hzInstance.getConfig().getGroupConfig().getName());
    return HazelcastClient.newHazelcastClient(clientConfig);
  }
}
