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
package org.sonarqube.ws.client.project;

import com.google.common.base.Joiner;
import org.sonarqube.ws.WsProjects.BulkUpdateKeyWsResponse;
import org.sonarqube.ws.WsProjects.CreateWsResponse;
import org.sonarqube.ws.WsProjects.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_BULK_UPDATE_KEY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_UPDATE_KEY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_UPDATE_VISIBILITY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_TO;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

/**
 * Maps web service {@code api/projects}.
 * @since 5.5
 */
public class ProjectsService extends BaseService {

  public ProjectsService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER);
  }

  /**
   * Provisions a new project.
   *
   * @throws org.sonarqube.ws.client.HttpException if HTTP status code is not 2xx.
   */
  public CreateWsResponse create(CreateRequest project) {
    PostRequest request = new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_ORGANIZATION, project.getOrganization())
      .setParam(PARAM_PROJECT, project.getKey())
      .setParam(PARAM_NAME, project.getName())
      .setParam(PARAM_BRANCH, project.getBranch())
      .setParam(PARAM_VISIBILITY, project.getVisibility());
    return call(request, CreateWsResponse.parser());
  }

  /**
   * @throws org.sonarqube.ws.client.HttpException if HTTP status code is not 2xx.
   */
  public void delete(DeleteRequest request) {
    call(new PostRequest(path("delete"))
      .setParam("id", request.getId())
      .setParam("key", request.getKey()));
  }

  public void updateKey(UpdateKeyWsRequest request) {
    PostRequest post = new PostRequest(path(ACTION_UPDATE_KEY))
      .setParam(PARAM_PROJECT_ID, request.getId())
      .setParam(PARAM_FROM, request.getKey())
      .setParam(PARAM_TO, request.getNewKey());

    call(post);
  }

  public BulkUpdateKeyWsResponse bulkUpdateKey(BulkUpdateKeyWsRequest request) {
    PostRequest post = new PostRequest(path(ACTION_BULK_UPDATE_KEY))
      .setParam(PARAM_PROJECT_ID, request.getId())
      .setParam(PARAM_PROJECT, request.getKey())
      .setParam(ProjectsWsParameters.PARAM_FROM, request.getFrom())
      .setParam(ProjectsWsParameters.PARAM_TO, request.getTo());

    return call(post, BulkUpdateKeyWsResponse.parser());
  }

  public SearchWsResponse search(SearchWsRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SEARCH))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_QUALIFIERS, Joiner.on(",").join(request.getQualifiers()))
      .setParam(TEXT_QUERY, request.getQuery())
      .setParam(PAGE, request.getPage())
      .setParam(PAGE_SIZE, request.getPageSize());
    return call(get, SearchWsResponse.parser());
  }

  public void updateVisibility(UpdateVisibilityRequest request) {
    PostRequest post = new PostRequest(path(ACTION_UPDATE_VISIBILITY))
      .setParam(PARAM_PROJECT, request.getProject())
      .setParam(PARAM_VISIBILITY, request.getVisibility());
    call(post);
  }
}
