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
package org.sonarqube.ws.client.projects;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Projects.BulkUpdateKeyWsResponse;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse;

/**
 * Manage project existence.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ProjectsService extends BaseService {

  public ProjectsService(WsConnector wsConnector) {
    super(wsConnector, "api/projects");
  }

  /**
   * Delete one or several projects.<br />Requires 'Administer System' permission.
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
        .setParam("projectIds", request.getProjectIds() == null ? null : request.getProjectIds().stream().collect(Collectors.joining(",")))
        .setParam("projects", request.getProjects() == null ? null : request.getProjects().stream().collect(Collectors.joining(",")))
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("visibility", request.getVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Bulk update a project or module key and all its sub-components keys. The bulk update allows to replace a part of the current key by another string on the current project and all its sub-modules.<br>It's possible to simulate the bulk update by setting the parameter 'dryRun' at true. No key is updated with a dry run.<br>Ex: to rename a project with key 'my_project' to 'my_new_project' and all its sub-components keys, call the WS with parameters:<ul>  <li>project: my_project</li>  <li>from: my_</li>  <li>to: my_new_</li></ul>Either 'projectId' or 'project' must be provided.<br> Requires one of the following permissions: <ul><li>'Administer System'</li><li>'Administer' rights on the specified project</li></ul>
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
        .setParam("projectId", request.getProjectId())
        .setParam("to", request.getTo()),
      BulkUpdateKeyWsResponse.parser());
  }

  /**
   * Create a project.<br/>Requires 'Create Projects' permission
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
   * Delete a project.<br> Requires 'Administer System' permission or 'Administer' permission on the project.
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
        .setParam("projectId", request.getProjectId())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * List ghost projects.<br> With the current architecture, it's no more possible to have invisible ghost projects. Therefore, the web service is deprecated.<br> Requires 'Administer System' permission.
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/ghosts">Further information about this action online (including a response example)</a>
   * @since 5.2
   * @deprecated since 6.6
   */
  @Deprecated
  public String ghosts(GhostsRequest request) {
    return call(
      new GetRequest(path("ghosts"))
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * This web service is deprecated, please use api/components/search instead
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/index">Further information about this action online (including a response example)</a>
   * @since 2.10
   * @deprecated since 6.3
   */
  @Deprecated
  public String index(IndexRequest request) {
    return call(
      new GetRequest(path("index"))
        .setParam("desc", request.getDesc())
        .setParam("format", request.getFormat())
        .setParam("libs", request.getLibs())
        .setParam("project", request.getProject())
        .setParam("search", request.getSearch())
        .setParam("subprojects", request.getSubprojects())
        .setParam("versions", request.getVersions())
        .setParam("views", request.getViews())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Get the list of provisioned projects.<br> Web service is deprecated. Use api/projects/search instead, with onProvisionedOnly=true.<br> Require 'Create Projects' permission.
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/provisioned">Further information about this action online (including a response example)</a>
   * @since 5.2
   * @deprecated since 6.6
   */
  @Deprecated
  public String provisioned(ProvisionedRequest request) {
    return call(
      new GetRequest(path("provisioned"))
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Search for projects or views to administrate them.<br>Requires 'System Administrator' permission
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
        .setParam("projectIds", request.getProjectIds() == null ? null : request.getProjectIds().stream().collect(Collectors.joining(",")))
        .setParam("projects", request.getProjects() == null ? null : request.getProjects().stream().collect(Collectors.joining(",")))
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("visibility", request.getVisibility()),
      SearchWsResponse.parser());
  }

  /**
   * Return list of projects for which the current user has 'Administer' permission.
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
   * Update a project or module key and all its sub-components keys.<br>Either 'from' or 'projectId' must be provided.<br> Requires one of the following permissions: <ul><li>'Administer System'</li><li>'Administer' rights on the specified project</li></ul>
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
        .setParam("projectId", request.getProjectId())
        .setParam("to", request.getTo())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Updates visibility of a project.<br>Requires 'Project administer' permission on the specified project
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
}
