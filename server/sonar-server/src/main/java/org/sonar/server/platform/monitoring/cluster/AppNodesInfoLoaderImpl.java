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
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

@ServerSide
public class AppNodesInfoLoaderImpl implements AppNodesInfoLoader {

  private final HazelcastMember hzMember;

  public AppNodesInfoLoaderImpl(HazelcastMember hzMember) {
    this.hzMember = hzMember;
  }

  public Collection<NodeInfo> load() {
    try {
      Map<String, NodeInfo> nodesByName = new HashMap<>();
      DistributedAnswer<ProtobufSystemInfo.SystemInfo> distributedAnswer = hzMember.call(ProcessInfoProvider::provide, new CeWebMemberSelector(), 15_000L);
      for (Member member : distributedAnswer.getMembers()) {
        String nodeName = member.getStringAttribute(HazelcastMember.Attribute.NODE_NAME);
        NodeInfo nodeInfo = nodesByName.get(nodeName);
        if (nodeInfo == null) {
          nodeInfo = new NodeInfo(nodeName);
          nodesByName.put(nodeName, nodeInfo);
        }
        completeNodeInfo(distributedAnswer, member, nodeInfo);
      }
      return nodesByName.values();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
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

  private static class CeWebMemberSelector implements MemberSelector {
    @Override
    public boolean select(Member member) {
      String processKey = member.getStringAttribute(HazelcastMember.Attribute.PROCESS_KEY);
      return processKey.equals(ProcessId.WEB_SERVER.getKey()) || processKey.equals(ProcessId.COMPUTE_ENGINE.getKey());
    }
  }
}
