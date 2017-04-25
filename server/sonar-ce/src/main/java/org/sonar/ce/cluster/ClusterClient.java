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

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.server.computation.taskprocessor.CeWorkerFactory;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class ClusterClient implements AutoCloseable, CeWorkerFactory.Listener {
  private static final String WORKER_UUIDS = "WORKER_UUIDS";
  private final CeWorkerFactory ceWorkerFactory;
  private final Map<String, List<String>> workerUUIDS;

  final HazelcastInstance hzInstance;

  public ClusterClient(ClusterClientProperties clusterClientProperties, CeWorkerFactory ceWorkerFactory) {
    checkState(clusterClientProperties.isEnabled(), "Cluster is not enabled");
    checkState(isNotEmpty(clusterClientProperties.getLocalEndPoint()), "LocalEndPoint have not been set");
    checkState(isNotEmpty(clusterClientProperties.getName()), "sonar.cluster.name is missing");

    ClientConfig hzConfig = new ClientConfig();
    hzConfig.getGroupConfig().setName(clusterClientProperties.getName());
    hzConfig.getNetworkConfig().addAddress(clusterClientProperties.getLocalEndPoint());

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

    // Create the Hazelcast instance
    this.hzInstance = HazelcastClient.newHazelcastClient(hzConfig);
    this.workerUUIDS = hzInstance.getReplicatedMap(WORKER_UUIDS);
    this.ceWorkerFactory = ceWorkerFactory;
    ceWorkerFactory.addListener(this);
    this.workerUUIDS.put(hzInstance.getLocalEndpoint().getUuid(), ceWorkerFactory.getWorkerUUIDs());
  }

  public List<String> getWorkerUUIDs() {
    return workerUUIDS.values().stream().flatMap(List::stream).collect(Collectors.toList());
  }

  @Override
  public void close() {
    workerUUIDS.remove(hzInstance.getLocalEndpoint().getUuid());
    // Shutdown Hazelcast properly
    hzInstance.shutdown();
  }

  @Override
  public void onChange() {
    this.workerUUIDS.put(hzInstance.getLocalEndpoint().getUuid(), ceWorkerFactory.getWorkerUUIDs());
  }
}
