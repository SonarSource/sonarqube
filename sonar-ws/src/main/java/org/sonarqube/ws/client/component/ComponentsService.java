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
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

public class ComponentsService extends BaseService {

  public ComponentsService(WsConnector wsConnector) {
    super(wsConnector, "api/components");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    GetRequest get = new GetRequest(path("search"))
      .setParam("qualifiers", request.getQualifiers())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("q", request.getQuery());
    return call(get, SearchWsResponse.parser());
  }
}
