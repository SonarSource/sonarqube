/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonarqube.ws.WsProjectLinks.CreateWsResponse;
import org.sonarqube.ws.WsProjectLinks.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.ACTION_DELETE;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_URL;

public class ProjectLinksService extends BaseService {

  public ProjectLinksService(WsConnector wsConnector) {
    super(wsConnector, "api/project_links");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(new GetRequest(path(ACTION_SEARCH))
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_PROJECT_ID, request.getProjectId()),
      SearchWsResponse.parser());
  }

  public CreateWsResponse create(CreateWsRequest request) {
    return call(new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_URL, request.getUrl()),
      CreateWsResponse.parser());
  }

  public void delete(DeleteWsRequest request) {
    call(new PostRequest(path(ACTION_DELETE))
      .setParam(PARAM_ID, request.getId()));
  }
}
