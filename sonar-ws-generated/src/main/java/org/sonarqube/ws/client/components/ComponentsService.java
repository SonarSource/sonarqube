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
 * Get information about a component (file, directory, project, ...) and its ancestors or descendants. Update a project or module key.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ComponentsService extends BaseService {

  public ComponentsService(WsConnector wsConnector) {
    super(wsConnector, "api/components");
  }

  /**
   * Coverage data required for rendering the component viewer.<br>Requires the following permission: 'Browse'.
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
   * Search for components
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
   * Search for projects
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
   * Returns a component (file, directory, project, view?) and its ancestors. The ancestors are ordered from the parent to the root project. The 'componentId' or 'component' parameter must be provided.<br>Requires the following permission: 'Browse' on the project of the specified component.
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
   * Internal WS for the top-right search engine. The result will contain component search results, grouped by their qualifiers.<p>Each result contains:<ul><li>the organization key</li><li>the component key</li><li>the component's name (unescaped)</li><li>optionally a display name, which puts emphasis to matching characters (this text contains html tags and parts of the html-escaped name)</li></ul>
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
   * Navigate through components based on the chosen strategy. The componentId or the component parameter must be provided.<br>Requires the following permission: 'Browse' on the specified project.<br>When limiting search with the q parameter, directories are not returned.
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
