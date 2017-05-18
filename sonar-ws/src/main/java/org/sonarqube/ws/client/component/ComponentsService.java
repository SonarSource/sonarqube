/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.component;

import com.google.common.base.Joiner;
import java.util.List;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.WsComponents.SearchWsResponse;
import org.sonarqube.ws.WsComponents.ShowWsResponse;
import org.sonarqube.ws.WsComponents.TreeWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonar.api.server.ws.WebService.Param;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SEARCH_PROJECTS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_TREE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.CONTROLLER_COMPONENTS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;

public class ComponentsService extends BaseService {

  public ComponentsService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_COMPONENTS);
  }

  public SearchWsResponse search(SearchWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SEARCH))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_QUALIFIERS, Joiner.on(",").join(request.getQualifiers()))
      .setParam(Param.PAGE, request.getPage())
      .setParam(Param.PAGE_SIZE, request.getPageSize())
      .setParam(Param.TEXT_QUERY, request.getQuery());
    return call(get, SearchWsResponse.parser());
  }

  public TreeWsResponse tree(TreeWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_TREE))
      .setParam(PARAM_COMPONENT_ID, request.getBaseComponentId())
      .setParam(PARAM_COMPONENT, request.getBaseComponentKey())
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
      .setParam(PARAM_COMPONENT_ID, request.getId())
      .setParam(PARAM_COMPONENT, request.getKey());
    return call(get, ShowWsResponse.parser());
  }

  public SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) {
    List<String> additionalFields = request.getAdditionalFields();
    List<String> facets = request.getFacets();
    GetRequest get = new GetRequest(path(ACTION_SEARCH_PROJECTS))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_FILTER, request.getFilter())
      .setParam(Param.FACETS, !facets.isEmpty() ? inlineMultipleParamValue(facets) : null)
      .setParam(Param.SORT, request.getSort())
      .setParam(Param.ASCENDING, request.getAsc())
      .setParam(Param.PAGE, request.getPage())
      .setParam(Param.PAGE_SIZE, request.getPageSize())
      .setParam(Param.FIELDS, !additionalFields.isEmpty() ? inlineMultipleParamValue(additionalFields) : null);
    return call(get, SearchProjectsWsResponse.parser());
  }
}
