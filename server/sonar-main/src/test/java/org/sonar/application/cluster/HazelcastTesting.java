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
package org.sonar.application.cluster;

import java.net.InetAddress;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.NodeType;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberBuilder;

public class HazelcastTesting {

  private HazelcastTesting() {
    // do not instantiate
  }

  public static HazelcastMember newHzMember() {
    // use loopback for support of offline builds
    InetAddress loopback = InetAddress.getLoopbackAddress();

    return new HazelcastMemberBuilder()
      .setNodeType(NodeType.APPLICATION)
      .setProcessId(ProcessId.COMPUTE_ENGINE)
      .setClusterName("foo")
      .setNodeName("bar")
      .setPort(NetworkUtils.INSTANCE.getNextAvailablePort(loopback))
      .setNetworkInterface(loopback.getHostAddress())
      .build();
  }
}
