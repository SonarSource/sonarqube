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
package org.sonarqube.ws.client.projectanalyses;

import javax.annotation.Generated;
import javax.annotation.Nullable;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.ProjectAnalyses.CreateEventResponse;
import org.sonarqube.ws.ProjectAnalyses.SearchResponse;
import org.sonarqube.ws.ProjectAnalyses.UpdateEventResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class ProjectAnalysesService extends BaseService {

  public ProjectAnalysesService(WsConnector wsConnector) {
    super(wsConnector, "api/project_analyses");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/create_event">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public CreateEventResponse createEvent(CreateEventRequest request) {
    return call(
      new PostRequest(path("create_event"))
        .setParam("analysis", request.getAnalysis())
        .setParam("category", request.getCategory())
        .setParam("name", request.getName()),
      CreateEventResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/delete">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("analysis", request.getAnalysis())
        .setMediaType(MediaTypes.JSON)).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/delete_event">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public void deleteEvent(DeleteEventRequest request) {
    call(
      new PostRequest(path("delete_event"))
        .setParam("event", request.getEvent())
        .setMediaType(MediaTypes.JSON)).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/search">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public SearchResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("branch", request.getBranch())
        .setParam("category", request.getCategory())
        .setParam("from", request.getFrom())
        .setParam("p", request.getP())
        .setParam("project", request.getProject())
        .setParam("ps", request.getPs())
        .setParam("pullRequest", request.getPullRequest())
        .setParam("to", request.getTo()),
      SearchResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/update_event">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public UpdateEventResponse updateEvent(UpdateEventRequest request) {
    return call(
      new PostRequest(path("update_event"))
        .setParam("event", request.getEvent())
        .setParam("name", request.getName()),
      UpdateEventResponse.parser());
  }

  public void set_baseline(String projectKey, @Nullable String branchName, String analysisUuid) {
    requireNonNull(projectKey, "projectKey can't be null");
    requireNonNull(analysisUuid, "analysisUuid can't be null");
    PostRequest request = new PostRequest(path("set_baseline"))
      .setParam("project", projectKey)
      .setParam("analysis", analysisUuid);
    ofNullable(branchName).ifPresent(t -> request.setParam("branch", t));
    call(request);
  }

  public void unset_baseline(String projectKey, @Nullable String branchName) {
    requireNonNull(projectKey, "projectKey can't be null");
    PostRequest request = new PostRequest(path("unset_baseline"))
      .setParam("project", projectKey);
    ofNullable(branchName).ifPresent(t -> request.setParam("branch", t));
    call(request);
  }
}
