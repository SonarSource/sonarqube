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
package org.sonarqube.ws.client.components;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Components.SearchWsResponse;
import org.sonarqube.ws.Components.SearchProjectsWsResponse;
import org.sonarqube.ws.Components.ShowWsResponse;
import org.sonarqube.ws.Components.SuggestionsWsResponse;
import org.sonarqube.ws.Components.TreeWsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ComponentsService extends BaseService {

  public ComponentsService(WsConnector wsConnector) {
    super(wsConnector, "api/components");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/app">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String app(AppRequest request) {
    return call(
      new GetRequest(path("app"))
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setParam("componentId", request.getComponentId())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/search">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("language", request.getLanguage())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(","))),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/search_projects">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public SearchProjectsWsResponse searchProjects(SearchProjectsRequest request) {
    return call(
      new GetRequest(path("search_projects"))
        .setParam("asc", request.getAsc())
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setParam("facets", request.getFacets() == null ? null : request.getFacets().stream().collect(Collectors.joining(",")))
        .setParam("filter", request.getFilter())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("s", request.getS()),
      SearchProjectsWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/show">Further information about this action online (including a response example)</a>
   * @since 5.4
   */
  public ShowWsResponse show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setParam("componentId", request.getComponentId()),
      ShowWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/suggestions">Further information about this action online (including a response example)</a>
   * @since 4.2
   */
  public SuggestionsWsResponse suggestions(SuggestionsRequest request) {
    return call(
      new GetRequest(path("suggestions"))
        .setParam("more", request.getMore())
        .setParam("recentlyBrowsed", request.getRecentlyBrowsed() == null ? null : request.getRecentlyBrowsed().stream().collect(Collectors.joining(",")))
        .setParam("s", request.getS()),
      SuggestionsWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/tree">Further information about this action online (including a response example)</a>
   * @since 5.4
   */
  public TreeWsResponse tree(TreeRequest request) {
    return call(
      new GetRequest(path("tree"))
        .setParam("asc", request.getAsc())
        .setParam("branch", request.getBranch())
        .setParam("component", request.getComponent())
        .setParam("componentId", request.getComponentId())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("s", request.getS() == null ? null : request.getS().stream().collect(Collectors.joining(",")))
        .setParam("strategy", request.getStrategy()),
      TreeWsResponse.parser());
  }
}
