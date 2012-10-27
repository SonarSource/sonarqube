/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import org.sonar.api.BatchComponent;
import org.sonar.api.platform.Server;
import org.sonar.batch.bootstrap.ServerClient;

import java.util.Date;

/**
 * @deprecated replaced by ServerClient since version 3.4. Plugins should use org.sonar.api.platform.Server
 */
@Deprecated
public class ServerMetadata extends Server implements BatchComponent {
  private ServerClient server;

  public ServerMetadata(ServerClient server) {
    this.server = server;
  }

  @Override
  public String getId() {
    return server.getId();
  }

  @Override
  public String getVersion() {
    return server.getVersion();
  }

  @Override
  public Date getStartedAt() {
    return server.getStartedAt();
  }

  /**
   * @return the server URL when executed from batch, else null.
   * @since 2.4
   */
  @Override
  public String getURL() {
    return server.getURL();
  }

  /**
   * @since 2.10
   */
  @Override
  public String getPermanentServerId() {
    return server.getPermanentServerId();
  }
}
