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
package org.sonarqube.ws.client.projectanalysis;

import org.sonarqube.ws.ProjectAnalyses.SearchResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_PROJECT;

public class ProjectAnalysisService extends BaseService {

  public ProjectAnalysisService(WsConnector wsConnector) {
    super(wsConnector, "api/project_analyses");
  }

  public SearchResponse search(SearchRequest searchRequest) {
    EventCategory eventCategory = searchRequest.getCategory();
    GetRequest request = new GetRequest(path("search"))
      .setParam(PARAM_PROJECT, searchRequest.getProject())
      .setParam(PARAM_BRANCH, searchRequest.getBranch())
      .setParam(PARAM_CATEGORY, eventCategory == null ? null : eventCategory.name())
      .setParam(PAGE, searchRequest.getPage())
      .setParam(PAGE_SIZE, searchRequest.getPageSize());
    return call(request, SearchResponse.parser());
  }

}
