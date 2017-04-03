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
package org.sonarqube.ws.client.usergroup;

import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonarqube.ws.WsUserGroups.CreateWsResponse;
import static org.sonarqube.ws.WsUserGroups.SearchWsResponse;
import static org.sonarqube.ws.WsUserGroups.UpdateWsResponse;

public class UserGroupsService extends BaseService {

  public UserGroupsService(WsConnector wsConnector) {
    super(wsConnector, "api/user_groups");
  }

  public CreateWsResponse create(CreateWsRequest request) {
    return call(new PostRequest(path("create"))
      .setParam("name", request.getName())
      .setParam("description", request.getDescription())
      .setParam("organization", request.getOrganization()),
      CreateWsResponse.parser());
  }

  public UpdateWsResponse update(UpdateWsRequest request) {
    return call(new PostRequest(path("update"))
      .setParam("id", request.getId())
      .setParam("name", request.getName())
      .setParam("description", request.getDescription()),
      UpdateWsResponse.parser());
  }

  public void delete(DeleteWsRequest request) {
    call(new PostRequest(path("delete"))
      .setParam("id", request.getId())
      .setParam("name", request.getName())
      .setParam("organization", request.getOrganization()));
  }

  public void addUser(AddUserWsRequest request) {
    call(new PostRequest(path("add_user"))
      .setParam("id", request.getId())
      .setParam("name", request.getName())
      .setParam("login", request.getLogin())
      .setParam("organization", request.getOrganization()));
  }

  public void removeUser(RemoveUserWsRequest request) {
    call(new PostRequest(path("remove_user"))
      .setParam("id", request.getId())
      .setParam("name", request.getName())
      .setParam("login", request.getLogin())
      .setParam("organization", request.getOrganization()));
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(new GetRequest(path("search"))
      .setParam(TEXT_QUERY, request.getQuery())
      .setParam(PAGE, request.getPage())
      .setParam(PAGE_SIZE, request.getPageSize())
      .setParam("organization", request.getOrganization())
      .setParam(FIELDS, inlineMultipleParamValue(request.getFields())),
      SearchWsResponse.parser());
  }

}
