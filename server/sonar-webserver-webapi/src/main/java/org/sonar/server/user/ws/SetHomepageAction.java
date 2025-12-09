/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.server.user.ws.HomepageTypes.Type.PROJECT;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SetHomepageAction implements UsersWsAction {

  private static final String ACTION = "set_homepage";

  public static final String PARAM_TYPE = "type";
  public static final String PARAM_COMPONENT = "component";
  public static final String PARAM_BRANCH = "branch";
  private static final String PARAMETER_REQUIRED = "Type %s requires a parameter '%s'";

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
      .setDescription("Set homepage of current user.<br> " +
        "Requires authentication.")
      .setSince("7.0")
      .setChangelog(
        new Change("8.3", "Types: MY_PROJECTS, MY_ISSUES, ORGANIZATION removed"),
        new Change("8.3", "Parameter 'organization' removed"),
        new Change("7.1", "Parameter 'parameter' is replaced by 'component' and 'organization'"))
      .setHandler(this);

    action.createParam(PARAM_TYPE)
      .setDescription("Type of the requested page")
      .setRequired(true)
      .setPossibleValues(HomepageTypes.Type.values());

    action.createParam(PARAM_COMPONENT)
      .setSince("7.1")
      .setDescription("Project key. It should only be used when parameter '%s' is set to '%s'", PARAM_TYPE, PROJECT)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. It can only be used when parameter '%s' is set to '%s'", PARAM_TYPE, PROJECT)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setSince("7.1");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    HomepageTypes.Type type = request.mandatoryParamAsEnum(PARAM_TYPE, HomepageTypes.Type.class);
    String componentParameter = request.param(PARAM_COMPONENT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      String parameter = getHomepageParameter(dbSession, type, componentParameter, request.param(PARAM_BRANCH));

      UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, userSession.getLogin());
      checkState(user != null, "User login '%s' cannot be found", userSession.getLogin());

      user.setHomepageType(type.name());
      user.setHomepageParameter(parameter);
      dbClient.userDao().update(dbSession, user);
      dbSession.commit();
    }

    response.noContent();
  }

  @CheckForNull
  private String getHomepageParameter(DbSession dbSession, HomepageTypes.Type type, @Nullable String componentParameter, @Nullable String branchParameter) {
    switch (type) {
      case PROJECT:
        checkArgument(isNotBlank(componentParameter), PARAMETER_REQUIRED, type.name(), PARAM_COMPONENT);
        ProjectDto projectDto = componentFinder.getProjectByKey(dbSession, componentParameter);
        return componentFinder.getBranchOrPullRequest(dbSession, projectDto, branchParameter, null).getUuid();
      case PORTFOLIO, APPLICATION:
        checkArgument(isNotBlank(componentParameter), PARAMETER_REQUIRED, type.name(), PARAM_COMPONENT);
        return componentFinder.getByKey(dbSession, componentParameter).uuid();
      case PORTFOLIOS, PROJECTS, ISSUES:
        return null;
      default:
        throw new IllegalArgumentException(format("Unknown type '%s'", type.name()));
    }
  }

}
