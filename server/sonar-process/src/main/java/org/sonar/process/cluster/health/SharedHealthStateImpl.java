/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.process.cluster.health;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastObjects;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

public class SharedHealthStateImpl implements SharedHealthState {
  private static final Logger LOG = LoggerFactory.getLogger(SharedHealthStateImpl.class);
  private static final int TIMEOUT_30_SECONDS = 30 * 1000;

  private final HazelcastMember hzMember;

  @Autowired(required = false)
  public SharedHealthStateImpl(HazelcastMember hzMember) {
    this.hzMember = hzMember;
  }

  @Autowired(required = false)
  public SharedHealthStateImpl() {
    this(null);
  }

  @Override
  public void writeMine(NodeHealth nodeHealth) {
    requireNonNull(nodeHealth, "nodeHealth can't be null");

    Map<UUID, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    LOG.atTrace()
      .addArgument(() -> new HashMap<>(sqHealthState))
      .addArgument(nodeHealth)
      .log("Reading {} and adding {}");
    sqHealthState.put(hzMember.getUuid(), new TimestampedNodeHealth(nodeHealth, hzMember.getClusterTime()));
  }

  @Override
  public void clearMine() {
    Map<UUID, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    UUID clientUUID = hzMember.getUuid();
    LOG.atTrace()
      .addArgument(() -> new HashMap<>(sqHealthState))
      .addArgument(clientUUID)
      .log("Reading {} and clearing for {}");
    sqHealthState.remove(clientUUID);
  }

  @Override
  public Set<NodeHealth> readAll() {
    long clusterTime = hzMember.getClusterTime();
    long timeout = clusterTime - TIMEOUT_30_SECONDS;
    Map<UUID, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    Set<UUID> hzMemberUUIDs = hzMember.getMemberUuids();
    Set<NodeHealth> existingNodeHealths = sqHealthState.entrySet().stream()
      .filter(outOfDate(timeout))
      .filter(ofNonExistentMember(hzMemberUUIDs))
      .map(entry -> entry.getValue().getNodeHealth())
      .collect(Collectors.toSet());
    LOG.trace("Reading {} and keeping {}", new HashMap<>(sqHealthState), existingNodeHealths);
    return ImmutableSet.copyOf(existingNodeHealths);
  }

  private static Predicate<Map.Entry<UUID, TimestampedNodeHealth>> outOfDate(long timeout) {
    return entry -> {
      boolean res = entry.getValue().getTimestamp() > timeout;
      if (!res) {
        LOG.trace("Ignoring NodeHealth of member {} because it is too old", entry.getKey());
      }
      return res;
    };
  }

  private static Predicate<Map.Entry<UUID, TimestampedNodeHealth>> ofNonExistentMember(Set<UUID> hzMemberUUIDs) {
    return entry -> {
      boolean res = hzMemberUUIDs.contains(entry.getKey());
      if (!res) {
        LOG.trace("Ignoring NodeHealth of member {} because it is not part of the cluster at the moment", entry.getKey());
      }
      return res;
    };
  }

  private Map<UUID, TimestampedNodeHealth> readReplicatedMap() {
    return hzMember.getReplicatedMap(HazelcastObjects.SQ_HEALTH_STATE);
  }

}
