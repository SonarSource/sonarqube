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

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.AppState;
import org.sonar.application.AppStateListener;
import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

public class AppStateClusterImpl implements AppState {
  private static Logger LOGGER = LoggerFactory.getLogger(AppStateClusterImpl.class);

  private final Map<ProcessId, Boolean> localProcesses = new EnumMap<>(ProcessId.class);
  private final HazelcastCluster hazelcastCluster;

  public AppStateClusterImpl(AppSettings appSettings) {
    ClusterProperties clusterProperties = new ClusterProperties(appSettings);
    clusterProperties.validate();

    if (!clusterProperties.isEnabled()) {
      throw new IllegalStateException("Cluster is not enabled on this instance");
    }

    hazelcastCluster = HazelcastCluster.create(clusterProperties);
    // Add the local endpoint to be used by processes
    appSettings.getProps().set(ProcessProperties.CLUSTER_LOCALENDPOINT, hazelcastCluster.getLocalEndPoint());
    appSettings.getProps().set(ProcessProperties.CLUSTER_MEMBERUUID, hazelcastCluster.getLocalUUID());

    String members = hazelcastCluster.getMembers().stream().collect(Collectors.joining(","));
    LOGGER.info("Joined the cluster [{}] that contains the following hosts : [{}]", hazelcastCluster.getName(), members);
  }

  @Override
  public void addListener(@Nonnull AppStateListener listener) {
    hazelcastCluster.addListener(listener);
  }

  @Override
  public boolean isOperational(@Nonnull ProcessId processId, boolean local) {
    if (local) {
      return localProcesses.computeIfAbsent(processId, p -> false);
    }
    return hazelcastCluster.isOperational(processId);
  }

  @Override
  public void setOperational(@Nonnull ProcessId processId) {
    localProcesses.put(processId, true);
    hazelcastCluster.setOperational(processId);
  }

  @Override
  public boolean tryToLockWebLeader() {
    return hazelcastCluster.tryToLockWebLeader();
  }

  @Override
  public void reset() {
    throw new IllegalStateException("state reset is not supported in cluster mode");
  }

  @Override
  public void close() {
    hazelcastCluster.close();
  }

  @Override
  public void registerSonarQubeVersion(String sonarqubeVersion) {
    hazelcastCluster.registerSonarQubeVersion(sonarqubeVersion);
  }

  @Override
  public Optional<String> getLeaderHostName() {
    return hazelcastCluster.getLeaderHostName();
  }

  HazelcastCluster getHazelcastCluster() {
    return hazelcastCluster;
  }

  /**
   * Only used for testing purpose
   *
   * @param logger
   */
  static void setLogger(Logger logger) {
    AppStateClusterImpl.LOGGER = logger;
  }

}
