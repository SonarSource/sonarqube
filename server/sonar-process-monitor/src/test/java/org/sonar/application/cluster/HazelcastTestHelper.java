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
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.ProcessProperties;

public class HazelcastTestHelper {

  static HazelcastInstance createHazelcastClient(HazelcastCluster hzCluster) {
    ClientConfig clientConfig = new ClientConfig();
    InetSocketAddress socketAddress = (InetSocketAddress) hzCluster.hzInstance.getLocalEndpoint().getSocketAddress();

    clientConfig.getNetworkConfig().getAddresses().add(
      String.format("%s:%d",
        socketAddress.getHostString(),
        socketAddress.getPort()
      ));
    clientConfig.getGroupConfig().setName(hzCluster.getName());
    return HazelcastClient.newHazelcastClient(clientConfig);
  }

  static HazelcastInstance createHazelcastClient(AppStateClusterImpl appStateCluster) {
    return createHazelcastClient(appStateCluster.getHazelcastCluster());
  }

  static TestAppSettings newClusterSettings() {
    TestAppSettings settings = new TestAppSettings();
    settings.set(ProcessProperties.CLUSTER_ENABLED, "true");
    settings.set(ProcessProperties.CLUSTER_NAME, "sonarqube");
    return settings;
  }
}
