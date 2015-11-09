/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonarqube.ws.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.sonarqube.ws.client.permission.PermissionsWsClient;

import static org.sonarqube.ws.client.WsRequest.MediaType.PROTOBUF;

/**
 * Entry point of the Java Client for SonarQube Web Services.
 * <p/>
 * Example:
 * <pre>
 *   WsClient client = new WsClient(Connector);
 * </pre>
 *
 * @since 5.2
 */
public class WsClient {

  @VisibleForTesting
  final WsConnector wsConnector;
  private final PermissionsWsClient permissionsWsClient;

  public WsClient(WsConnector wsConnector) {
    this.wsConnector = wsConnector;
    this.permissionsWsClient = new PermissionsWsClient(this);
  }

  public String execute(WsRequest wsRequest) {
    return wsConnector.execute(wsRequest);
  }

  public <T extends Message> T execute(WsRequest wsRequest, Parser<T> protobufParser) {
    return wsConnector.execute(wsRequest.setMediaType(PROTOBUF), protobufParser);
  }

  public PermissionsWsClient permissionsClient() {
    return this.permissionsWsClient;
  }
}
