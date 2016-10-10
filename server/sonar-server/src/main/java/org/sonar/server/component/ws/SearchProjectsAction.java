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

package org.sonar.server.component.ws;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static org.sonar.api.utils.Paging.offset;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.DEFAULT_PAGE_SIZE;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.MAX_PAGE_SIZE;

public class SearchProjectsAction implements ComponentsWsAction {
  private final DbClient dbClient;

  public SearchProjectsAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("search_projects")
      .setSince("6.2")
      .setDescription("Search for projects")
      .addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE)
      .setInternal(true)
      .setResponseExample(getClass().getResource("search_projects-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    SearchProjectsWsResponse response = doHandle(toRequest(httpRequest));

    writeProtobuf(response, httpRequest, httpResponse);
  }

  private SearchProjectsWsResponse doHandle(SearchProjectsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchResult searchResult = searchProjects(dbSession, request);

      return buildResponse(request, searchResult);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private SearchResult searchProjects(DbSession dbSession, SearchProjectsRequest request) {
    List<ComponentDto> allProjects = dbClient.componentDao().selectProjects(dbSession);
    List<ComponentDto> projects = allProjects
      .stream()
      .sorted((c1, c2) -> c1.name().compareTo(c2.name()))
      .skip(offset(request.getPage(), request.getPageSize()))
      .limit(request.getPageSize())
      .collect(Collectors.toList());

    return new SearchResult(projects, allProjects.size());
  }

  private static SearchProjectsRequest toRequest(Request httpRequest) {
    SearchProjectsRequest.Builder request = SearchProjectsRequest.builder();

    request.setPage(httpRequest.mandatoryParamAsInt(Param.PAGE));
    request.setPageSize(httpRequest.mandatoryParamAsInt(Param.PAGE_SIZE));

    return request.build();
  }

  private static SearchProjectsWsResponse buildResponse(SearchProjectsRequest request, SearchResult searchResult) {
    SearchProjectsWsResponse.Builder response = SearchProjectsWsResponse.newBuilder();

    response.setPaging(Common.Paging.newBuilder()
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setTotal(searchResult.total));

    Function<ComponentDto, Component.Builder> dbToWsComponent = dbComponent -> Component.newBuilder()
      .setId(dbComponent.uuid())
      .setKey(dbComponent.key())
      .setName(dbComponent.name());

    searchResult.projects
      .stream()
      .map(dbToWsComponent)
      .sorted((component1, component2) -> component1.getName().compareTo(component2.getName()))
      .forEach(response::addComponents);

    return response.build();
  }

  private static class SearchResult {
    private final List<ComponentDto> projects;
    private final int total;

    private SearchResult(List<ComponentDto> projects, int total) {
      this.projects = projects;
      this.total = total;
    }
  }
}
