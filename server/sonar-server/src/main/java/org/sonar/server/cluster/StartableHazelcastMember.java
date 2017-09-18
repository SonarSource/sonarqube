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
package org.sonar.server.cluster;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.MemberSelector;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.cluster.NodeType;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberBuilder;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_KEY;
import static org.sonar.process.ProcessProperties.CLUSTER_HOSTS;
import static org.sonar.process.ProcessProperties.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.CLUSTER_NODE_TYPE;

/**
 * Implementation of {@link HazelcastMember} as used by Compute Engine and
 * Web Server processes. It is configured by {@link Configuration}
 * and its lifecycle is managed by picocontainer.
 */
public class StartableHazelcastMember implements HazelcastMember, Startable {

  private final Configuration config;
  private final NetworkUtils networkUtils;
  private HazelcastMember member = null;

  public StartableHazelcastMember(Configuration config, NetworkUtils networkUtils) {
    this.config = config;
    this.networkUtils = networkUtils;
  }

  @Override
  public <E> IAtomicReference<E> getAtomicReference(String name) {
    return nonNullMember().getAtomicReference(name);
  }

  @Override
  public <E> Set<E> getSet(String name) {
    return nonNullMember().getSet(name);
  }

  @Override
  public <E> List<E> getList(String name) {
    return nonNullMember().getList(name);
  }

  @Override
  public <K, V> Map<K, V> getMap(String name) {
    return nonNullMember().getMap(name);
  }

  @Override
  public <K, V> Map<K, V> getReplicatedMap(String name) {
    return nonNullMember().getReplicatedMap(name);
  }

  @Override
  public String getUuid() {
    return nonNullMember().getUuid();
  }

  @Override
  public Set<String> getMemberUuids() {
    return nonNullMember().getMemberUuids();
  }

  @Override
  public Lock getLock(String name) {
    return nonNullMember().getLock(name);
  }

  @Override
  public long getClusterTime() {
    return nonNullMember().getClusterTime();
  }

  @Override
  public Cluster getCluster() {
    return nonNullMember().getCluster();
  }

  @Override
  public <T> DistributedAnswer<T> call(DistributedCall<T> callable, MemberSelector memberSelector, long timeoutMs)
    throws InterruptedException {
    return nonNullMember().call(callable, memberSelector, timeoutMs);
  }

  private HazelcastMember nonNullMember() {
    return requireNonNull(member, "Hazelcast member not started");
  }

  @Override
  public void close() {
    if (member != null) {
      member.close();
      member = null;
    }
  }

  @Override
  public void start() {
    String networkAddress = config.get(CLUSTER_NODE_HOST).orElseThrow(() -> new IllegalStateException("Missing node host"));
    int freePort;
    try {
      freePort = networkUtils.getNextAvailablePort(InetAddress.getByName(networkAddress));
    } catch (UnknownHostException e) {
      throw new IllegalStateException(format("Can not resolve address %s", networkAddress), e);
    }
    this.member = new HazelcastMemberBuilder()
      .setClusterName(config.get(ProcessProperties.CLUSTER_NAME).orElseThrow(() -> new IllegalStateException("Missing cluster name")))
      .setNodeName(config.get(ProcessProperties.CLUSTER_NODE_NAME).orElseThrow(() -> new IllegalStateException("Missing node name")))
      .setNodeType(NodeType.parse(config.get(CLUSTER_NODE_TYPE).orElseThrow(() -> new IllegalStateException("Missing node type"))))
      .setPort(freePort)
      .setProcessId(ProcessId.fromKey(config.get(PROPERTY_PROCESS_KEY).orElseThrow(() -> new IllegalStateException("Missing process key"))))
      .setMembers(asList(config.getStringArray(CLUSTER_HOSTS)))
      .setNetworkInterface(networkAddress)
      .build();
  }

  @Override
  public void stop() {
    close();
  }
}
