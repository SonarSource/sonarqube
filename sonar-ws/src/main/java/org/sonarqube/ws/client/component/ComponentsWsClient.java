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

package org.sonarqube.ws.client.component;

import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.client.WsClient;

import static org.sonarqube.ws.client.WsRequest.newGetRequest;

public class ComponentsWsClient {
  private static final String ENDPOINT = "api/components/";
  private final WsClient wsClient;

  public ComponentsWsClient(WsClient wsClient) {
    this.wsClient = wsClient;
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return wsClient.execute(
      newGetRequest(action("search"))
        .setParam("qualifiers", request.getQualifiers())
        .setParam("p", request.getPage())
        .setParam("ps", request.getPageSize())
        .setParam("q", request.getQuery()),
      SearchWsResponse.parser());
  }

  private static String action(String action) {
    return ENDPOINT + action;
  }
}
