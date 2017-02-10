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
package org.sonarqube.ws.client.favorite;

import org.sonar.api.server.ws.WebService.Param;
import org.sonarqube.ws.Favorites.SearchResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.ACTION_ADD;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.ACTION_REMOVE;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.CONTROLLER_FAVORITES;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.PARAM_COMPONENT;

public class FavoritesService extends BaseService {
  public FavoritesService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_FAVORITES);
  }

  public void add(String component) {
    PostRequest post = new PostRequest(path(ACTION_ADD)).setParam(PARAM_COMPONENT, component);

    call(post);
  }

  public void remove(String component) {
    PostRequest post = new PostRequest(path(ACTION_REMOVE)).setParam(PARAM_COMPONENT, component);

    call(post);
  }

  public SearchResponse search(SearchRequest request) {
    GetRequest get = new GetRequest(path(ACTION_SEARCH));
    if (request.getPage() != null) {
      get.setParam(Param.PAGE, request.getPage());
    }
    if (request.getPageSize() != null) {
      get.setParam(Param.PAGE_SIZE, request.getPageSize());
    }

    return call(get, SearchResponse.parser());
  }
}
