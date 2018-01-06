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
package org.sonar.server.projecttag.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonarqube.ws.ProjectTags;

import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements ProjectTagsWsAction {
  private final ProjectMeasuresIndex index;

  public SearchAction(ProjectMeasuresIndex index) {
    this.index = index;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setDescription("Search tags")
      .setSince("6.4")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this);

    action.addSearchQuery("off", "tags");
    action.createPageSize(10, 100);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ProjectTags.SearchResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private ProjectTags.SearchResponse doHandle(Request request) {
    List<String> tags = index.searchTags(request.param(TEXT_QUERY), request.mandatoryParamAsInt(PAGE_SIZE));
    return ProjectTags.SearchResponse.newBuilder().addAllTags(tags).build();
  }
}
