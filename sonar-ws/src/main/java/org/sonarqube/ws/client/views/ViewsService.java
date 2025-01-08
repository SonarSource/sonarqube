/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.client.views;

import java.util.stream.Collectors;
import jakarta.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ViewsService extends BaseService {

  public ViewsService(WsConnector wsConnector) {
    super(wsConnector, "api/views");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/add_application">Further information about this action online (including a response example)</a>
   * @since 9.3
   */
  public void addApplication(AddApplicationRequest request) {
    call(
      new PostRequest(path("add_application"))
        .setParam("application", request.getApplication())
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/add_portfolio">Further information about this action online (including a response example)</a>
   * @since 9.3
   */
  public void addPortfolio(AddPortfolioRequest request) {
    call(
      new PostRequest(path("add_portfolio"))
        .setParam("portfolio", request.getPortfolio())
        .setParam("reference", request.getReference())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/add_project">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void addProject(AddProjectRequest request) {
    call(
      new PostRequest(path("add_project"))
        .setParam("key", request.getKey())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/add_project_branch">Further information about this action online (including a response example)</a>
   * @since 9.2
   */
  public void addProjectBranch(AddProjectBranchRequest request) {
    call(
      new PostRequest(path("add_project_branch"))
        .setParam("branch", request.getBranch())
        .setParam("key", request.getKey())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/applications">Further information about this action online (including a response example)</a>
   * @since 9.3
   */
  public String applications(ApplicationsRequest request) {
    return call(
      new GetRequest(path("applications"))
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/create">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void create(CreateRequest request) {
    call(
      new PostRequest(path("create"))
        .setParam("description", request.getDescription())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setParam("parent", request.getParent())
        .setParam("visibility", request.getVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/delete">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/list">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public String list() {
    return call(
      new GetRequest(path("list"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/move">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void move(MoveRequest request) {
    call(
      new PostRequest(path("move"))
        .setParam("destination", request.getDestination())
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/move_options">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public String moveOptions(MoveOptionsRequest request) {
    return call(
      new GetRequest(path("move_options"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/portfolios">Further information about this action online (including a response example)</a>
   * @since 9.3
   */
  public String portfolios(PortfoliosRequest request) {
    return call(
      new GetRequest(path("portfolios"))
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/projects">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public String projects(ProjectsRequest request) {
    return call(
      new GetRequest(path("projects"))
        .setParam("key", request.getKey())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("query", request.getQuery())
        .setParam("selected", request.getSelected())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/refresh">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public void refresh(RefreshRequest request) {
    call(
      new PostRequest(path("refresh"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/remove_application">Further information about this action online (including a response example)</a>
   * @since 9.3
   */
  public void removeApplication(RemoveApplicationRequest request) {
    call(
      new PostRequest(path("remove_application"))
        .setParam("application", request.getApplication())
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/remove_portfolio">Further information about this action online (including a response example)</a>
   * @since 9.3
   */
  public void removePortfolio(RemovePortfolioRequest request) {
    call(
      new PostRequest(path("remove_portfolio"))
        .setParam("portfolio", request.getPortfolio())
        .setParam("reference", request.getReference())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/remove_project">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void removeProject(RemoveProjectRequest request) {
    call(
      new PostRequest(path("remove_project"))
        .setParam("key", request.getKey())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/remove_project_branch">Further information about this action online (including a response example)</a>
   * @since 9.2
   */
  public void removeProjectBranch(RemoveProjectBranchRequest request) {
    call(
      new PostRequest(path("remove_project_branch"))
        .setParam("branch", request.getBranch())
        .setParam("key", request.getKey())
        .setParam("project", request.getProject())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/search">Further information about this action online (including a response example)</a>
   * @since 2.0
   */
  public String search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("onlyFavorites", request.getOnlyFavorites())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/set_manual_mode">Further information about this action online (including a response example)</a>
   * @since 7.4
   */
  public void setManualMode(SetManualModeRequest request) {
    call(
      new PostRequest(path("set_manual_mode"))
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/set_none_mode">Further information about this action online (including a response example)</a>
   * @since 9.1
   */
  public void setNoneMode(SetNoneModeRequest request) {
    call(
      new PostRequest(path("set_none_mode"))
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/set_regexp_mode">Further information about this action online (including a response example)</a>
   * @since 7.4
   */
  public void setRegexpMode(SetRegexpModeRequest request) {
    call(
      new PostRequest(path("set_regexp_mode"))
        .setParam("branch", request.getBranch())
        .setParam("portfolio", request.getPortfolio())
        .setParam("regexp", request.getRegexp())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/set_remaining_projects_mode">Further information about this action online (including a response example)</a>
   * @since 7.4
   */
  public void setRemainingProjectsMode(SetRemainingProjectsModeRequest request) {
    call(
      new PostRequest(path("set_remaining_projects_mode"))
        .setParam("branch", request.getBranch())
        .setParam("portfolio", request.getPortfolio())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/set_tags_mode">Further information about this action online (including a response example)</a>
   * @since 7.4
   */
  public void setTagsMode(SetTagsModeRequest request) {
    call(
      new PostRequest(path("set_tags_mode"))
        .setParam("branch", request.getBranch())
        .setParam("portfolio", request.getPortfolio())
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/show">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public String show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/update">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("description", request.getDescription())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

}
