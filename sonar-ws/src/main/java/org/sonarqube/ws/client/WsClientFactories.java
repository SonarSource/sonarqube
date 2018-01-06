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
package org.sonarqube.ws.client;

import org.sonar.api.server.ws.LocalConnector;

/**
 * All provided implementations of {@link WsClientFactory}.
 */
public class WsClientFactories {

  private WsClientFactories() {
    // prevent instantiation
  }

  /**
   * Factory to be used when connecting to a remote SonarQube web server.
   */
  public static WsClientFactory getDefault() {
    return DefaultWsClientFactory.INSTANCE;
  }

  /**
   * Factory that allows a SonarQube web service to interact
   * with other web services, without using the HTTP stack.
   * @see org.sonar.api.server.ws.LocalConnector
   */
  public static LocalWsClientFactory getLocal() {
    return DefaultLocalWsClientFactory.INSTANCE;
  }

  private enum DefaultWsClientFactory implements WsClientFactory {
    INSTANCE;

    @Override
    public WsClient newClient(WsConnector connector) {
      return new DefaultWsClient(connector);
    }
  }

  private enum DefaultLocalWsClientFactory implements LocalWsClientFactory {
    INSTANCE;

    @Override
    public WsClient newClient(WsConnector connector) {
      return DefaultWsClientFactory.INSTANCE.newClient(connector);
    }

    @Override
    public WsClient newClient(LocalConnector localConnector) {
      return new DefaultWsClient(new LocalWsConnector(localConnector));
    }
  }
}
