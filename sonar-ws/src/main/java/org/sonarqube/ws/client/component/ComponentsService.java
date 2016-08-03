/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonarqube.ws.client.component;

import com.google.common.base.Joiner;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.WsComponents.ShowWsResponse;
import org.sonarqube.ws.WsComponents.TreeWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_TREE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_NEW_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;

public class ComponentsService extends BaseService {

  public ComponentsService(WsConnector wsConnector) {
    super(wsConnector, "api/components");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    GetRequest get = new GetRequest(path("search"))
      .setParam("qualifiers", Joiner.on(",").join(request.getQualifiers()))
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("q", request.getQuery());
    return call(get, SearchWsResponse.parser());
  }

  public TreeWsResponse tree(TreeWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_TREE))
      .setParam(PARAM_BASE_COMPONENT_ID, request.getBaseComponentId())
      .setParam(PARAM_BASE_COMPONENT_KEY, request.getBaseComponentKey())
      .setParam(PARAM_QUALIFIERS, inlineMultipleParamValue(request.getQualifiers()))
      .setParam(PARAM_STRATEGY, request.getStrategy())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("q", request.getQuery())
      .setParam("s", request.getSort());
    return call(get, TreeWsResponse.parser());
  }

  public ShowWsResponse show(ShowWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SHOW))
      .setParam(PARAM_ID, request.getId())
      .setParam(PARAM_KEY, request.getKey());
    return call(get, ShowWsResponse.parser());
  }

  public void updateKey(UpdateWsRequest request) {
    PostRequest post = new PostRequest(path("update_key"))
      .setParam(PARAM_ID, request.getId())
      .setParam(PARAM_KEY, request.getKey())
      .setParam(PARAM_NEW_KEY, request.getNewKey());

    call(post);
  }
}
