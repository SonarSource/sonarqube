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
package org.sonarqube.ws.client.user;

import java.util.List;
import org.sonarqube.ws.WsUsers.CreateWsResponse;
import org.sonarqube.ws.WsUsers.CurrentWsResponse;
import org.sonarqube.ws.WsUsers.GroupsWsResponse;
import org.sonarqube.ws.WsUsers.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;

import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CURRENT;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_DEACTIVATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_GROUPS;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_SKIP_ONBOARDING_TUTORIAL;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_UPDATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.CONTROLLER_USERS;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOCAL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SELECTED;

public class UsersService extends BaseService {

  public UsersService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_USERS);
  }

  public SearchWsResponse search(SearchRequest request) {
    List<String> additionalFields = request.getPossibleFields();
    return call(new GetRequest(path(ACTION_SEARCH))
      .setParam(PAGE, request.getPage())
      .setParam(PAGE_SIZE, request.getPageSize())
      .setParam(TEXT_QUERY, request.getQuery())
      .setParam(FIELDS, !additionalFields.isEmpty() ? inlineMultipleParamValue(additionalFields) : null),
      SearchWsResponse.parser());
  }

  public CreateWsResponse create(CreateRequest request) {
    return call(new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_LOGIN, request.getLogin())
      .setParam(PARAM_PASSWORD, request.getPassword())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_EMAIL, request.getEmail())
      .setParam(PARAM_SCM_ACCOUNT, request.getScmAccounts())
      .setParam(PARAM_LOCAL, request.isLocal()),
      CreateWsResponse.parser());
  }

  public void update(UpdateRequest request) {
    call(new PostRequest(path(ACTION_UPDATE))
      .setParam(PARAM_LOGIN, request.getLogin())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_EMAIL, request.getEmail())
      .setParam(PARAM_SCM_ACCOUNT, request.getScmAccounts()));
  }

  public GroupsWsResponse groups(GroupsRequest request) {
    return call(new GetRequest(path(ACTION_GROUPS))
      .setParam(PARAM_LOGIN, request.getLogin())
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_SELECTED, request.getSelected())
      .setParam(TEXT_QUERY, request.getQuery())
      .setParam(PAGE, request.getPage())
      .setParam(PAGE_SIZE, request.getPageSize()),
      GroupsWsResponse.parser());
  }

  public CurrentWsResponse current() {
    return call(new GetRequest(path(ACTION_CURRENT)), CurrentWsResponse.parser());
  }

  public WsResponse skipOnboardingTutorial() {
    return call(new PostRequest(path(ACTION_SKIP_ONBOARDING_TUTORIAL)));
  }

  public void deactivate(String login) {
    call(new PostRequest(path(ACTION_DEACTIVATE))
      .setParam(PARAM_LOGIN, login));
  }

}
