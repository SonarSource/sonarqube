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
package org.sonar.application;

import com.google.common.net.HostAndPort;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.application.cluster.AppNodesClusterHostsConsistency;
import org.sonar.application.cluster.ClusterAppStateImpl;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.ClusterSettings;
import org.sonar.application.es.EsConnector;
import org.sonar.application.es.EsConnectorImpl;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberBuilder;

import static java.util.Arrays.asList;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HOST;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HZ_PORT;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;

public class AppStateFactory {
  private final AppSettings settings;

  public AppStateFactory(AppSettings settings) {
    this.settings = settings;
  }

  public AppState create() {
    if (ClusterSettings.shouldStartHazelcast(settings)) {
      EsConnector esConnector = createEsConnector(settings.getProps());
      HazelcastMember hzMember = createHzMember(settings.getProps());
      AppNodesClusterHostsConsistency appNodesClusterHostsConsistency = AppNodesClusterHostsConsistency.setInstance(hzMember, settings);
      return new ClusterAppStateImpl(settings, hzMember, esConnector, appNodesClusterHostsConsistency);
    }
    return new AppStateImpl();
  }

  private static HazelcastMember createHzMember(Props props) {
    HazelcastMemberBuilder builder = new HazelcastMemberBuilder()
      .setNetworkInterface(props.nonNullValue(CLUSTER_NODE_HOST.getKey()))
      .setMembers(asList(props.nonNullValue(CLUSTER_HZ_HOSTS.getKey()).split(",")))
      .setNodeName(props.nonNullValue(CLUSTER_NODE_NAME.getKey()))
      .setPort(Integer.parseInt(props.nonNullValue(CLUSTER_NODE_HZ_PORT.getKey())))
      .setProcessId(ProcessId.APP);
    return builder.build();
  }

  private static EsConnector createEsConnector(Props props) {
    String searchHosts = props.nonNullValue(CLUSTER_SEARCH_HOSTS.getKey());
    Set<HostAndPort> hostAndPorts = Arrays.stream(searchHosts.split(","))
      .map(HostAndPort::fromString)
      .collect(Collectors.toSet());
    return new EsConnectorImpl(props.nonNullValue(CLUSTER_NAME.getKey()), hostAndPorts);
  }
}
