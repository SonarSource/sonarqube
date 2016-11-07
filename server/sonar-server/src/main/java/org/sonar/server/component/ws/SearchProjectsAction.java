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
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collector;
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
import static org.sonar.server.component.es.ProjectMeasuresIndex.SUPPORTED_FACETS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.DEFAULT_PAGE_SIZE;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.MAX_PAGE_SIZE;

public class SearchProjectsAction implements ComponentsWsAction {
  private final DbClient dbClient;
  private final ProjectMeasuresIndex index;
  private final ProjectMeasuresQueryValidator queryValidator;
  private final ProjectMeasuresQueryFactory queryFactory;

  public SearchProjectsAction(DbClient dbClient, ProjectMeasuresIndex index, ProjectMeasuresQueryFactory queryFactory,
    ProjectMeasuresQueryValidator queryValidator) {
    this.dbClient = dbClient;
    this.index = index;
    this.queryFactory = queryFactory;
    this.queryValidator = queryValidator;
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

    action.createParam(Param.FACETS)
      .setDescription("Comma-separated list of the facets to be computed. No facet is computed by default.")
      .setPossibleValues(SUPPORTED_FACETS);
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
    ProjectMeasuresQuery query = queryFactory.newProjectMeasuresQuery(dbSession, filter);
    queryValidator.validate(dbSession, query);

    SearchIdResult<String> searchResults = index.search(query, new SearchOptions()
      .addFacets(request.getFacets())
      .setPage(request.getPage(), request.getPageSize()));

    Ordering<ComponentDto> ordering = Ordering.explicit(searchResults.getIds()).onResultOf(ComponentDto::uuid);
    List<ComponentDto> projects = ordering.immutableSortedCopy(dbClient.componentDao().selectByUuids(dbSession, searchResults.getIds()));

    return new SearchResults(projects, searchResults);
  }

  private static SearchProjectsRequest toRequest(Request httpRequest) {
    SearchProjectsRequest.Builder request = SearchProjectsRequest.builder()
      .setFilter(httpRequest.param(PARAM_FILTER))
      .setPage(httpRequest.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(httpRequest.mandatoryParamAsInt(Param.PAGE_SIZE));
    if (httpRequest.hasParam(Param.FACETS)) {
      request.setFacets(httpRequest.paramAsStrings(Param.FACETS));
    }
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

  private static SearchProjectsWsResponse.Builder addFacets(Facets esFacets, SearchProjectsWsResponse.Builder wsResponse) {
    EsToWsFacet esToWsFacet = new EsToWsFacet();

    Common.Facets wsFacets = esFacets.getAll().entrySet().stream()
      .map(esToWsFacet)
      .collect(Collector.of(
        Common.Facets::newBuilder,
        Common.Facets.Builder::addFacets,
        (result1, result2) -> {
          throw new IllegalStateException("Parallel execution forbidden");
        },
        Common.Facets.Builder::build));

    wsResponse.setFacets(wsFacets);

    return wsResponse;
  }

  private static class EsToWsFacet implements Function<Entry<String, LinkedHashMap<String, Long>>, Common.Facet> {
    private final BucketToFacetValue bucketToFacetValue = new BucketToFacetValue();
    private final Common.Facet.Builder wsFacet = Common.Facet.newBuilder();

    @Override
    public Common.Facet apply(Entry<String, LinkedHashMap<String, Long>> esFacet) {
      wsFacet
        .clear()
        .setProperty(esFacet.getKey());
      LinkedHashMap<String, Long> buckets = esFacet.getValue();
      if (buckets != null) {
        buckets.entrySet()
          .stream()
          .map(bucketToFacetValue)
          .forEach(wsFacet::addValues);
      } else {
        wsFacet.addAllValues(Collections.<Common.FacetValue>emptyList());
      }

      return wsFacet.build();
    }
  }

  private static class BucketToFacetValue implements Function<Entry<String, Long>, Common.FacetValue> {
    private final Common.FacetValue.Builder facetValue;

    private BucketToFacetValue() {
      this.facetValue = Common.FacetValue.newBuilder();
    }

    @Override
    public Common.FacetValue apply(Entry<String, Long> bucket) {
      return facetValue
        .clear()
        .setVal(bucket.getKey())
        .setCount(bucket.getValue())
        .build();
    }
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

    private SearchResults(List<ComponentDto> projects, SearchIdResult<String> searchResults) {
      this.projects = projects;
      this.total = (int) searchResults.getTotal();
      this.facets = searchResults.getFacets();
    }
  }
}
