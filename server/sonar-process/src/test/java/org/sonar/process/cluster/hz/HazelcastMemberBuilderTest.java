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
package org.sonar.process.cluster.hz;

import java.net.InetAddress;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HazelcastMemberBuilderTest {

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  // use loopback for support of offline builds
  private final InetAddress loopback = InetAddress.getLoopbackAddress();

  @Test
  public void build_tcp_ip_member_hostaddress() {
    HazelcastMember member = new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP)
      .setMembers(loopback.getHostAddress())
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
  public void build_tcp_ip_member_hostname() {
    HazelcastMember member = new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP)
        .setMembers(loopback.getHostName())
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
  public void build_kubernetes_member() {
    HazelcastMember member = new HazelcastMemberBuilder(JoinConfigurationType.KUBERNETES)
      .setMembers(loopback.getHostAddress())
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
  public void fail_if_elasticsearch_process() {
    var builder = new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP);
    assertThatThrownBy(() -> builder.setProcessId(ProcessId.ELASTICSEARCH))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Hazelcast must not be enabled on Elasticsearch node");
  }
}
