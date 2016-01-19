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
package org.sonarqube.ws.client.usertoken;

import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;
import org.sonarqube.ws.WsUserTokens.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.ACTION_GENERATE;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.ACTION_REVOKE;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.CONTROLLER;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_NAME;

public class UserTokensService extends BaseService {

  public UserTokensService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER);
  }

  public GenerateWsResponse generate(GenerateWsRequest request) {
    return call(
      new PostRequest(path(ACTION_GENERATE))
        .setParam(PARAM_LOGIN, request.getLogin())
        .setParam(PARAM_NAME, request.getName()),
      GenerateWsResponse.parser());
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path(ACTION_SEARCH)).setParam(PARAM_LOGIN, request.getLogin()),
      SearchWsResponse.parser());
  }

  public void revoke(RevokeWsRequest request) {
    call(
      new PostRequest(path(ACTION_REVOKE))
        .setParam(PARAM_LOGIN, request.getLogin())
        .setParam(PARAM_NAME, request.getName()));
  }

}
