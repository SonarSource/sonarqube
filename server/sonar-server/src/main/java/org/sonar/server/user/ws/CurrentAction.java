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

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.ws.AvatarResolver;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Users.CurrentWsResponse;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Users.CurrentWsResponse.Permissions;
import static org.sonarqube.ws.Users.CurrentWsResponse.newBuilder;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_PROJECTS;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CURRENT;

public class CurrentAction implements UsersWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final AvatarResolver avatarResolver;

  public CurrentAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    AvatarResolver avatarResolver) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.avatarResolver = avatarResolver;
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
      .setHomepage(findHomepageFor(dbSession, user))
      .setShowOnboardingTutorial(!user.isOnboarded());
    setNullable(emptyToNull(user.getEmail()), builder::setEmail);
    setNullable(emptyToNull(user.getEmail()), u -> builder.setAvatar(avatarResolver.create(user)));
    setNullable(user.getExternalIdentity(), builder::setExternalIdentity);
    setNullable(user.getExternalIdentityProvider(), builder::setExternalProvider);
    return builder.build();
  }

  private List<String> getGlobalPermissions() {
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    return OrganizationPermission.all()
      .filter(permission -> userSession.hasPermission(permission, defaultOrganizationUuid))
      .map(OrganizationPermission::getKey)
      .collect(toList());
  }

  private CurrentWsResponse.Homepage findHomepageFor(DbSession dbSession, UserDto user) {
    if (user.getHomepageType() == null) {
      return defaultHomepageOf();
    }
    String homepageParameter = getHomepageParameter(dbSession, user.getHomepageType(), user.getHomepageParameter());
    CurrentWsResponse.Homepage.Builder homepage = CurrentWsResponse.Homepage.newBuilder()
      .setType(CurrentWsResponse.HomepageType.valueOf(user.getHomepageType()));
    setNullable(homepageParameter, homepage::setParameter);
    return homepage.build();
  }

  @CheckForNull
  private String getHomepageParameter(DbSession dbSession, String homepageType, String homepageParameter) {
    if (PROJECT.toString().equals(homepageType)) {
      return dbClient.componentDao().selectByUuid(dbSession, homepageParameter)
        .transform(ComponentDto::getKey)
        .or(() -> {
          throw new IllegalStateException(format("Unknown component '%s' for homepageParameter", homepageParameter));
        });
    }
    if (ORGANIZATION.toString().equals(homepageType)) {
      return dbClient.organizationDao().selectByUuid(dbSession, homepageParameter)
        .map(OrganizationDto::getKey)
        .orElseThrow(() -> new IllegalStateException(format("Unknown organization '%s' for homepageParameter", homepageParameter)));
    }
    return null;
  }

  private static CurrentWsResponse.Homepage defaultHomepageOf() {
    return CurrentWsResponse.Homepage.newBuilder()
      .setType(MY_PROJECTS)
      .build();
  }

}
