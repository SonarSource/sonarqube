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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.component.es.ProjectMeasuresIndex;
import org.sonar.server.component.es.ProjectMeasuresQuery;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchProjectsWsResponse;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.sonar.server.component.es.ProjectMeasuresIndex.SUPPORTED_FACETS;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.DEFAULT_PAGE_SIZE;
import static org.sonarqube.ws.client.component.SearchProjectsRequest.MAX_PAGE_SIZE;

public class SearchProjectsAction implements ComponentsWsAction {
  private final DbClient dbClient;
  private final ProjectMeasuresIndex index;
  private final ProjectMeasuresQueryValidator queryValidator;
  private final UserSession userSession;

  public SearchProjectsAction(DbClient dbClient, ProjectMeasuresIndex index, ProjectMeasuresQueryValidator queryValidator, UserSession userSession) {
    this.dbClient = dbClient;
    this.index = index;
    this.queryValidator = queryValidator;
    this.userSession = userSession;
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
      .setDescription("Filter of projects on measure value, quality gate or whether a project is a favorite or not.<br>" +
        "The filter must be encoded to form a valid URL (for example '=' must be replaced by '%3D').<br>" +
        "Examples of use:" +
        "<ul>" +
        " <li>to filter my favorite projects with a failed quality gate and a coverage greater than or equals to 60% and a coverage strictly lower than 80%:<br>" +
        "   <code>filter=\"alert_status = ERROR and isFavorite and coverage >= 60 and coverage < 80\"</code></li>" +
        " <li>to filter projects with a reliability, security and maintainability rating equals or worse than B:<br>" +
        "   <code>filter=\"reliability_rating>=2 and security_rating>=2 and sqale_rating>=2\"</code></li>" +
        "</ul>" +
        "To filter on any numeric metric, provide the metric key.<br>" +
        "Use the WS api/metrics/search to find the key of a metric.<br>" +
        "<br>" +
        "To filter on a rating, provide the corresponding metric key (ex: reliability_rating for reliability rating).<br>" +
        "The possible values are:" +
        "<ul>" +
        " <li>'1' for rating A</li>" +
        " <li>'2' for rating B</li>" +
        " <li>'3' for rating C</li>" +
        " <li>'4' for rating D</li>" +
        " <li>'5' for rating E</li>" +
        "</ul>" +
        "To filter on a Quality Gate status use the metric key 'alert_status'. Only the '=' operator can be used.<br>" +
        "The possible values are:" +
        "<ul>" +
        " <li>'OK' for Passed</li>" +
        " <li>'WARN' for Warning</li>" +
        " <li>'ERROR' for Failed</li>" +
        "</ul>");
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    SearchProjectsWsResponse response = doHandle(toRequest(httpRequest));

    writeProtobuf(response, httpRequest, httpResponse);
  }

  private SearchProjectsWsResponse doHandle(SearchProjectsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchResults searchResults = searchData(dbSession, request);

      return buildResponse(request, searchResults);
    }
  }

  private SearchResults searchData(DbSession dbSession, SearchProjectsRequest request) {
    String filter = firstNonNull(request.getFilter(), "");

    Set<String> favoriteProjectUuids = searchFavoriteProjects(dbSession);

    ProjectMeasuresQuery query = newProjectMeasuresQuery(filter, favoriteProjectUuids);
    queryValidator.validate(dbSession, query);

    SearchIdResult<String> esResults = index.search(query, new SearchOptions()
      .addFacets(request.getFacets())
      .setPage(request.getPage(), request.getPageSize()));

    Ordering<ComponentDto> ordering = Ordering.explicit(esResults.getIds()).onResultOf(ComponentDto::uuid);
    List<ComponentDto> projects = ordering.immutableSortedCopy(dbClient.componentDao().selectByUuids(dbSession, esResults.getIds()));

    return new SearchResults(projects, favoriteProjectUuids, esResults);
  }

  private Set<String> searchFavoriteProjects(DbSession dbSession) {
    List<Long> favoriteDbIds = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserId(userSession.getUserId())
      .setKey("favourite")
      .build(), dbSession)
      .stream()
      .map(PropertyDto::getResourceId)
      .collect(Collectors.toList());

    return dbClient.componentDao().selectByIds(dbSession, favoriteDbIds)
      .stream()
      .filter(dbComponent -> Qualifiers.PROJECT.equals(dbComponent.qualifier()))
      .map(ComponentDto::uuid)
      .collect(Collectors.toSet());
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

  private SearchProjectsWsResponse buildResponse(SearchProjectsRequest request, SearchResults searchResults) {
    Function<ComponentDto, Component> dbToWsComponent = new DbToWsComponent(searchResults.favoriteProjectUuids, userSession.isLoggedIn());

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
        wsFacet.addAllValues(Collections.emptyList());
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
    private final Set<String> favoriteProjectUuids;
    private final boolean isUserLoggedIn;

    private DbToWsComponent(Set<String> favoriteProjectUuids, boolean isUserLoggedIn) {
      this.isUserLoggedIn = isUserLoggedIn;
      this.wsComponent = Component.newBuilder();
      this.favoriteProjectUuids = favoriteProjectUuids;
    }

    @Override
    public Component apply(ComponentDto dbComponent) {
      wsComponent
        .clear()
        .setId(dbComponent.uuid())
        .setKey(dbComponent.key())
        .setName(dbComponent.name());

      if (isUserLoggedIn) {
        wsComponent.setIsFavorite(favoriteProjectUuids.contains(dbComponent.uuid()));
      }

      return wsComponent.build();
    }
  }

  private static class SearchResults {
    private final List<ComponentDto> projects;
    private final Set<String> favoriteProjectUuids;
    private final Facets facets;
    private final int total;

    private SearchResults(List<ComponentDto> projects, Set<String> favoriteProjectUuids, SearchIdResult<String> searchResults) {
      this.projects = projects;
      this.favoriteProjectUuids = favoriteProjectUuids;
      this.total = (int) searchResults.getTotal();
      this.facets = searchResults.getFacets();
    }
  }
}
