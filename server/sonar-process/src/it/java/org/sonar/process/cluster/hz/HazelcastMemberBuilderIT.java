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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Timeout(60)
class HazelcastMemberBuilderIT {

  // use loopback for support of offline builds
  private final InetAddress loopback = InetAddress.getLoopbackAddress();

  @Test
  void build_tcp_ip_member_hostaddress() {
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
  void strip_square_brackets_from_ipv6_address_when_building_a_member() {
    HazelcastMemberBuilder memberBuilder = new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP)
      .setMembers(loopback.getHostAddress())
      .setProcessId(ProcessId.COMPUTE_ENGINE)
      .setNodeName("bar")
      .setPort(NetworkUtilsImpl.INSTANCE.getNextLoopbackAvailablePort())
      .setNetworkInterface("[" + loopback.getHostAddress() + "]");

    assertThatCode(memberBuilder::build).doesNotThrowAnyException();
  }

  @Test
  void build_tcp_ip_member_hostname() {
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
  void build_kubernetes_member() {
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
  void fail_if_elasticsearch_process() {
    var builder = new HazelcastMemberBuilder(JoinConfigurationType.TCP_IP);
    assertThatThrownBy(() -> builder.setProcessId(ProcessId.ELASTICSEARCH))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Hazelcast must not be enabled on Elasticsearch node");
  }
}
