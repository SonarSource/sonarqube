/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.user.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_ISSUES;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_PROJECTS;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;

public class SetHomepageAction implements UsersWsAction {

  static final String PARAM_TYPE = "type";
  static final String PARAM_PARAMETER = "parameter";
  static final String ACTION = "set_homepage";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public SetHomepageAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setPost(true)
      .setInternal(true)
      .setDescription("Set homepage of current user.<br> Requires authentication.")
      .setSince("7.0")
      .setHandler(this);

    action.createParam(PARAM_TYPE)
      .setDescription("Type of the requested page")
      .setRequired(true)
      .setPossibleValues(HomepageTypes.keys());

    action.createParam(PARAM_PARAMETER)
      .setDescription("Additional information to identify the page (project or organization key)")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String type = request.mandatoryParam(PARAM_TYPE);
    String parameter = request.param(PARAM_PARAMETER);

    checkRequest(type, parameter);

    String login = userSession.getLogin();

    try (DbSession dbSession = dbClient.openSession(false)) {

      UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      checkState(user != null, "User login '%s' cannot be found", login);

      user.setHomepageType(type);
      user.setHomepageParameter(findHomepageParameter(dbSession, type, parameter));

      dbClient.userDao().update(dbSession, user);
      dbSession.commit();
    }

    response.noContent();
  }

  @CheckForNull
  private String findHomepageParameter(DbSession dbSession, String type, String parameter) {

    if (PROJECT.toString().equals(type)) {
      return componentFinder.getByKey(dbSession, parameter).uuid();
    }

    if (ORGANIZATION.toString().equals(type)) {
      return checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, parameter), "No organizationDto with key '%s'", parameter).getUuid();
    }

    return null;
  }

  private static void checkRequest(String type, @Nullable String parameter) {

    if (PROJECT.toString().equals(type) || ORGANIZATION.toString().equals(type)) {
      checkArgument(isNotBlank(parameter), "Type %s requires a parameter", type);
    }

    if (MY_PROJECTS.toString().equals(type) || MY_ISSUES.toString().equals(type)) {
      checkArgument(isBlank(parameter), "Parameter parameter must not be provided when type is %s", type);
    }

  }
}
