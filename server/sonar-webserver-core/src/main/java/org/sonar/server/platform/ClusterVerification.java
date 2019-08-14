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

import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.MessageException;

@ServerSide
public class ClusterVerification implements Startable {

  private final WebServer server;
  @Nullable
  private final ClusterFeature feature;

  public ClusterVerification(WebServer server, @Nullable ClusterFeature feature) {
    this.server = server;
    this.feature = feature;
  }

  public ClusterVerification(WebServer server) {
    this(server, null);
  }

  @Override
  public void start() {
    if (server.isStandalone()) {
      return;
    }
    if (feature == null || !feature.isEnabled()) {
      throw MessageException.of(
        "Cluster mode can't be enabled. Please install the Data Center Edition. More details at https://redirect.sonarsource.com/editions/datacenter.html.");
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
