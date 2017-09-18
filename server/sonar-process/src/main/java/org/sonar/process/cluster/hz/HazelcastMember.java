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
package org.sonar.process.cluster.hz;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.MemberSelector;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.NodeType;

public interface HazelcastMember extends AutoCloseable {

  interface Attribute {
    /**
     * The key of the hostname attribute of a member
     */
    String HOSTNAME = "HOSTNAME";
    /**
     * The key of the ips list attribute of a member
     */
    String IP_ADDRESSES = "IP_ADDRESSES";
    /**
     * The key of the node name attribute of a member
     */
    String NODE_NAME = "NODE_NAME";
    /**
     * The role of the sonar-application inside the SonarQube cluster
     * {@link NodeType}
     */
    String NODE_TYPE = "NODE_TYPE";
    /**
     * Key of process as defined by {@link ProcessId#getKey()}
     */
    String PROCESS_KEY = "PROCESS_KEY";
  }

  <E> IAtomicReference<E> getAtomicReference(String name);

  /**
   * Gets the set shared by the cluster and identified by name
   */
  <E> Set<E> getSet(String name);

  /**
   * Gets the list shared by the cluster and identified by name
   */
  <E> List<E> getList(String name);

  /**
   * Gets the map shared by the cluster and identified by name
   */
  <K, V> Map<K, V> getMap(String name);

  /**
   * Gets the replicated map shared by the cluster and identified by name.
   * Result can be casted to {@link com.hazelcast.core.ReplicatedMap} if needed to
   * benefit from listeners.
   */
  <K, V> Map<K, V> getReplicatedMap(String name);

  String getUuid();

  /**
   * The UUIDs of all the members (both members and local clients of these members) currently connected to the
   * Hazelcast cluster.
   */
  Set<String> getMemberUuids();

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
   * @param callable the query that is executed on all target members. Be careful of classloader, don't use classes
   *                 that are not available in classpath of target members.
   * @param memberSelector the subset of members to target. See {@link com.hazelcast.cluster.memberselector.MemberSelectors}
   *                       for utilities.
   * @param timeoutMs the total timeout to get responses from all target members, in milliseconds. If timeout is reached, then
   *                the members that didn't answer on time are marked as timed-out in {@link DistributedAnswer}
   */
  <T> DistributedAnswer<T> call(DistributedCall<T> callable, MemberSelector memberSelector, long timeoutMs)
    throws InterruptedException;

  @Override
  void close();
}
