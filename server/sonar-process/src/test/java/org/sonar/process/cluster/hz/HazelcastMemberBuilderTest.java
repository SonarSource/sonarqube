/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HZ_PORT;

public class HazelcastMemberBuilderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  // use loopback for support of offline builds
  private final InetAddress loopback = InetAddress.getLoopbackAddress();
  private final InetAdressResolver inetAdressResolver = mock(InetAdressResolver.class);
  private final HazelcastMemberBuilder underTest = new HazelcastMemberBuilder(inetAdressResolver);

  @Before
  public void before() throws UnknownHostException {
    when(inetAdressResolver.getAllByName("foo")).thenReturn(Collections.singletonList("foo/5.6.7.8"));
    when(inetAdressResolver.getAllByName("bar")).thenReturn(Collections.singletonList("bar/8.7.6.5"));
    when(inetAdressResolver.getAllByName("wizz")).thenReturn(Arrays.asList("wizz/1.2.3.4", "wizz/2.3.4.5", "wizz/3.4.5.6"));
    when(inetAdressResolver.getAllByName("ninja")).thenReturn(Arrays.asList("ninja/4.5.6.7", "ninja/5.6.7.8"));
  }

  @Test
  public void testMultipleIPsByHostname() {
    underTest.setMembers(asList("wizz:9001", "ninja"));

    List<String> members = underTest.getMembers();
    assertThat(members).containsExactlyInAnyOrder("1.2.3.4:9001", "2.3.4.5:9001", "3.4.5.6:9001", "4.5.6.7:9003", "5.6.7.8:9003");

  }

  @Test
  public void build_member() {
    HazelcastMember member = underTest
      .setProcessId(ProcessId.COMPUTE_ENGINE)
      .setNodeName("bar")
      .setPort(NetworkUtilsImpl.INSTANCE.getNextLoopbackAvailablePort())
      .setNetworkInterface(loopback.getHostAddress())
      .build();

    assertThat(member.getUuid()).isNotNull();
    assertThat(member.getClusterTime()).isPositive();
    assertThat(member.getCluster().getMembers()).hasSize(1);
    assertThat(member.getMemberUuids()).containsOnlyOnce(member.getUuid());

    assertThat(member.getAtomicReference("baz")).isNotNull();
    assertThat(member.getLock("baz")).isNotNull();
    assertThat(member.getReplicatedMap("baz")).isNotNull();

    member.close();
  }

  @Test
  public void default_port_is_added_when_missing() {
    underTest.setMembers(asList("foo", "bar:9100", "1.2.3.4"));

    assertThat(underTest.getMembers()).containsExactly(
      "5.6.7.8:" + CLUSTER_NODE_HZ_PORT.getDefaultValue(),
      "8.7.6.5:9100",
      "1.2.3.4:" + CLUSTER_NODE_HZ_PORT.getDefaultValue());
  }

  @Test
  public void fail_if_elasticsearch_process() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Hazelcast must not be enabled on Elasticsearch node");

    underTest.setProcessId(ProcessId.ELASTICSEARCH);
  }
}
