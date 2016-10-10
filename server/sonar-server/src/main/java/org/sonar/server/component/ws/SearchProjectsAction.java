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

import static java.util.Comparator.comparing;
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
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchResults searchResults = searchProjects(dbSession, request);

      return buildResponse(request, searchResults);
    }
  }

  private SearchResults searchProjects(DbSession dbSession, SearchProjectsRequest request) {
    List<ComponentDto> allProjects = dbClient.componentDao().selectProjects(dbSession);
    List<ComponentDto> projects = allProjects
      .stream()
      .sorted(comparing(ComponentDto::name))
      .skip(offset(request.getPage(), request.getPageSize()))
      .limit(request.getPageSize())
      .collect(Collectors.toList());

    return new SearchResults(projects, allProjects.size());
  }

  private static SearchProjectsRequest toRequest(Request httpRequest) {
    SearchProjectsRequest.Builder request = SearchProjectsRequest.builder();

    request.setPage(httpRequest.mandatoryParamAsInt(Param.PAGE));
    request.setPageSize(httpRequest.mandatoryParamAsInt(Param.PAGE_SIZE));

    return request.build();
  }

  private static SearchProjectsWsResponse buildResponse(SearchProjectsRequest request, SearchResults searchResults) {
    SearchProjectsWsResponse.Builder response = SearchProjectsWsResponse.newBuilder();

    response.setPaging(Common.Paging.newBuilder()
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setTotal(searchResults.total));

    Function<ComponentDto, Component> dbToWsComponent = new DbToWsComponent();

    searchResults.projects
      .stream()
      .map(dbToWsComponent)
      .sorted(comparing(Component::getName))
      .forEach(response::addComponents);

    return response.build();
  }

  private static class DbToWsComponent implements Function<ComponentDto, Component> {
    private final Component.Builder wsComponent;

    private DbToWsComponent() {
      this.wsComponent = Component.newBuilder();
    }

    @Override
    public Component apply(ComponentDto dbComponent) {
      return wsComponent
        .clear()
        .setId(dbComponent.uuid())
        .setKey(dbComponent.key())
        .setName(dbComponent.name())
        .build();
    }
  }

  private static class SearchResults {
    private final List<ComponentDto> projects;
    private final int total;

    private SearchResults(List<ComponentDto> projects, int total) {
      this.projects = projects;
      this.total = total;
    }
  }
}
