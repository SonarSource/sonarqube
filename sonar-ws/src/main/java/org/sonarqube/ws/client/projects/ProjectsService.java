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
package org.sonarqube.ws.client.projects;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Projects.BulkUpdateKeyWsResponse;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ProjectsService extends BaseService {

  public ProjectsService(WsConnector wsConnector) {
    super(wsConnector, "api/projects");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/bulk_delete">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void bulkDelete(BulkDeleteRequest request) {
    call(
      new PostRequest(path("bulk_delete"))
        .setParam("analyzedBefore", request.getAnalyzedBefore())
        .setParam("onProvisionedOnly", request.getOnProvisionedOnly())
        .setParam("organization", request.getOrganization())
        .setParam("projects", request.getProjects() == null ? null : request.getProjects().stream().collect(Collectors.joining(",")))
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("visibility", request.getVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/bulk_update_key">Further information about this action online (including a response example)</a>
   * @since 6.1
   */
  public BulkUpdateKeyWsResponse bulkUpdateKey(BulkUpdateKeyRequest request) {
    return call(
      new PostRequest(path("bulk_update_key"))
        .setParam("dryRun", request.getDryRun())
        .setParam("from", request.getFrom())
        .setParam("project", request.getProject())
        .setParam("to", request.getTo()),
      BulkUpdateKeyWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/create">Further information about this action online (including a response example)</a>
   * @since 4.0
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("branch", request.getBranch())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject())
        .setParam("visibility", request.getVisibility()),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/delete">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/search">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("analyzedBefore", request.getAnalyzedBefore())
        .setParam("onProvisionedOnly", request.getOnProvisionedOnly())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("projects", request.getProjects() == null ? null : request.getProjects().stream().collect(Collectors.joining(",")))
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("visibility", request.getVisibility()),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/search_my_projects">Further information about this action online (including a response example)</a>
   * @since 6.0
   */
  public SearchMyProjectsWsResponse searchMyProjects(SearchMyProjectsRequest request) {
    return call(
      new GetRequest(path("search_my_projects"))
        .setParam("p", request.getP())
        .setParam("ps", request.getPs()),
      SearchMyProjectsWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/update_key">Further information about this action online (including a response example)</a>
   * @since 6.1
   */
  public void updateKey(UpdateKeyRequest request) {
    call(
      new PostRequest(path("update_key"))
        .setParam("from", request.getFrom())
        .setParam("to", request.getTo())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/update_visibility">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public void updateVisibility(UpdateVisibilityRequest request) {
    call(
      new PostRequest(path("update_visibility"))
        .setParam("project", request.getProject())
        .setParam("visibility", request.getVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/update_default_visibility">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public void updateDefaultVisibility(UpdateDefaultVisibilityRequest request) {
    call(
      new PostRequest(path("update_default_visibility"))
        .setParam("projectVisibility", request.getProjectVisibility())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }
}
