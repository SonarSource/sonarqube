/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.hazelcast.core.Cluster;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.nio.Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.DistributedCallback;
import org.sonar.process.cluster.hz.HazelcastMember;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;

public class AppNodesClusterHostsConsistencyTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestAppSettings settings = new TestAppSettings();
  private Consumer<String> logger = mock(Consumer.class);

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

    settings.set(CLUSTER_HZ_HOSTS.getKey(), "1.1.1.1:1000,1.1.1.1:2000,1.1.1.2:1000");

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

    settings.set(CLUSTER_HZ_HOSTS.getKey(), "1.1.1.1:1000,1.1.1.1:2000,1.1.1.2:1000");

    TestHazelcastMember member = new TestHazelcastMember(hostsPerMember, m1);
    AppNodesClusterHostsConsistency underTest = AppNodesClusterHostsConsistency.setInstance(member, settings, logger);
    underTest.check();

    verifyZeroInteractions(logger);
  }

  @Test
  public void setInstance_fails_with_ISE_when_called_twice_with_same_arguments() throws UnknownHostException {
    TestHazelcastMember member = new TestHazelcastMember(Collections.emptyMap(), newLocalHostMember(1, true));

    AppNodesClusterHostsConsistency.setInstance(member, settings);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Instance is already set");

    AppNodesClusterHostsConsistency.setInstance(member, settings);
  }

  @Test
  public void setInstance_fails_with_ISE_when_called_twice_with_other_arguments() throws UnknownHostException {
    TestHazelcastMember member1 = new TestHazelcastMember(Collections.emptyMap(), newLocalHostMember(1, true));
    TestHazelcastMember member2 = new TestHazelcastMember(Collections.emptyMap(), newLocalHostMember(2, true));
    AppNodesClusterHostsConsistency.setInstance(member1, new TestAppSettings());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Instance is already set");

    AppNodesClusterHostsConsistency.setInstance(member2, new TestAppSettings());
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

  private class TestHazelcastMember implements HazelcastMember {
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
    public String getUuid() {
      throw new IllegalStateException("not expected to be called");
    }

    @Override
    public Set<String> getMemberUuids() {
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
