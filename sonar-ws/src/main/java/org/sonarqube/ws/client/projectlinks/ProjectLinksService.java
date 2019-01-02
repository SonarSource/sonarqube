/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client.projectlinks;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.ProjectLinks.CreateWsResponse;
import org.sonarqube.ws.ProjectLinks.SearchWsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_links">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ProjectLinksService extends BaseService {

  public ProjectLinksService(WsConnector wsConnector) {
    super(wsConnector, "api/project_links");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_links/create">Further information about this action online (including a response example)</a>
   * @since 6.1
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("name", request.getName())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setParam("url", request.getUrl()),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_links/delete">Further information about this action online (including a response example)</a>
   * @since 6.1
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("id", request.getId())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_links/search">Further information about this action online (including a response example)</a>
   * @since 6.1
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey()),
      SearchWsResponse.parser());
  }
}
