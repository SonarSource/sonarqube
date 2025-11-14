/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Favorites.Favorite;
import org.sonarqube.ws.Favorites.SearchResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.server.favorite.ws.FavoritesWsParameters.ACTION_SEARCH;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements FavoritesWsAction {
  private static final int MAX_PAGE_SIZE = 500;

  private final FavoriteFinder favoriteFinder;
  private final UserSession userSession;

  public SearchAction(FavoriteFinder favoriteFinder, UserSession userSession) {
    this.favoriteFinder = favoriteFinder;
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

    List<EntityDto> authorizedFavorites = getAuthorizedFavorites();
    Paging paging = Paging.forPageIndex(Integer.parseInt(request.getP())).withPageSize(Integer.parseInt(request.getPs())).andTotal(authorizedFavorites.size());
    List<EntityDto> displayedFavorites = authorizedFavorites.stream()
      .skip(paging.offset())
      .limit(paging.pageSize())
      .toList();
    return new SearchResults(paging, displayedFavorites);
  }

  private List<EntityDto> getAuthorizedFavorites() {
    List<EntityDto> entities = favoriteFinder.list();
    return userSession.keepAuthorizedEntities(ProjectPermission.USER, entities);
  }

  private static class SearchResults {
    private final List<EntityDto> favorites;
    private final Paging paging;

    private SearchResults(Paging paging, List<EntityDto> favorites) {
      this.paging = paging;
      this.favorites = favorites;
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
      .map(componentDto -> toWsFavorite(favoriteBuilder, componentDto))
      .forEach(builder::addFavorites);
  }

  private static Favorite toWsFavorite(Favorite.Builder builder, EntityDto entity) {
    builder
      .clear()
      .setKey(entity.getKey());
    ofNullable(entity.getName()).ifPresent(builder::setName);
    ofNullable(entity.getQualifier()).ifPresent(builder::setQualifier);
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
