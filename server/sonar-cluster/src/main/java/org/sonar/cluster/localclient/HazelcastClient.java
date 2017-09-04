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

package org.sonar.cluster.localclient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * The interface Hazelcast client wrapper.
 */
public interface HazelcastClient {
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
   * Gets the replicated map shared by the cluster and identified by name
   */
  <K,V> Map<K,V> getReplicatedMap(String name);

  /**
   * The UUID of the Hazelcast client.
   *
   * <p>The uuid of the member of the current client is a member, otherwise the UUID of the client if the
   * member is a local client of one of the members.</p>
   */
  String getUUID();

  /**
   * The UUIDs of all the members (both members and local clients of these members) currently connected to the
   * Hazelcast cluster.
   */
  Set<String> getMemberUuids();

  /**
   * Gets lock among the cluster, identified by name
   */
  Lock getLock(String name);
}
