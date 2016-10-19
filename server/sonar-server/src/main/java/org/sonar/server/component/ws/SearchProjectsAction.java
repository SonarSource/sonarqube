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

import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.es.ProjectMeasuresIndex;
import org.sonar.server.component.es.ProjectMeasuresQuery;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.DEFAULT_PAGE_SIZE;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.MAX_PAGE_SIZE;

public class SearchProjectsAction implements ComponentsWsAction {
  private final DbClient dbClient;
  private final ProjectMeasuresIndex index;
  private final ProjectMeasuresQueryValidator projectMeasuresQueryValidator;

  public SearchProjectsAction(DbClient dbClient, ProjectMeasuresIndex index, ProjectMeasuresQueryValidator projectMeasuresQueryValidator) {
    this.dbClient = dbClient;
    this.index = index;
    this.projectMeasuresQueryValidator = projectMeasuresQueryValidator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_projects")
      .setSince("6.2")
      .setDescription("Search for projects")
      .addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE)
      .setInternal(true)
      .setResponseExample(getClass().getResource("search_projects-example.json"))
      .setHandler(this);

    action
      .createParam(PARAM_FILTER)
      .setDescription("TODO");
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
    String filter = firstNonNull(request.getFilter(), "");
    ProjectMeasuresQuery query = newProjectMeasuresQuery(filter);
    projectMeasuresQueryValidator.validate(dbSession, query);

    SearchIdResult<String> searchResult = index.search(query, new SearchOptions().setPage(request.getPage(), request.getPageSize()));

    Ordering<ComponentDto> ordering = Ordering.explicit(searchResult.getIds()).onResultOf(ComponentDto::uuid);
    List<ComponentDto> projects = ordering.immutableSortedCopy(dbClient.componentDao().selectByUuids(dbSession, searchResult.getIds()));

    return new SearchResults(projects, searchResult.getFacets(), searchResult.getTotal());
  }

  private static SearchProjectsRequest toRequest(Request httpRequest) {
    SearchProjectsRequest.Builder request = SearchProjectsRequest.builder()
      .setFilter(httpRequest.param(PARAM_FILTER))
      .setPage(httpRequest.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(httpRequest.mandatoryParamAsInt(Param.PAGE_SIZE));
    return request.build();
  }

  private static SearchProjectsWsResponse buildResponse(SearchProjectsRequest request, SearchResults searchResults) {
    Function<ComponentDto, Component> dbToWsComponent = new DbToWsComponent();

    return Stream.of(SearchProjectsWsResponse.newBuilder())
      .map(response -> response.setPaging(Common.Paging.newBuilder()
        .setPageIndex(request.getPage())
        .setPageSize(request.getPageSize())
        .setTotal(searchResults.total)))
      .map(response -> {
        searchResults.projects.stream()
          .map(dbToWsComponent)
          .forEach(response::addComponents);
        return response;
      })
      .map(response -> addFacets(searchResults.facets, response))
      .map(SearchProjectsWsResponse.Builder::build)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("SearchProjectsWsResponse not built"));
  }

  private static SearchProjectsWsResponse.Builder addFacets(Facets facets, SearchProjectsWsResponse.Builder wsResponse) {
    Common.Facets.Builder wsFacets = Common.Facets.newBuilder();
    Common.Facet.Builder wsFacet = Common.Facet.newBuilder();
    for (Map.Entry<String, LinkedHashMap<String, Long>> facet : facets.getAll().entrySet()) {
      wsFacet.clear();
      wsFacet.setProperty(facet.getKey());
      LinkedHashMap<String, Long> buckets = facet.getValue();
      if (buckets != null) {
        for (Map.Entry<String, Long> bucket : buckets.entrySet()) {
          Common.FacetValue.Builder valueBuilder = wsFacet.addValuesBuilder();
          valueBuilder.setVal(bucket.getKey());
          valueBuilder.setCount(bucket.getValue());
          valueBuilder.build();
        }
      } else {
        wsFacet.addAllValues(Collections.<Common.FacetValue>emptyList());
      }
      wsFacets.addFacets(wsFacet);
    }
    wsResponse.setFacets(wsFacets);

    return wsResponse;
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
    private final Facets facets;
    private final int total;

    private SearchResults(List<ComponentDto> projects, Facets facets, long total) {
      this.projects = projects;
      this.total = (int) total;
      this.facets = facets;
    }
  }
}
