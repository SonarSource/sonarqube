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

package org.sonar.server.favorite.ws;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.favorite.FavoriteFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Favorites.Favorite;
import org.sonarqube.ws.Favorites.SearchResponse;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.Collectors.toOneElement;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.ACTION_SEARCH;

public class SearchAction implements FavoritesWsAction {
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

    action.addPagingParams(100, 500);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchResponse wsResponse = Stream.of(request)
      .map(search())
      .map(new ResponseBuilder())
      .collect(Collectors.toOneElement());
    writeProtobuf(wsResponse, request, response);
  }

  private Function<Request, SearchResults> search() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        return Stream.of(request)
          .peek(checkAuthentication(userSession))
          .map(SearchResults.builder(dbSession))
          .peek(addAuthorizedProjectUuids())
          .peek(addFavorites())
          .map(SearchResults.Builder::build)
          .collect(Collectors.toOneElement());
      }
    };
  }

  private Consumer<SearchResults.Builder> addFavorites() {
    return results -> results.allFavorites = favoriteFinder.list();
  }

  private Consumer<SearchResults.Builder> addAuthorizedProjectUuids() {
    return results -> results.authorizedProjectUuids = ImmutableSet
      .copyOf(dbClient.authorizationDao().selectAuthorizedRootProjectsUuids(results.dbSession, userSession.getUserId(), UserRole.USER));
  }

  private static Consumer<Request> checkAuthentication(UserSession userSession) {
    return r -> userSession.checkLoggedIn();
  }

  private static class SearchResults {
    private final List<ComponentDto> favorites;
    private final Paging paging;

    private SearchResults(Builder builder) {
      Predicate<ComponentDto> authorizedProjects = c -> builder.authorizedProjectUuids.contains(c.projectUuid());
      int total = (int) builder.allFavorites.stream().filter(authorizedProjects).count();
      this.paging = Paging.forPageIndex(builder.page).withPageSize(builder.pageSize).andTotal(total);
      this.favorites = builder.allFavorites.stream()
        .filter(authorizedProjects)
        .skip(paging.offset())
        .limit(paging.pageSize())
        .collect(Collectors.toList());
    }

    static Function<Request, Builder> builder(DbSession dbSession) {
      return request -> new Builder(dbSession, request);
    }

    private static class Builder {
      private final DbSession dbSession;
      private final int page;
      private final int pageSize;
      private Set<String> authorizedProjectUuids;
      private List<ComponentDto> allFavorites;

      private Builder(DbSession dbSession, Request request) {
        this.dbSession = dbSession;
        this.page = request.mandatoryParamAsInt(Param.PAGE);
        this.pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
      }

      public SearchResults build() {
        return new SearchResults(this);
      }
    }
  }

  private static class ResponseBuilder implements Function<SearchResults, SearchResponse> {
    private final SearchResponse.Builder response;
    private final Favorite.Builder favorite;

    private ResponseBuilder() {
      this.response = SearchResponse.newBuilder();
      this.favorite = Favorite.newBuilder();
    }

    @Override
    public SearchResponse apply(SearchResults searchResults) {
      return Stream.of(searchResults)
        .peek(addPaging())
        .peek(addFavorites())
        .map(results -> response.build())
        .collect(toOneElement());
    }

    private Consumer<SearchResults> addPaging() {
      return results -> response.setPaging(Common.Paging.newBuilder()
        .setPageIndex(results.paging.pageIndex())
        .setPageSize(results.paging.pageSize())
        .setTotal(results.paging.total()));
    }

    private Consumer<SearchResults> addFavorites() {
      return results -> results.favorites.stream()
        .map(toWsFavorite())
        .forEach(response::addFavorites);
    }

    private Function<ComponentDto, Favorite> toWsFavorite() {
      return componentDto -> {
        favorite
          .clear()
          .setKey(componentDto.key());
        setNullable(componentDto.name(), favorite::setName);
        setNullable(componentDto.qualifier(), favorite::setQualifier);
        return favorite.build();
      };
    }

  }
}
