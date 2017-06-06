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
import javax.annotation.Nullable;
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

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

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
    context.createAction("current")
      .setDescription("Get the details of the current authenticated user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("current-example.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserDto user;
    boolean showOnboarding;
    Collection<String> groups;
    if (userSession.isLoggedIn()) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        user = selectCurrentUser(dbSession);
        showOnboarding = !dbClient.userDao().selectOnboarded(dbSession, user);
        groups = selectGroups(dbSession);
      }
    } else {
      user = null;
      showOnboarding = false;
      groups = emptyList();
    }
    writeProtobuf(toWsResponse(user, showOnboarding, groups), request, response);
  }

  private UserDto selectCurrentUser(DbSession dbSession) {
    return dbClient.userDao().selectActiveUserByLogin(dbSession, userSession.getLogin());
  }

  private Collection<String> selectGroups(DbSession dbSession) {
    return dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userSession.getLogin()))
      .get(userSession.getLogin());
  }

  private CurrentWsResponse toWsResponse(@Nullable UserDto user, boolean showOnboarding, Collection<String> groups) {
    CurrentWsResponse.Builder builder = CurrentWsResponse.newBuilder();
    builder.setIsLoggedIn(userSession.isLoggedIn());
    setNullable(userSession.getLogin(), builder::setLogin);
    setNullable(userSession.getName(), builder::setName);
    if (user != null) {
      setNullable(emptyToNull(user.getEmail()), builder::setEmail);
      builder.setLocal(user.isLocal());
      setNullable(user.getExternalIdentity(), builder::setExternalIdentity);
      setNullable(user.getExternalIdentityProvider(), builder::setExternalProvider);
      builder.addAllScmAccounts(user.getScmAccountsAsList());
    }
    builder.setShowOnboardingTutorial(showOnboarding);
    builder.addAllGroups(groups);
    builder.setPermissions(CurrentWsResponse.Permissions.newBuilder().addAllGlobal(getGlobalPermissions()).build());
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
