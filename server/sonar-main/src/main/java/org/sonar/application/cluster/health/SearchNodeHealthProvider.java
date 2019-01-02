/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.cluster.health;

import org.sonar.application.cluster.ClusterAppState;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;
import org.sonar.process.cluster.health.NodeHealthProvider;

import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HZ_PORT;

public class SearchNodeHealthProvider implements NodeHealthProvider {

  private final ClusterAppState clusterAppState;
  private final NodeDetails nodeDetails;

  public SearchNodeHealthProvider(Props props, ClusterAppState clusterAppState, NetworkUtils networkUtils) {
    this(props, clusterAppState, networkUtils, new Clock());
  }

  SearchNodeHealthProvider(Props props, ClusterAppState clusterAppState, NetworkUtils networkUtils, Clock clock) {
    this.clusterAppState = clusterAppState;
    this.nodeDetails = NodeDetails.newNodeDetailsBuilder()
      .setType(NodeDetails.Type.SEARCH)
      .setName(props.nonNullValue(CLUSTER_NODE_NAME.getKey()))
      .setHost(getHost(props, networkUtils))
      .setPort(Integer.valueOf(props.nonNullValue(CLUSTER_NODE_HZ_PORT.getKey())))
      .setStartedAt(clock.now())
      .build();
  }

  private static String getHost(Props props, NetworkUtils networkUtils) {
    String host = props.value(CLUSTER_NODE_HOST.getKey());
    if (host != null && !host.isEmpty()) {
      return host;
    }
    return networkUtils.getHostname();
  }

  @Override
  public NodeHealth get() {
    NodeHealth.Builder builder = NodeHealth.newNodeHealthBuilder();
    if (clusterAppState.isOperational(ProcessId.ELASTICSEARCH, true)) {
      builder.setStatus(NodeHealth.Status.GREEN);
    } else {
      builder.setStatus(NodeHealth.Status.RED)
        .addCause("Elasticsearch is not operational");
    }
    return builder
      .setDetails(nodeDetails)
      .build();
  }

  static class Clock {
    long now() {
      return System.currentTimeMillis();
    }
  }
}
