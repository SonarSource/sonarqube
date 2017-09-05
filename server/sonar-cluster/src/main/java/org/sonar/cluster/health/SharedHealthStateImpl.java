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
package org.sonar.cluster.health;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cluster.localclient.HazelcastClient;

import static java.util.Objects.requireNonNull;

public class SharedHealthStateImpl implements SharedHealthState {
  private static final String SQ_HEALTH_STATE_REPLICATED_MAP_IDENTIFIER = "sq_health_state";
  private static final Logger LOG = Loggers.get(SharedHealthStateImpl.class);

  private final HazelcastClient hazelcastClient;

  public SharedHealthStateImpl(HazelcastClient hazelcastClient) {
    this.hazelcastClient = hazelcastClient;
  }

  @Override
  public void writeMine(NodeHealth nodeHealth) {
    requireNonNull(nodeHealth, "nodeHealth can't be null");

    Map<String, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Reading {} and adding {}", new HashMap<>(sqHealthState), nodeHealth);
    }
    sqHealthState.put(hazelcastClient.getUUID(), new TimestampedNodeHealth(nodeHealth, hazelcastClient.getClusterTime()));
  }

  @Override
  public void clearMine() {
    Map<String, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    String clientUUID = hazelcastClient.getUUID();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Reading {} and clearing for {}", new HashMap<>(sqHealthState), clientUUID);
    }
    sqHealthState.remove(clientUUID);
  }

  @Override
  public Set<NodeHealth> readAll() {
    Map<String, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    Set<String> hzMemberUUIDs = hazelcastClient.getMemberUuids();
    Set<NodeHealth> existingNodeHealths = sqHealthState.entrySet().stream()
      .filter(entry -> hzMemberUUIDs.contains(entry.getKey()))
      .map(entry -> entry.getValue().getNodeHealth())
      .collect(Collectors.toSet());
    if (LOG.isTraceEnabled()) {
      LOG.trace("Reading {} and keeping {}", new HashMap<>(sqHealthState), existingNodeHealths);
    }
    return ImmutableSet.copyOf(existingNodeHealths);
  }

  private Map<String, TimestampedNodeHealth> readReplicatedMap() {
    return hazelcastClient.getReplicatedMap(SQ_HEALTH_STATE_REPLICATED_MAP_IDENTIFIER);
  }

}
