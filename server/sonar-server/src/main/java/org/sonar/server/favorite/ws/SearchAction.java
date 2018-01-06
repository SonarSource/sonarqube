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
package org.sonar.server.favorite.ws;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Favorites.Favorite;
import org.sonarqube.ws.Favorites.SearchResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonar.server.favorite.ws.FavoritesWsParameters.ACTION_SEARCH;

public class SearchAction implements FavoritesWsAction {
  private static final int MAX_PAGE_SIZE = 500;

  private final FavoriteFinder favoriteFinder;
  private final DbClient dbClient;
  private final UserSession userSession;

  public SearchAction(FavoriteFinder favoriteFinder, DbClient dbClient, UserSession userSession) {
    this.favoriteFinder = favoriteFinder;
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setDescription("Search for the authenticated user favorites.<br>" +
        "Requires authentication.")
      .setSince("6.3")
      .setResponseExample(getClass().getResource("search-example.json"))
      .setHandler(this);

    action.addPagingParams(100, MAX_PAGE_SIZE);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchRequest searchRequest = toWsRequest(request);
    SearchResults searchResults = toSearchResults(searchRequest);
    SearchResponse wsResponse = toSearchResponse(searchResults);
    writeProtobuf(wsResponse, request, response);
  }

  private static SearchRequest toWsRequest(Request request) {
    return new SearchRequest()
      .setP(request.mandatoryParam(Param.PAGE))
      .setPs(request.mandatoryParam(Param.PAGE_SIZE));
  }

  private SearchResults toSearchResults(SearchRequest request) {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<ComponentDto> authorizedFavorites = getAuthorizedFavorites();
      Paging paging = Paging.forPageIndex(Integer.parseInt(request.getP())).withPageSize(Integer.parseInt(request.getPs())).andTotal(authorizedFavorites.size());
      List<ComponentDto> displayedFavorites = authorizedFavorites.stream()
        .skip(paging.offset())
        .limit(paging.pageSize())
        .collect(MoreCollectors.toList());
      Map<String, OrganizationDto> organizationsByUuid = getOrganizationsOfComponents(dbSession, displayedFavorites);
      return new SearchResults(paging, displayedFavorites, organizationsByUuid);
    }
  }

  private List<ComponentDto> getAuthorizedFavorites() {
    List<ComponentDto> componentDtos = favoriteFinder.list();
    return userSession.keepAuthorizedComponents(UserRole.USER, componentDtos);
  }

  private Map<String, OrganizationDto> getOrganizationsOfComponents(DbSession dbSession, List<ComponentDto> displayedFavorites) {
    Set<String> organizationUuids = displayedFavorites.stream()
      .map(ComponentDto::getOrganizationUuid)
      .collect(MoreCollectors.toSet());
    return dbClient.organizationDao().selectByUuids(dbSession, organizationUuids)
      .stream()
      .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid));
  }

  private static class SearchResults {
    private final List<ComponentDto> favorites;
    private final Paging paging;
    private final Map<String, OrganizationDto> organizationsByUuid;

    private SearchResults(Paging paging, List<ComponentDto> favorites, Map<String, OrganizationDto> organizationsByUuid) {
      this.paging = paging;
      this.favorites = favorites;
      this.organizationsByUuid = organizationsByUuid;
    }
  }

  private static SearchResponse toSearchResponse(SearchResults searchResults) {
    SearchResponse.Builder builder = SearchResponse.newBuilder();
    addPaging(builder, searchResults);
    addFavorites(builder, searchResults);
    return builder.build();
  }

  private static void addPaging(SearchResponse.Builder builder, SearchResults results) {
    builder
      .setPaging(Common.Paging.newBuilder()
        .setPageIndex(results.paging.pageIndex())
        .setPageSize(results.paging.pageSize())
        .setTotal(results.paging.total()));
  }

  private static void addFavorites(SearchResponse.Builder builder, SearchResults results) {
    Favorite.Builder favoriteBuilder = Favorite.newBuilder();
    results.favorites.stream()
      .map(componentDto -> toWsFavorite(favoriteBuilder, results, componentDto))
      .forEach(builder::addFavorites);
  }

  private static Favorite toWsFavorite(Favorite.Builder builder, SearchResults results, ComponentDto componentDto) {
    OrganizationDto organization = results.organizationsByUuid.get(componentDto.getOrganizationUuid());
    checkArgument(organization != null,
      "Organization with uuid '%s' not found for favorite with uuid '%s'",
      componentDto.getOrganizationUuid(), componentDto.uuid());
    builder
      .clear()
      .setOrganization(organization.getKey())
      .setKey(componentDto.getDbKey());
    setNullable(componentDto.name(), builder::setName);
    setNullable(componentDto.qualifier(), builder::setQualifier);
    return builder.build();
  }

  private static class SearchRequest {

    private String p;
    private String ps;

    public SearchRequest setP(String p) {
      this.p = p;
      return this;
    }

    public String getP() {
      return p;
    }

    public SearchRequest setPs(String ps) {
      this.ps = ps;
      return this;
    }

    public String getPs() {
      return ps;
    }
  }
}
