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
package org.sonarqube.ws.client.projecttags;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.ProjectTags.SearchResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_tags">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ProjectTagsService extends BaseService {

  public ProjectTagsService(WsConnector wsConnector) {
    super(wsConnector, "api/project_tags");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_tags/search">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public SearchResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      SearchResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_tags/set">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public void set(SetRequest request) {
    call(
      new PostRequest(path("set"))
        .setParam("project", request.getProject())
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
