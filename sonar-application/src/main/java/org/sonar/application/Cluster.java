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

import com.google.common.annotations.VisibleForTesting;
import com.hazelcast.cluster.ClusterState;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import javax.annotation.Nonnull;

/**
 * Manager for the cluster communication between Main Processes
 */
public class Cluster implements AutoCloseable {
  /**
   * The Hazelcast instance.
   */
  @VisibleForTesting
  final HazelcastInstance hazelcastInstance;

  /**
   * Instantiates a new Cluster.
   *
   * @param clusterProperties The properties of the cluster read from configuration
   */
  protected Cluster(@Nonnull ClusterProperties clusterProperties) {
    if (clusterProperties.isEnabled()) {
      Config hzConfig = new Config();
      // Configure the network instance
      NetworkConfig netConfig = hzConfig.getNetworkConfig();
      netConfig.setPort(clusterProperties.getPort())
        .setPortAutoIncrement(clusterProperties.isPortAutoincrement());

      if (!clusterProperties.getInterfaces().isEmpty()) {
        netConfig.getInterfaces()
          .setEnabled(true)
          .setInterfaces(clusterProperties.getInterfaces());
      }

      // Only allowing TCP/IP configuration
      JoinConfig joinConfig = netConfig.getJoin();
      joinConfig.getAwsConfig().setEnabled(false);
      joinConfig.getMulticastConfig().setEnabled(false);
      joinConfig.getTcpIpConfig().setEnabled(true);
      joinConfig.getTcpIpConfig().setMembers(clusterProperties.getMembers());

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

      hazelcastInstance = Hazelcast.newHazelcastInstance(hzConfig);
    } else {
      hazelcastInstance = null;
    }
  }

  /**
   * Is the cluster active
   *
   * @return the boolean
   */
  public boolean isActive() {
    return hazelcastInstance != null && hazelcastInstance.getCluster().getClusterState() == ClusterState.ACTIVE;
  }

  @Override
  public void close() {
    if (hazelcastInstance != null) {
      hazelcastInstance.shutdown();
    }
  }
}
