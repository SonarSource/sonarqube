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
import com.hazelcast.core.HazelcastInstance;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.sonar.application.config.TestAppSettings;

import static org.sonar.process.cluster.ClusterProperties.CLUSTER_ENABLED;
import static org.sonar.process.cluster.ClusterProperties.CLUSTER_NAME;
import static org.sonar.process.cluster.ClusterProperties.CLUSTER_NODE_TYPE;

public class HazelcastClusterTestHelper {

  // Be careful this test won't work if parallel tests is used
  private static final List<HazelcastInstance> HAZELCAST_INSTANCES = new ArrayList<>();

  static HazelcastInstance createHazelcastClient(HazelcastCluster hzCluster) {
    ClientConfig clientConfig = new ClientConfig();
    InetSocketAddress socketAddress = (InetSocketAddress) hzCluster.hzInstance.getLocalEndpoint().getSocketAddress();

    clientConfig.getNetworkConfig().getAddresses().add(
      String.format("%s:%d",
        socketAddress.getHostString(),
        socketAddress.getPort()));
    clientConfig.getGroupConfig().setName(hzCluster.getName());
    HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
    HAZELCAST_INSTANCES.add(hazelcastInstance);
    return hazelcastInstance;
  }

  static HazelcastInstance createHazelcastClient(ClusterAppStateImpl appStateCluster) {
    return createHazelcastClient(appStateCluster.getHazelcastCluster());
  }

  static void closeAllHazelcastClients() {
    HAZELCAST_INSTANCES.stream().forEach(
      hz -> {
        try {
          hz.shutdown();
        } catch (Exception ex) {
          // Ignore it
        }
      });
  }

  static TestAppSettings newApplicationSettings() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(CLUSTER_NAME, "sonarqube");
    settings.set(CLUSTER_NODE_TYPE, "application");
    return settings;
  }

  static TestAppSettings newSearchSettings() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(CLUSTER_ENABLED, "true");
    settings.set(CLUSTER_NAME, "sonarqube");
    settings.set(CLUSTER_NODE_TYPE, "search");
    return settings;
  }
}
