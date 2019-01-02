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
package org.sonarqube.ws.client.applications;

import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ApplicationsService extends BaseService {

  public ApplicationsService(WsConnector wsConnector) {
    super(wsConnector, "api/applications");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/add_project">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void addProject(AddProjectRequest request) {
    call(
      new PostRequest(path("add_project"))
        .setParam("application", request.getApplication())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/create">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public String create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("description", request.getDescription())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setParam("visibility", request.getVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/create_branch">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void createBranch(CreateBranchRequest request) {
    call(
      new PostRequest(path("create_branch"))
        .setParam("application", request.getApplication())
        .setParam("branch", request.getBranch())
        .setParam("project", request.getProject())
        .setParam("projectBranch", request.getProjectBranch())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/delete">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("application", request.getApplication())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/delete_branch">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void deleteBranch(DeleteBranchRequest request) {
    call(
      new PostRequest(path("delete_branch"))
        .setParam("application", request.getApplication())
        .setParam("branch", request.getBranch())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/remove_project">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void removeProject(RemoveProjectRequest request) {
    call(
      new PostRequest(path("remove_project"))
        .setParam("application", request.getApplication())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/search_projects">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public String searchProjects(SearchProjectsRequest request) {
    return call(
      new GetRequest(path("search_projects"))
        .setParam("application", request.getApplication())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("selected", request.getSelected())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/show">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public String show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("application", request.getApplication())
        .setParam("branch", request.getBranch())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/show_leak">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public String showLeak(ShowLeakRequest request) {
    return call(
      new GetRequest(path("show_leak"))
        .setParam("application", request.getApplication())
        .setParam("branch", request.getBranch())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/update">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("application", request.getApplication())
        .setParam("description", request.getDescription())
        .setParam("name", request.getName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/applications/update_branch">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public void updateBranch(UpdateBranchRequest request) {
    call(
      new PostRequest(path("update_branch"))
        .setParam("application", request.getApplication())
        .setParam("branch", request.getBranch())
        .setParam("name", request.getName())
        .setParam("project", request.getProject())
        .setParam("projectBranch", request.getProjectBranch())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
