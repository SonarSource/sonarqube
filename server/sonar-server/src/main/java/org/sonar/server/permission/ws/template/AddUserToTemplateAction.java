/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws.template;

import com.google.common.base.Predicate;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsTemplateRef;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_USER_LOGIN;
import static org.sonar.server.permission.ws.WsPermissionParameters.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.WsPermissionParameters.createUserLoginParameter;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;

public class AddUserToTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionDependenciesFinder dependenciesFinder;
  private final UserSession userSession;

  public AddUserToTemplateAction(DbClient dbClient, PermissionDependenciesFinder dependenciesFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.dependenciesFinder = dependenciesFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("add_user_to_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Add a user to a permission template.<br /> " +
        "It requires administration permissions to access.")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
    createUserLoginParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);

    String permission = wsRequest.mandatoryParam(PARAM_PERMISSION);
    final String userLogin = wsRequest.mandatoryParam(PARAM_USER_LOGIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      validateProjectPermission(permission);
      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, WsTemplateRef.fromRequest(wsRequest));
      UserDto user = dependenciesFinder.getUser(dbSession, userLogin);

      if (!isUserAlreadyAdded(dbSession, template.getId(), userLogin, permission)) {
        dbClient.permissionTemplateDao().insertUserPermission(dbSession, template.getId(), user.getId(), permission);
      }
    } finally {
      dbClient.closeSession(dbSession);
    }

    wsResponse.noContent();
  }

  private boolean isUserAlreadyAdded(DbSession dbSession, long templateId, String userLogin, String permission) {
    PermissionQuery permissionQuery = PermissionQuery.builder().permission(permission).membership(IN).build();
    List<UserWithPermissionDto> usersWithPermission = dbClient.permissionTemplateDao().selectUsers(dbSession, permissionQuery, templateId, 0, Integer.MAX_VALUE);
    return from(usersWithPermission).anyMatch(new HasUserPredicate(userLogin));
  }

  private static class HasUserPredicate implements Predicate<UserWithPermissionDto> {
    private final String userLogin;

    public HasUserPredicate(String userLogin) {
      this.userLogin = userLogin;
    }

    @Override
    public boolean apply(UserWithPermissionDto userWithPermission) {
      return userLogin.equals(userWithPermission.getLogin());
    }
  }
}
