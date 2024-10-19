/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.process.cluster.hz;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.MemberSelector;
import com.hazelcast.cp.IAtomicReference;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import org.sonar.process.ProcessId;

public interface HazelcastMember extends AutoCloseable {

  enum Attribute {
    /**
     * The key of the node name attribute of a member
     */
    NODE_NAME("NODE_NAME"),
    /**
     * Key of process as defined by {@link ProcessId#getKey()}
     */
    PROCESS_KEY("PROCESS_KEY");

    private final String key;

    Attribute(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }

  <E> IAtomicReference<E> getAtomicReference(String name);

  /**
   * Gets the replicated map shared by the cluster and identified by name.
   * Result can be casted to {@link com.hazelcast.replicatedmap.ReplicatedMap} if needed to
   * benefit from listeners.
   */
  <K, V> Map<K, V> getReplicatedMap(String name);

  UUID getUuid();

  /**
   * The UUIDs of all the members (both members and local clients of these members) currently connected to the
   * Hazelcast cluster.
   */
  Set<UUID> getMemberUuids();

  /**
   * Gets lock among the cluster, identified by name
   */
  Lock getLock(String name);

  /**
   * Retrieves the cluster time which is (almost) identical on all members of the cluster.
   */
  long getClusterTime();

  Cluster getCluster();

  /**
   * Runs a distributed query on a set of Hazelcast members.
   *
   * @param callable       the query that is executed on all target members. Be careful of classloader, don't use classes
   *                       that are not available in classpath of target members.
   * @param memberSelector the subset of members to target. See {@link com.hazelcast.cluster.memberselector.MemberSelectors}
   *                       for utilities.
   * @param timeoutMs      the total timeout to get responses from all target members, in milliseconds. If timeout is reached, then
   *                       the members that didn't answer on time are marked as timed-out in {@link DistributedAnswer}
   * @throws java.util.concurrent.RejectedExecutionException if no member is selected
   */
  <T> DistributedAnswer<T> call(DistributedCall<T> callable, MemberSelector memberSelector, long timeoutMs)
    throws InterruptedException;

  /**
   * Runs asynchronously a distributed query on a set of Hazelcast members.
   *
   * @param callable       the query that is executed on all target members. Be careful of classloader, don't use classes
   *                       that are not available in classpath of target members.
   * @param memberSelector the subset of members to target. See {@link com.hazelcast.cluster.memberselector.MemberSelectors}
   *                       for utilities.
   * @param callback       will be called once we get all responses.
   * @throws java.util.concurrent.RejectedExecutionException if no member is selected
   */
  <T> void callAsync(DistributedCall<T> callable, MemberSelector memberSelector, DistributedCallback<T> callback);

  @Override
  void close();
}
