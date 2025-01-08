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
package org.sonar.server.platform;

import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.slf4j.LoggerFactory;

import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_WEB_STARTUP_LEADER;

public class DefaultNodeInformation implements NodeInformation {

  private final boolean clusterEnabled;
  private final boolean startupLeader;
  private final String nodeName;

  public DefaultNodeInformation(Configuration config) {
    this.clusterEnabled = config.getBoolean(CLUSTER_ENABLED.getKey()).orElse(false);
    if (this.clusterEnabled) {
      this.startupLeader = config.getBoolean(CLUSTER_WEB_STARTUP_LEADER.getKey()).orElse(false);
      this.nodeName = config.get(CLUSTER_NODE_NAME.getKey()).orElse(CLUSTER_NODE_NAME.getDefaultValue());
      LoggerFactory.getLogger(DefaultNodeInformation.class).info("Cluster enabled (startup {})", startupLeader ? "leader" : "follower");
    } else {
      this.startupLeader = true;
      this.nodeName = null;
    }
  }

  @Override
  public boolean isStandalone() {
    return !clusterEnabled;
  }

  @Override
  public boolean isStartupLeader() {
    return startupLeader;
  }

  @Override
  public Optional<String> getNodeName() {
    return Optional.ofNullable(nodeName);
  }
}
