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
package org.sonar.application;

import org.sonar.application.cluster.ClusterAppStateImpl;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.ClusterSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.cluster.NodeType;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberBuilder;

import static java.util.Arrays.asList;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;

public class AppStateFactory {

  private final AppSettings settings;

  public AppStateFactory(AppSettings settings) {
    this.settings = settings;
  }

  public AppState create() {
    if (ClusterSettings.isClusterEnabled(settings)) {
      HazelcastMember hzMember = createHzMember(settings.getProps());
      return new ClusterAppStateImpl(settings, hzMember);
    }
    return new AppStateImpl();
  }

  private static HazelcastMember createHzMember(Props props) {
    HazelcastMemberBuilder builder = new HazelcastMemberBuilder()
      .setNetworkInterface(props.nonNullValue(CLUSTER_NODE_HOST.getKey()))
      .setMembers(asList(props.nonNullValue(CLUSTER_HOSTS.getKey()).split(",")))
      .setNodeType(NodeType.parse(props.nonNullValue(CLUSTER_NODE_TYPE.getKey())))
      .setNodeName(props.nonNullValue(CLUSTER_NODE_NAME.getKey()))
      .setPort(Integer.parseInt(props.nonNullValue(CLUSTER_NODE_PORT.getKey())))
      .setProcessId(ProcessId.APP);
    return builder.build();
  }
}
