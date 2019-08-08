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
package org.sonar.server.user.ws;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.AvatarResolver;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Users.CurrentWsResponse;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Users.CurrentWsResponse.Permissions;
import static org.sonarqube.ws.Users.CurrentWsResponse.newBuilder;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.APPLICATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PORTFOLIO;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_CURRENT;

public class CurrentAction implements UsersWsAction {

  private static final String GOVERNANCE_PLUGIN_KEY = "governance";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final AvatarResolver avatarResolver;
  private final HomepageTypes homepageTypes;
  private final PluginRepository pluginRepository;
  private final PermissionService permissionService;

  public CurrentAction(UserSession userSession, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    AvatarResolver avatarResolver, HomepageTypes homepageTypes, PluginRepository pluginRepository, PermissionService permissionService) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.avatarResolver = avatarResolver;
    this.homepageTypes = homepageTypes;
    this.pluginRepository = pluginRepository;
    this.permissionService = permissionService;
  }

  @Override
  public void define(NewController context) {
    context.createAction(ACTION_CURRENT)
      .setDescription("Get the details of the current authenticated user.")
      .setSince("5.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("current-example.json"))
      .setChangelog(
        new Change("6.5", "showOnboardingTutorial is now returned in the response"),
        new Change("7.1", "'parameter' is replaced by 'component' and 'organization' in the response"));
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
      .setHomepage(buildHomepage(dbSession, user))
      .setShowOnboardingTutorial(!user.isOnboarded())
      .addAllSettings(loadUserSettings(dbSession, user));
    ofNullable(emptyToNull(user.getEmail())).ifPresent(builder::setEmail);
    ofNullable(emptyToNull(user.getEmail())).ifPresent(u -> builder.setAvatar(avatarResolver.create(user)));
    ofNullable(user.getExternalLogin()).ifPresent(builder::setExternalIdentity);
    ofNullable(user.getExternalIdentityProvider()).ifPresent(builder::setExternalProvider);
    return builder.build();
  }

  private List<String> getGlobalPermissions() {
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    return permissionService.getAllOrganizationPermissions().stream()
      .filter(permission -> userSession.hasPermission(permission, defaultOrganizationUuid))
      .map(OrganizationPermission::getKey)
      .collect(toList());
  }

  private CurrentWsResponse.Homepage buildHomepage(DbSession dbSession, UserDto user) {
    if (noHomepageSet(user)) {
      return defaultHomepage();
    }

    return doBuildHomepage(dbSession, user).orElse(defaultHomepage());
  }

  private Optional<CurrentWsResponse.Homepage> doBuildHomepage(DbSession dbSession, UserDto user) {

    if (PROJECT.toString().equals(user.getHomepageType())) {
      return projectHomepage(dbSession, user);
    }

    if (APPLICATION.toString().equals(user.getHomepageType()) || PORTFOLIO.toString().equals(user.getHomepageType())) {
      return applicationAndPortfolioHomepage(dbSession, user);
    }

    if (ORGANIZATION.toString().equals(user.getHomepageType())) {
      return organizationHomepage(dbSession, user);
    }

    return of(CurrentWsResponse.Homepage.newBuilder()
      .setType(CurrentWsResponse.HomepageType.valueOf(user.getHomepageType()))
      .build());
  }

  private Optional<CurrentWsResponse.Homepage> projectHomepage(DbSession dbSession, UserDto user) {
    Optional<ComponentDto> projectOptional = ofNullable(dbClient.componentDao().selectByUuid(dbSession, of(user.getHomepageParameter()).orElse(EMPTY)).orElse(null));
    if (shouldCleanProjectHomepage(projectOptional)) {
      cleanUserHomepageInDb(dbSession, user);
      return empty();
    }

    CurrentWsResponse.Homepage.Builder homepage = CurrentWsResponse.Homepage.newBuilder()
      .setType(CurrentWsResponse.HomepageType.valueOf(user.getHomepageType()))
      .setComponent(projectOptional.get().getKey());
    ofNullable(projectOptional.get().getBranch()).ifPresent(homepage::setBranch);
    return of(homepage.build());
  }

  private boolean shouldCleanProjectHomepage(Optional<ComponentDto> projectOptional) {
    return !projectOptional.isPresent() || !userSession.hasComponentPermission(USER, projectOptional.get());
  }

  private Optional<CurrentWsResponse.Homepage> applicationAndPortfolioHomepage(DbSession dbSession, UserDto user) {
    Optional<ComponentDto> componentOptional = ofNullable(dbClient.componentDao().selectByUuid(dbSession, of(user.getHomepageParameter()).orElse(EMPTY)).orElse(null));
    if (shouldCleanApplicationOrPortfolioHomepage(componentOptional)) {
      cleanUserHomepageInDb(dbSession, user);
      return empty();
    }

    return of(CurrentWsResponse.Homepage.newBuilder()
      .setType(CurrentWsResponse.HomepageType.valueOf(user.getHomepageType()))
      .setComponent(componentOptional.get().getKey())
      .build());
  }

  private boolean shouldCleanApplicationOrPortfolioHomepage(Optional<ComponentDto> componentOptional) {
    return !componentOptional.isPresent() || !pluginRepository.hasPlugin(GOVERNANCE_PLUGIN_KEY)
      || !userSession.hasComponentPermission(USER, componentOptional.get());
  }

  private Optional<CurrentWsResponse.Homepage> organizationHomepage(DbSession dbSession, UserDto user) {
    Optional<OrganizationDto> organizationOptional = dbClient.organizationDao().selectByUuid(dbSession, of(user.getHomepageParameter()).orElse(EMPTY));
    if (!organizationOptional.isPresent()) {
      cleanUserHomepageInDb(dbSession, user);
      return empty();
    }

    return of(CurrentWsResponse.Homepage.newBuilder()
      .setType(CurrentWsResponse.HomepageType.valueOf(user.getHomepageType()))
      .setOrganization(organizationOptional.get().getKey())
      .build());
  }

  private void cleanUserHomepageInDb(DbSession dbSession, UserDto user) {
    dbClient.userDao().cleanHomepage(dbSession, user);
  }

  private CurrentWsResponse.Homepage defaultHomepage() {
    return CurrentWsResponse.Homepage.newBuilder()
      .setType(CurrentWsResponse.HomepageType.valueOf(homepageTypes.getDefaultType().name()))
      .build();
  }

  private List<CurrentWsResponse.Setting> loadUserSettings(DbSession dbSession, UserDto user) {
    return dbClient.userPropertiesDao().selectByUser(dbSession, user)
      .stream()
      .map(dto -> CurrentWsResponse.Setting.newBuilder()
        .setKey(dto.getKey())
        .setValue(dto.getValue())
        .build())
      .collect(MoreCollectors.toList());
  }

  private static boolean noHomepageSet(UserDto user) {
    return user.getHomepageType() == null;
  }

}
