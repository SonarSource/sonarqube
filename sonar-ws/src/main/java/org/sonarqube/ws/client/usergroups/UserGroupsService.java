/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client.usergroups;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.UserGroups.CreateWsResponse;
import org.sonarqube.ws.UserGroups.SearchWsResponse;
import org.sonarqube.ws.UserGroups.UpdateWsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class UserGroupsService extends BaseService {

  public UserGroupsService(WsConnector wsConnector) {
    super(wsConnector, "api/user_groups");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/add_user">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void addUser(AddUserRequest request) {
    call(
      new PostRequest(path("add_user"))
        .setParam("id", request.getId())
        .setParam("login", request.getLogin())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/create">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("description", request.getDescription())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization()),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/delete">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("id", request.getId())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/remove_user">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void removeUser(RemoveUserRequest request) {
    call(
      new PostRequest(path("remove_user"))
        .setParam("id", request.getId())
        .setParam("login", request.getLogin())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/search">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/update">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("description", request.getDescription())
        .setParam("id", request.getId())
        .setParam("name", request.getName()),
      UpdateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/user_groups/users">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String users(UsersRequest request) {
    return call(
      new GetRequest(path("users"))
        .setParam("id", request.getId())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("selected", request.getSelected())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
