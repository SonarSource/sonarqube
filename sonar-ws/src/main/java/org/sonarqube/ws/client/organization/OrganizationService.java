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
package org.sonarqube.ws.client.organization;

import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.Organizations.CreateWsResponse;
import static org.sonarqube.ws.Organizations.UpdateWsResponse;

public class OrganizationService extends BaseService {

  public OrganizationService(WsConnector wsConnector) {
    super(wsConnector, "api/organizations");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    GetRequest get = new GetRequest(path("search"))
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize());

    return call(get, SearchWsResponse.parser());
  }

  public CreateWsResponse create(CreateWsRequest request) {
    PostRequest post = new PostRequest(path("create"))
      .setParam("name", request.getName())
      .setParam("key", request.getKey())
      .setParam("description", request.getDescription())
      .setParam("url", request.getUrl())
      .setParam("avatar", request.getAvatar());

    return call(post, CreateWsResponse.parser());
  }

  public UpdateWsResponse update(UpdateWsRequest request) {
    PostRequest post = new PostRequest(path("update"))
      .setParam("key", request.getKey())
      .setParam("name", request.getName())
      .setParam("description", request.getDescription())
      .setParam("url", request.getUrl())
      .setParam("avatar", request.getAvatar());

    return call(post, UpdateWsResponse.parser());
  }

  public void delete(@Nullable String key) {
    PostRequest post = new PostRequest(path("delete"))
      .setParam("key", key);

    call(post).failIfNotSuccessful();
  }
}
