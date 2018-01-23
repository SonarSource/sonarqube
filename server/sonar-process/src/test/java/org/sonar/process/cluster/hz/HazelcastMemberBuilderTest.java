/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.process.cluster.NodeType;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_PORT;

public class HazelcastMemberBuilderTest {

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  // use loopback for support of offline builds
  private InetAddress loopback = InetAddress.getLoopbackAddress();
  private HazelcastMemberBuilder underTest = new HazelcastMemberBuilder();

  @Test
  public void build_member() {
    HazelcastMember member = underTest
      .setNodeType(NodeType.APPLICATION)
      .setProcessId(ProcessId.COMPUTE_ENGINE)
      .setNodeName("bar")
      .setPort(NetworkUtilsImpl.INSTANCE.getNextAvailablePort(loopback))
      .setNetworkInterface(loopback.getHostAddress())
      .build();

    assertThat(member.getUuid()).isNotEmpty();
    assertThat(member.getClusterTime()).isGreaterThan(0);
    assertThat(member.getCluster().getMembers()).hasSize(1);
    assertThat(member.getMemberUuids()).containsOnlyOnce(member.getUuid());

    assertThat(member.getAtomicReference("baz")).isNotNull();
    assertThat(member.getLock("baz")).isNotNull();
    assertThat(member.getReplicatedMap("baz")).isNotNull();
    assertThat(member.getList("baz")).isNotNull();
    assertThat(member.getMap("baz")).isNotNull();
    assertThat(member.getSet("baz")).isNotNull();

    member.close();
  }

  @Test
  public void default_port_is_added_when_missing() {
    underTest.setMembers(asList("foo", "bar:9100", "1.2.3.4"));

    assertThat(underTest.getMembers()).containsExactly(
      "foo:" + CLUSTER_NODE_PORT.getDefaultValue(),
      "bar:9100",
      "1.2.3.4:" + CLUSTER_NODE_PORT.getDefaultValue());
  }
}
