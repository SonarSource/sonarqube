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
package org.sonar.application.health;

import java.util.Random;
import org.sonar.api.utils.System2;
import org.sonar.cluster.ClusterProperties;
import org.sonar.cluster.health.NodeDetails;
import org.sonar.cluster.health.NodeHealth;
import org.sonar.cluster.health.NodeHealthProvider;
import org.sonar.process.Props;

import static org.sonar.cluster.ClusterProperties.CLUSTER_NODE_PORT;

public class SearchNodeHealthProvider implements NodeHealthProvider {
  private final System2 system2;
  private final NodeDetails nodeDetails;

  public SearchNodeHealthProvider(Props props, System2 system2) {
    this.system2 = system2;
    this.nodeDetails = NodeDetails.newNodeDetailsBuilder()
      .setType(NodeDetails.Type.SEARCH)
      .setName(props.nonNullValue(ClusterProperties.CLUSTER_NAME) + new Random().nextInt(999))
      // TODO read sonar.cluster.node.host
      .setHost("host hardcoded for now")
      .setPort(Integer.valueOf(props.nonNullValue(CLUSTER_NODE_PORT)))
      // TODO is now good enough?
      .setStarted(system2.now())
      .build();
  }

  @Override
  public NodeHealth get() {
    return NodeHealth.newNodeHealthBuilder()
      .setStatus(NodeHealth.Status.GREEN)
      .setDetails(nodeDetails)
      .setDate(system2.now())
      .build();
  }
}
