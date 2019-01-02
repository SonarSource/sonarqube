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
package org.sonar.server.platform;

import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_WEB_STARTUP_LEADER;

public class WebServerImpl implements WebServer {

  private final boolean clusterEnabled;
  private final boolean startupLeader;

  public WebServerImpl(Configuration config) {
    this.clusterEnabled = config.getBoolean(CLUSTER_ENABLED.getKey()).orElse(false);
    if (this.clusterEnabled) {
      this.startupLeader = config.getBoolean(CLUSTER_WEB_STARTUP_LEADER.getKey()).orElse(false);
      Loggers.get(WebServerImpl.class).info("Cluster enabled (startup {})", startupLeader ? "leader" : "follower");
    } else {
      this.startupLeader = true;
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
}
