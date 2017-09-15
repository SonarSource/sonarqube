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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.Member;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

class HazelcastMemberImpl implements HazelcastMember {

  private final HazelcastInstance hzInstance;

  HazelcastMemberImpl(HazelcastInstance hzInstance) {
    this.hzInstance = hzInstance;
  }

  @Override
  public <E> IAtomicReference<E> getAtomicReference(String name) {
    return hzInstance.getAtomicReference(name);
  }

  @Override
  public <E> Set<E> getSet(String s) {
    return hzInstance.getSet(s);
  }

  @Override
  public <E> List<E> getList(String s) {
    return hzInstance.getList(s);
  }

  @Override
  public <K, V> Map<K, V> getMap(String s) {
    return hzInstance.getMap(s);
  }

  @Override
  public <K, V> Map<K, V> getReplicatedMap(String s) {
    return hzInstance.getReplicatedMap(s);
  }

  @Override
  public String getUuid() {
    return hzInstance.getLocalEndpoint().getUuid();
  }

  @Override
  public Set<String> getMemberUuids() {
    return hzInstance.getCluster().getMembers().stream().map(Member::getUuid).collect(Collectors.toSet());
  }

  @Override
  public Lock getLock(String s) {
    return hzInstance.getLock(s);
  }

  @Override
  public long getClusterTime() {
    return hzInstance.getCluster().getClusterTime();
  }

  @Override
  public Cluster getCluster() {
    return hzInstance.getCluster();
  }

  @Override
  public void close() {
    try {
      hzInstance.shutdown();
    } catch (HazelcastInstanceNotActiveException e) {
      LoggerFactory.getLogger(getClass()).debug("Unable to shutdown Hazelcast member", e);
    }
  }
}
