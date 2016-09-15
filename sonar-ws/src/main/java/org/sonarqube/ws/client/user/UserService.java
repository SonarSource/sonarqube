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

package org.sonarqube.ws.client.user;

import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_UPDATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.CONTROLLER_USERS;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;

public class UserService extends BaseService {

  public UserService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_USERS);
  }

  public void create(CreateRequest request) {
    call(new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_LOGIN, request.getLogin())
      .setParam(PARAM_PASSWORD, request.getPassword())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_EMAIL, request.getEmail())
      .setParam(PARAM_SCM_ACCOUNT, request.getScmAccounts()));
  }

  public void update(UpdateRequest request) {
    call(new PostRequest(path(ACTION_UPDATE))
      .setParam(PARAM_LOGIN, request.getLogin())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_EMAIL, request.getEmail())
      .setParam(PARAM_SCM_ACCOUNT, request.getScmAccounts()));
  }

}
