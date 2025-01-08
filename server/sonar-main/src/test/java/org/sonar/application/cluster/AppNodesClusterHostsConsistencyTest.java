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
package org.sonar.application.cluster;

import com.google.common.collect.ImmutableMap;
import com.hazelcast.cluster.Address;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MemberSelector;
import com.hazelcast.cp.IAtomicReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.DistributedCallback;
import org.sonar.process.cluster.hz.HazelcastMember;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;

public class AppNodesClusterHostsConsistencyTest {

  @SuppressWarnings("unchecked")
  private final Consumer<String> logger = mock(Consumer.class);

  @Before
  @After
  public void setUp() {
    AppNodesClusterHostsConsistency.clearInstance();
  }

  @Test
  public void log_warning_if_configured_hosts_are_not_consistent() throws UnknownHostException {
    Map<Member, List<String>> hostsPerMember = new LinkedHashMap<>();
    Member m1 = newLocalHostMember(1, true);
    Member m2 = newLocalHostMember(2);
    Member m3 = newLocalHostMember(3);

    hostsPerMember.put(m2, Arrays.asList("1.1.1.1:1000", "1.1.1.1:2000"));
    hostsPerMember.put(m3, Arrays.asList("1.1.1.1:1000", "1.1.1.2:1000"));

    TestAppSettings settings = new TestAppSettings(ImmutableMap.of(CLUSTER_HZ_HOSTS.getKey(), "1.1.1.1:1000,1.1.1.1:2000,1.1.1.2:1000"));

    TestHazelcastMember member = new TestHazelcastMember(hostsPerMember, m1);
    AppNodesClusterHostsConsistency underTest = AppNodesClusterHostsConsistency.setInstance(member, settings, logger);
    underTest.check();

    verify(logger).accept("The configuration of the current node doesn't match the list of hosts configured in the application nodes that have already joined the cluster:\n" +
      m1.getAddress().getHost() + ":" + m1.getAddress().getPort() + " : [1.1.1.1:1000, 1.1.1.1:2000, 1.1.1.2:1000] (current)\n" +
      m2.getAddress().getHost() + ":" + m2.getAddress().getPort() + " : [1.1.1.1:1000, 1.1.1.1:2000]\n" +
      m3.getAddress().getHost() + ":" + m3.getAddress().getPort() + " : [1.1.1.1:1000, 1.1.1.2:1000]\n" +
      "Make sure the configuration is consistent among all application nodes before you restart any node");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void dont_log_if_configured_hosts_are_consistent() throws UnknownHostException {
    Map<Member, List<String>> hostsPerMember = new LinkedHashMap<>();
    Member m1 = newLocalHostMember(1, true);
    Member m2 = newLocalHostMember(2);
    Member m3 = newLocalHostMember(3);

    hostsPerMember.put(m2, Arrays.asList("1.1.1.1:1000", "1.1.1.1:2000", "1.1.1.2:1000"));
    hostsPerMember.put(m3, Arrays.asList("1.1.1.1:1000", "1.1.1.1:2000", "1.1.1.2:1000"));

    TestAppSettings settings = new TestAppSettings(ImmutableMap.of(CLUSTER_HZ_HOSTS.getKey(), "1.1.1.1:1000,1.1.1.1:2000,1.1.1.2:1000"));

    TestHazelcastMember member = new TestHazelcastMember(hostsPerMember, m1);
    AppNodesClusterHostsConsistency underTest = AppNodesClusterHostsConsistency.setInstance(member, settings, logger);
    underTest.check();

    verifyNoMoreInteractions(logger);
  }

  @Test
  public void setInstance_fails_with_ISE_when_called_twice_with_same_arguments() throws UnknownHostException {
    TestHazelcastMember member = new TestHazelcastMember(Collections.emptyMap(), newLocalHostMember(1, true));
    TestAppSettings settings = new TestAppSettings();
    AppNodesClusterHostsConsistency.setInstance(member, settings);

    assertThatThrownBy(() -> AppNodesClusterHostsConsistency.setInstance(member, settings))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Instance is already set");
  }

  @Test
  public void setInstance_fails_with_ISE_when_called_twice_with_other_arguments() throws UnknownHostException {
    TestHazelcastMember member1 = new TestHazelcastMember(Collections.emptyMap(), newLocalHostMember(1, true));
    TestHazelcastMember member2 = new TestHazelcastMember(Collections.emptyMap(), newLocalHostMember(2, true));
    AppNodesClusterHostsConsistency.setInstance(member1, new TestAppSettings());

    assertThatThrownBy(() -> AppNodesClusterHostsConsistency.setInstance(member2, new TestAppSettings()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Instance is already set");
  }

  private Member newLocalHostMember(int port) throws UnknownHostException {
    return newLocalHostMember(port, false);
  }

  private Member newLocalHostMember(int port, boolean localMember) throws UnknownHostException {
    Member member = mock(Member.class);
    when(member.localMember()).thenReturn(localMember);
    Address address1 = new Address(InetAddress.getLocalHost(), port);
    when(member.getAddress()).thenReturn(address1);
    return member;
  }

  private static class TestHazelcastMember implements HazelcastMember {
    private final Map<Member, List<String>> hostsPerMember;
    private final Cluster cluster = mock(Cluster.class);

    private TestHazelcastMember(Map<Member, List<String>> hostsPerMember, Member localMember) {
      this.hostsPerMember = hostsPerMember;
      when(cluster.getLocalMember()).thenReturn(localMember);
    }

    @Override
    public <E> IAtomicReference<E> getAtomicReference(String name) {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public <K, V> Map<K, V> getReplicatedMap(String name) {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public UUID getUuid() {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public Set<UUID> getMemberUuids() {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public Lock getLock(String name) {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public long getClusterTime() {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public Cluster getCluster() {
      return cluster;
    }

    @Override
    public <T> DistributedAnswer<T> call(DistributedCall<T> callable, MemberSelector memberSelector, long timeoutMs) {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public <T> void callAsync(DistributedCall<T> callable, MemberSelector memberSelector, DistributedCallback<T> callback) {
      callback.onComplete((Map<Member, T>) hostsPerMember);
    }

    @Override
    public void close() {

    }
  }
}
