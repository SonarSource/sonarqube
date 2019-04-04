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
package org.sonarqube.ws.client.users;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Users.CreateWsResponse;
import org.sonarqube.ws.Users.CurrentWsResponse;
import org.sonarqube.ws.Users.GroupsWsResponse;
import org.sonarqube.ws.Users.IdentityProvidersWsResponse;
import org.sonarqube.ws.Users.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class UsersService extends BaseService {

  public UsersService(WsConnector wsConnector) {
    super(wsConnector, "api/users");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/change_password">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void changePassword(ChangePasswordRequest request) {
    call(
      new PostRequest(path("change_password"))
        .setParam("login", request.getLogin())
        .setParam("password", request.getPassword())
        .setParam("previousPassword", request.getPreviousPassword())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/create">Further information about this action online (including a response example)</a>
   * @since 3.7
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("email", request.getEmail())
        .setParam("local", request.getLocal())
        .setParam("login", request.getLogin())
        .setParam("name", request.getName())
        .setParam("password", request.getPassword())
        .setParam("scmAccount", request.getScmAccount())
        .setParam("scmAccounts", request.getScmAccounts() == null ? null : request.getScmAccounts().stream().collect(Collectors.joining(","))),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/current">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public CurrentWsResponse current() {
    return call(
      new GetRequest(path("current")),
      CurrentWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/deactivate">Further information about this action online (including a response example)</a>
   * @since 3.7
   */
  public String deactivate(DeactivateRequest request) {
    return call(
      new PostRequest(path("deactivate"))
        .setParam("login", request.getLogin())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/groups">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public GroupsWsResponse groups(GroupsRequest request) {
    return call(
      new GetRequest(path("groups"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("selected", request.getSelected()),
      GroupsWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/identity_providers">Further information about this action online (including a response example)</a>
   * @since 5.5
   */
  public IdentityProvidersWsResponse identityProviders() {
    return call(
      new GetRequest(path("identity_providers")),
      IdentityProvidersWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/search">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/set_homepage">Further information about this action online (including a response example)</a>
   * @since 7.0
   */
  public void setHomepage(SetHomepageRequest request) {
    call(
      new PostRequest(path("set_homepage"))
        .setParam("parameter", request.getParameter())
        .setParam("type", request.getType())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/set_setting">Further information about this action online (including a response example)</a>
   * @since 7.6
   */
  public void setSetting(SetSettingRequest request) {
    call(
      new PostRequest(path("set_setting"))
        .setParam("key", request.getKey())
        .setParam("value", request.getValue())
        .setMediaType(MediaTypes.JSON)
    ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/skip_onboarding_tutorial">Further information about this action online (including a response example)</a>
   * @since 6.5
   */
  public void skipOnboardingTutorial() {
    call(
      new PostRequest(path("skip_onboarding_tutorial"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/update">Further information about this action online (including a response example)</a>
   * @since 3.7
   */
  public String update(UpdateRequest request) {
    return call(
      new PostRequest(path("update"))
        .setParam("email", request.getEmail())
        .setParam("login", request.getLogin())
        .setParam("name", request.getName())
        .setParam("scmAccount", request.getScmAccount())
        .setParam("scmAccounts", request.getScmAccounts())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/update_login">Further information about this action online (including a response example)</a>
   * @since 7.6
   */
  public void updateLogin(UpdateLoginRequest request) {
    call(
      new PostRequest(path("update_login"))
        .setParam("login", request.getLogin())
        .setParam("newLogin", request.getNewLogin())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
