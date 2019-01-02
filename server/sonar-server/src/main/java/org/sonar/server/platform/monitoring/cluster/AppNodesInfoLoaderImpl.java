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
package org.sonar.server.platform.monitoring.cluster;

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.DistributedAnswer;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberSelectors;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.NODE_NAME;

@ServerSide
public class AppNodesInfoLoaderImpl implements AppNodesInfoLoader {

  /**
   * Timeout to get information from all nodes
   */
  private static final long DISTRIBUTED_TIMEOUT_MS = 15_000L;

  private final HazelcastMember hzMember;

  public AppNodesInfoLoaderImpl(HazelcastMember hzMember) {
    this.hzMember = hzMember;
  }

  public AppNodesInfoLoaderImpl() {
    this(null);
  }

  public Collection<NodeInfo> load() throws InterruptedException {
    Map<String, NodeInfo> nodesByName = new HashMap<>();
    MemberSelector memberSelector = HazelcastMemberSelectors.selectorForProcessIds(ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
    DistributedAnswer<ProtobufSystemInfo.SystemInfo> distributedAnswer = hzMember.call(ProcessInfoProvider::provide, memberSelector, DISTRIBUTED_TIMEOUT_MS);
    for (Member member : distributedAnswer.getMembers()) {
      String nodeName = member.getStringAttribute(NODE_NAME.getKey());
      NodeInfo nodeInfo = nodesByName.computeIfAbsent(nodeName, name -> {
        NodeInfo info = new NodeInfo(name);
        info.setHost(member.getAddress().getHost());
        return info;
      });
      completeNodeInfo(distributedAnswer, member, nodeInfo);
    }
    return nodesByName.values();
  }

  private static void completeNodeInfo(DistributedAnswer<ProtobufSystemInfo.SystemInfo> distributedAnswer, Member member, NodeInfo nodeInfo) {
    Optional<ProtobufSystemInfo.SystemInfo> nodeAnswer = distributedAnswer.getAnswer(member);
    Optional<Exception> failure = distributedAnswer.getFailed(member);
    if (distributedAnswer.hasTimedOut(member)) {
      nodeInfo.setErrorMessage("Failed to retrieve information on time");
    } else if (failure.isPresent()) {
      nodeInfo.setErrorMessage("Failed to retrieve information: " + failure.get().getMessage());
    } else if (nodeAnswer.isPresent()) {
      nodeAnswer.get().getSectionsList().forEach(nodeInfo::addSection);
    }
  }
}
