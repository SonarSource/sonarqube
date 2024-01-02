/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.v2.api.user.controller;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.server.common.PaginationInformation;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.user.service.UserCreateRequest;
import org.sonar.server.common.user.service.UserInformation;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.common.user.service.UsersSearchRequest;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.user.converter.UsersSearchRestResponseGenerator;
import org.sonar.server.v2.api.user.response.UserRestResponse;
import org.sonar.server.v2.api.user.request.UserCreateRestRequest;
import org.sonar.server.v2.api.user.request.UserUpdateRestRequest;
import org.sonar.server.v2.api.user.request.UsersSearchRestRequest;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;

import static org.sonar.server.common.PaginationInformation.forPageIndex;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class DefaultUserController implements UserController {
  private final UsersSearchRestResponseGenerator usersSearchResponseGenerator;
  private final UserService userService;
  private final UserSession userSession;

  public DefaultUserController(
    UserSession userSession,
    UserService userService,
    UsersSearchRestResponseGenerator usersSearchResponseGenerator) {
    this.userSession = userSession;
    this.usersSearchResponseGenerator = usersSearchResponseGenerator;
    this.userService = userService;
  }

  @Override
  public UsersSearchRestResponse search(UsersSearchRestRequest usersSearchRestRequest, @Nullable String excludedGroupId, RestPage page) {
    throwIfAdminOnlyParametersAreUsed(usersSearchRestRequest, excludedGroupId);

    SearchResults<UserInformation> userSearchResults = userService.findUsers(toUserSearchRequest(usersSearchRestRequest, excludedGroupId, page));
    PaginationInformation paging = forPageIndex(page.pageIndex()).withPageSize(page.pageSize()).andTotal(userSearchResults.total());

    return usersSearchResponseGenerator.toUsersForResponse(userSearchResults.searchResults(), paging);
  }

  private void throwIfAdminOnlyParametersAreUsed(UsersSearchRestRequest usersSearchRestRequest, @Nullable String excludedGroupId) {
    if (!userSession.isSystemAdministrator()) {
      throwIfValuePresent("groupId", usersSearchRestRequest.groupId());
      throwIfValuePresent("groupId!", excludedGroupId);
      throwIfValuePresent("externalIdentity", usersSearchRestRequest.externalIdentity());
      throwIfValuePresent("sonarLintLastConnectionDateFrom", usersSearchRestRequest.sonarLintLastConnectionDateFrom());
      throwIfValuePresent("sonarLintLastConnectionDateTo", usersSearchRestRequest.sonarLintLastConnectionDateTo());
      throwIfValuePresent("sonarQubeLastConnectionDateFrom", usersSearchRestRequest.sonarQubeLastConnectionDateFrom());
      throwIfValuePresent("sonarQubeLastConnectionDateTo", usersSearchRestRequest.sonarQubeLastConnectionDateTo());
    }
  }

  private static void throwIfValuePresent(String parameter, @Nullable Object value) {
    Optional.ofNullable(value).ifPresent(v -> throwForbiddenFor(parameter));
  }

  private static void throwForbiddenFor(String parameterName) {
    throw new ForbiddenException("Parameter " + parameterName + " requires Administer System permission.");
  }

  private static UsersSearchRequest toUserSearchRequest(UsersSearchRestRequest usersSearchRestRequest, @Nullable String excludedGroupId, RestPage page) {
    return UsersSearchRequest.builder()
      .setDeactivated(Optional.ofNullable(usersSearchRestRequest.active()).map(active -> !active).orElse(false))
      .setManaged(usersSearchRestRequest.managed())
      .setQuery(usersSearchRestRequest.q())
      .setExternalLogin(usersSearchRestRequest.externalIdentity())
      .setLastConnectionDateFrom(usersSearchRestRequest.sonarQubeLastConnectionDateFrom())
      .setLastConnectionDateTo(usersSearchRestRequest.sonarQubeLastConnectionDateTo())
      .setSonarLintLastConnectionDateFrom(usersSearchRestRequest.sonarLintLastConnectionDateFrom())
      .setSonarLintLastConnectionDateTo(usersSearchRestRequest.sonarLintLastConnectionDateTo())
      .setGroupUuid(usersSearchRestRequest.groupId())
      .setExcludedGroupUuid(excludedGroupId)
      .setPage(page.pageIndex())
      .setPageSize(page.pageSize())
      .build();
  }

  @Override
  public void deactivate(String id, Boolean anonymize) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    checkRequest(!id.equals(userSession.getUuid()), "Self-deactivation is not possible");
    userService.deactivate(id, anonymize);
  }

  @Override
  public UserRestResponse fetchUser(String id) {
    return usersSearchResponseGenerator.toRestUser(userService.fetchUser(id));
  }

  @Override
  public UserRestResponse updateUser(String id, UserUpdateRestRequest updateRequest) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    UpdateUser update = toUpdateUser(updateRequest);
    UserInformation updatedUser = userService.updateUser(id, update);
    return usersSearchResponseGenerator.toRestUser(updatedUser);
  }

  private static UpdateUser toUpdateUser(UserUpdateRestRequest updateRequest) {
    UpdateUser update = new UpdateUser();
    updateRequest.getName().applyIfDefined(update::setName);
    updateRequest.getEmail().applyIfDefined(update::setEmail);
    updateRequest.getScmAccounts().applyIfDefined(update::setScmAccounts);
    return update;
  }

  @Override
  public UserRestResponse create(UserCreateRestRequest userCreateRestRequest) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    UserCreateRequest userCreateRequest = toUserCreateRequest(userCreateRestRequest);
    return usersSearchResponseGenerator.toRestUser(userService.createUser(userCreateRequest));
  }

  private static UserCreateRequest toUserCreateRequest(UserCreateRestRequest userCreateRestRequest) {
    return UserCreateRequest.builder()
      .setEmail(userCreateRestRequest.email())
      .setLocal(userCreateRestRequest.local())
      .setLogin(userCreateRestRequest.login())
      .setName(userCreateRestRequest.name())
      .setPassword(userCreateRestRequest.password())
      .setScmAccounts(userCreateRestRequest.scmAccounts())
      .build();
  }

}
