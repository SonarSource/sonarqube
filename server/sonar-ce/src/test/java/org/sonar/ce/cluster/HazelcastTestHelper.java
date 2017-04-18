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

package org.sonar.ce.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Client;
import com.hazelcast.core.ClientListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.net.InetAddress;

import static org.sonar.process.NetworkUtils.getHostName;
import static org.sonar.process.cluster.ClusterObjectKeys.CLIENT_UUIDS;

public class HazelcastTestHelper {

  public static HazelcastInstance createHazelcastCluster(String clusterName, int port) {
    Config hzConfig = new Config();
    hzConfig.getGroupConfig().setName(clusterName);

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
    hzConfig.getMemberAttributeConfig().setStringAttribute("HOSTNAME", getHostName());

    // We are not using the partition group of Hazelcast, so disabling it
    hzConfig.getPartitionGroupConfig().setEnabled(false);
    HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
    hzInstance.getClientService().addClientListener(new ConnectedClientListener(hzInstance));
    return hzInstance;
  }

  private static class ConnectedClientListener implements ClientListener {
    private final HazelcastInstance hzInstance;

    private ConnectedClientListener(HazelcastInstance hzInstance) {
      this.hzInstance = hzInstance;
    }

    @Override
    public void clientConnected(Client client) {
      hzInstance.getSet(CLIENT_UUIDS).add(client.getUuid());
    }

    @Override
    public void clientDisconnected(Client client) {
      hzInstance.getSet(CLIENT_UUIDS).remove(client.getUuid());
    }
  }
}
