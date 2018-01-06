/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastObjects;

import static java.util.Objects.requireNonNull;

public class SharedHealthStateImpl implements SharedHealthState {
  private static final Logger LOG = LoggerFactory.getLogger(SharedHealthStateImpl.class);
  private static final int TIMEOUT_30_SECONDS = 30 * 1000;

  private final HazelcastMember hzMember;

  public SharedHealthStateImpl(HazelcastMember hzMember) {
    this.hzMember = hzMember;
  }

  public SharedHealthStateImpl() {
    this(null);
  }

  @Override
  public void writeMine(NodeHealth nodeHealth) {
    requireNonNull(nodeHealth, "nodeHealth can't be null");

    Map<String, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Reading {} and adding {}", new HashMap<>(sqHealthState), nodeHealth);
    }
    sqHealthState.put(hzMember.getUuid(), new TimestampedNodeHealth(nodeHealth, hzMember.getClusterTime()));
  }

  @Override
  public void clearMine() {
    Map<String, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    String clientUUID = hzMember.getUuid();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Reading {} and clearing for {}", new HashMap<>(sqHealthState), clientUUID);
    }
    sqHealthState.remove(clientUUID);
  }

  @Override
  public Set<NodeHealth> readAll() {
    long clusterTime = hzMember.getClusterTime();
    long timeout = clusterTime - TIMEOUT_30_SECONDS;
    Map<String, TimestampedNodeHealth> sqHealthState = readReplicatedMap();
    Set<String> hzMemberUUIDs = hzMember.getMemberUuids();
    Set<NodeHealth> existingNodeHealths = sqHealthState.entrySet().stream()
      .filter(outOfDate(timeout))
      .filter(ofNonExistentMember(hzMemberUUIDs))
      .map(entry -> entry.getValue().getNodeHealth())
      .collect(Collectors.toSet());
    if (LOG.isTraceEnabled()) {
      LOG.trace("Reading {} and keeping {}", new HashMap<>(sqHealthState), existingNodeHealths);
    }
    return ImmutableSet.copyOf(existingNodeHealths);
  }

  private static Predicate<Map.Entry<String, TimestampedNodeHealth>> outOfDate(long timeout) {
    return entry -> {
      boolean res = entry.getValue().getTimestamp() > timeout;
      if (!res) {
        LOG.trace("Ignoring NodeHealth of member {} because it is too old", entry.getKey());
      }
      return res;
    };
  }

  private static Predicate<Map.Entry<String, TimestampedNodeHealth>> ofNonExistentMember(Set<String> hzMemberUUIDs) {
    return entry -> {
      boolean res = hzMemberUUIDs.contains(entry.getKey());
      if (!res) {
        LOG.trace("Ignoring NodeHealth of member {} because it is not part of the cluster at the moment", entry.getKey());
      }
      return res;
    };
  }

  private Map<String, TimestampedNodeHealth> readReplicatedMap() {
    return hzMember.getReplicatedMap(HazelcastObjects.SQ_HEALTH_STATE);
  }

}
