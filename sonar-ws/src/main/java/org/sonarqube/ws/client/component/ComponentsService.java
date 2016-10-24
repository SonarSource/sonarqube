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
import org.sonarqube.ws.WsComponents.BulkUpdateKeyWsResponse;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.WsComponents.ShowWsResponse;
import org.sonarqube.ws.WsComponents.TreeWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonar.api.server.ws.WebService.Param;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_BULK_UPDATE_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH_PROJECTS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_TREE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_UPDATE_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.CONTROLLER_COMPONENTS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_NEW_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_TO;

public class ComponentsService extends BaseService {

  public ComponentsService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_COMPONENTS);
  }

  public SearchWsResponse search(SearchWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SEARCH))
      .setParam(PARAM_QUALIFIERS, Joiner.on(",").join(request.getQualifiers()))
      .setParam(Param.PAGE, request.getPage())
      .setParam(Param.PAGE_SIZE, request.getPageSize())
      .setParam(Param.TEXT_QUERY, request.getQuery());
    return call(get, SearchWsResponse.parser());
  }

  public TreeWsResponse tree(TreeWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_TREE))
      .setParam(PARAM_BASE_COMPONENT_ID, request.getBaseComponentId())
      .setParam(PARAM_BASE_COMPONENT_KEY, request.getBaseComponentKey())
      .setParam(PARAM_QUALIFIERS, inlineMultipleParamValue(request.getQualifiers()))
      .setParam(PARAM_STRATEGY, request.getStrategy())
      .setParam(Param.PAGE, request.getPage())
      .setParam(Param.PAGE_SIZE, request.getPageSize())
      .setParam(Param.TEXT_QUERY, request.getQuery())
      .setParam(Param.SORT, request.getSort());
    return call(get, TreeWsResponse.parser());
  }

  public ShowWsResponse show(ShowWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SHOW))
      .setParam(PARAM_ID, request.getId())
      .setParam(PARAM_KEY, request.getKey());
    return call(get, ShowWsResponse.parser());
  }

  public void updateKey(UpdateWsRequest request) {
    PostRequest post = new PostRequest(path(ACTION_UPDATE_KEY))
      .setParam(PARAM_ID, request.getId())
      .setParam(PARAM_KEY, request.getKey())
      .setParam(PARAM_NEW_KEY, request.getNewKey());

    call(post);
  }

  public BulkUpdateKeyWsResponse bulkUpdateKey(BulkUpdateWsRequest request) {
    PostRequest post = new PostRequest(path(ACTION_BULK_UPDATE_KEY))
      .setParam(PARAM_ID, request.getId())
      .setParam(PARAM_KEY, request.getKey())
      .setParam(PARAM_FROM, request.getFrom())
      .setParam(PARAM_TO, request.getTo());

    return call(post, BulkUpdateKeyWsResponse.parser());
  }

  public SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SEARCH_PROJECTS))
      .setParam(PARAM_FILTER, request.getFilter())
      .setParam(Param.FACETS, request.getFacets())
      .setParam(Param.PAGE, request.getPage())
      .setParam(Param.PAGE_SIZE, request.getPageSize());
    return call(get, SearchProjectsWsResponse.parser());
  }
}
