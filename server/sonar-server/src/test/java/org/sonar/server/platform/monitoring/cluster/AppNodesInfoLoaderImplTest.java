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
package org.sonar.server.platform.monitoring.cluster;

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.nio.Address;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.DistributedCall;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.SystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AppNodesInfoLoaderImplTest {

  private static final InetAddress AN_ADDRESS = InetAddress.getLoopbackAddress();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private HazelcastMember hzMember = mock(HazelcastMember.class);
  private AppNodesInfoLoaderImpl underTest = new AppNodesInfoLoaderImpl(hzMember);

  @Test
  public void load_info_from_all_nodes() throws Exception {
    DistributedAnswer<SystemInfo> answer = new DistributedAnswer<>();
    answer.setAnswer(newMember("foo"), SystemInfo.newBuilder().addSections(Section.newBuilder().build()).build());
    answer.setTimedOut(newMember("bar"));
    answer.setFailed(newMember("baz"), new IOException("BOOM"));
    when(hzMember.call(any(DistributedCall.class), any(MemberSelector.class), anyLong())).thenReturn(answer);

    Collection<NodeInfo> nodes = underTest.load();

    assertThat(nodes).hasSize(3);

    NodeInfo successfulNodeInfo = findNode(nodes, "foo");
    assertThat(successfulNodeInfo.getName()).isEqualTo("foo");
    assertThat(successfulNodeInfo.getHost()).hasValue(AN_ADDRESS.getHostAddress());
    assertThat(successfulNodeInfo.getErrorMessage()).isEmpty();
    assertThat(successfulNodeInfo.getSections()).hasSize(1);

    NodeInfo timedOutNodeInfo = findNode(nodes, "bar");
    assertThat(timedOutNodeInfo.getName()).isEqualTo("bar");
    assertThat(timedOutNodeInfo.getErrorMessage()).hasValue("Failed to retrieve information on time");
    assertThat(timedOutNodeInfo.getSections()).isEmpty();

    NodeInfo failedNodeInfo = findNode(nodes, "baz");
    assertThat(failedNodeInfo.getName()).isEqualTo("baz");
    assertThat(failedNodeInfo.getErrorMessage()).hasValue("Failed to retrieve information: BOOM");
    assertThat(failedNodeInfo.getSections()).isEmpty();
  }

  private NodeInfo findNode(Collection<NodeInfo> nodes, String name) {
    return nodes.stream()
      .filter(n -> n.getName().equals(name))
      .findFirst()
      .orElseThrow(IllegalStateException::new);
  }

  private Member newMember(String name) {
    Member member = mock(Member.class, Mockito.RETURNS_MOCKS);
    when(member.getStringAttribute(HazelcastMember.Attribute.NODE_NAME.getKey())).thenReturn(name);
    when(member.getAddress()).thenReturn(new Address(AN_ADDRESS, 6789));
    return member;
  }
}
