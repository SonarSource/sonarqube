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
package org.sonar.cluster;

import java.util.Properties;
import java.util.UUID;

import static java.lang.String.valueOf;

public final class ClusterProperties {
  public static final String CLUSTER_ENABLED = "sonar.cluster.enabled";
  public static final String CLUSTER_NODE_TYPE = "sonar.cluster.node.type";
  public static final String CLUSTER_SEARCH_HOSTS = "sonar.cluster.search.hosts";
  public static final String CLUSTER_HOSTS = "sonar.cluster.hosts";
  public static final String CLUSTER_NODE_PORT = "sonar.cluster.node.port";
  public static final String CLUSTER_NODE_HOST = "sonar.cluster.node.host";
  public static final String CLUSTER_NODE_NAME = "sonar.cluster.node.name";
  public static final String CLUSTER_NAME = "sonar.cluster.name";
  public static final String HAZELCAST_LOG_LEVEL = "sonar.log.level.app.hazelcast";
  public static final String CLUSTER_WEB_LEADER = "sonar.cluster.web.startupLeader";
  // Internal property used by sonar-application to share the local endpoint of Hazelcast
  public static final String CLUSTER_LOCALENDPOINT = "sonar.cluster.hazelcast.localEndPoint";
  // Internal property used by sonar-application to share the local UUID of the Hazelcast member
  public static final String CLUSTER_MEMBERUUID = "sonar.cluster.hazelcast.memberUUID";

  private ClusterProperties() {
    // prevents instantiation
  }

  public static void putClusterDefaults(Properties properties) {
    properties.put(CLUSTER_ENABLED, valueOf(false));
    properties.put(CLUSTER_NAME, "sonarqube");
    properties.put(CLUSTER_NODE_HOST, "");
    properties.put(CLUSTER_HOSTS, "");
    properties.put(CLUSTER_NODE_PORT, "9003");
    properties.put(CLUSTER_NODE_NAME, "sonarqube-" + UUID.randomUUID().toString());
    properties.put(HAZELCAST_LOG_LEVEL, "WARN");
  }
}
