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
package org.sonar.server.user.ws;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsUsers.CurrentWsResponse;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsUsers.CurrentWsResponse.Permissions;
import static org.sonarqube.ws.WsUsers.CurrentWsResponse.newBuilder;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CURRENT;

public class CurrentAction implements UsersWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public CurrentAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(NewController context) {
    context.createAction(ACTION_CURRENT)
      .setDescription("Get the details of the current authenticated user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("current-example.json"))
      .setSince("5.2")
      .setChangelog(new Change("6.5", "showOnboardingTutorial is now returned in the response"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (userSession.isLoggedIn()) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        writeProtobuf(toWsResponse(dbSession, userSession.getLogin()), request, response);
      }
    } else {
      writeProtobuf(newBuilder()
        .setIsLoggedIn(false)
        .setPermissions(Permissions.newBuilder().addAllGlobal(getGlobalPermissions()).build())
        .build(),
        request, response);
    }
  }

  private CurrentWsResponse toWsResponse(DbSession dbSession, String userLogin) {
    UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
    checkState(user != null, "User login '%s' cannot be found", userLogin);
    Collection<String> groups = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userLogin)).get(userLogin);

    CurrentWsResponse.Builder builder = newBuilder()
      .setIsLoggedIn(true)
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setLocal(user.isLocal())
      .addAllGroups(groups)
      .addAllScmAccounts(user.getScmAccountsAsList())
      .setPermissions(Permissions.newBuilder().addAllGlobal(getGlobalPermissions()).build())
      .setShowOnboardingTutorial(!user.isOnboarded());
    setNullable(emptyToNull(user.getEmail()), builder::setEmail);
    setNullable(user.getExternalIdentity(), builder::setExternalIdentity);
    setNullable(user.getExternalIdentityProvider(), builder::setExternalProvider);
    return builder.build();
  }

  private List<String> getGlobalPermissions() {
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    return OrganizationPermission.all()
      .filter(permission -> userSession.hasPermission(permission, defaultOrganizationUuid))
      .map(OrganizationPermission::getKey)
      .collect(Collectors.toList());
  }
}
