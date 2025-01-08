/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import java.util.Objects;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.PaginationInformation;
import org.sonar.server.common.user.UsersSearchResponseGenerator;
import org.sonar.server.common.user.service.UserInformation;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Users;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.sonarqube.ws.Users.SearchWsResponse.newBuilder;

public class SearchWsReponseGenerator implements UsersSearchResponseGenerator<Users.SearchWsResponse> {

  private final UserSession userSession;

  public SearchWsReponseGenerator(UserSession userSession) {
    this.userSession = userSession;
  }

  @Override
  public Users.SearchWsResponse toUsersForResponse(List<UserInformation> userInformations, PaginationInformation paginationInformation) {
    Users.SearchWsResponse.Builder responseBuilder = newBuilder();
    userInformations.forEach(user -> responseBuilder.addUsers(toSearchResponsUser(user)));
    responseBuilder.getPagingBuilder()
      .setPageIndex(paginationInformation.pageIndex())
      .setPageSize(paginationInformation.pageSize())
      .setTotal(paginationInformation.total())
      .build();
    return responseBuilder.build();
  }

  private Users.SearchWsResponse.User toSearchResponsUser(UserInformation userInformation) {
    UserDto userDto = userInformation.userDto();
    Users.SearchWsResponse.User.Builder userBuilder = Users.SearchWsResponse.User.newBuilder().setLogin(userDto.getLogin());
    ofNullable(userDto.getName()).ifPresent(userBuilder::setName);
    if (userSession.isLoggedIn()) {
      userInformation.avatar().ifPresent(userBuilder::setAvatar);
      userBuilder.setActive(userDto.isActive());
      userBuilder.setLocal(userDto.isLocal());
      ofNullable(userDto.getExternalIdentityProvider()).ifPresent(userBuilder::setExternalProvider);
      if (!userDto.getSortedScmAccounts().isEmpty()) {
        userBuilder.setScmAccounts(Users.SearchWsResponse.ScmAccounts.newBuilder().addAllScmAccounts(userDto.getSortedScmAccounts()));
      }
    }
    if (userSession.isSystemAdministrator() || Objects.equals(userSession.getUuid(), userDto.getUuid())) {
      ofNullable(userDto.getEmail()).ifPresent(userBuilder::setEmail);
      if (!userInformation.groups().isEmpty()) {
        userBuilder.setGroups(Users.SearchWsResponse.Groups.newBuilder().addAllGroups(userInformation.groups()));
      }
      ofNullable(userDto.getExternalLogin()).ifPresent(userBuilder::setExternalIdentity);
      userBuilder.setTokensCount(userInformation.tokensCount());
      ofNullable(userDto.getLastConnectionDate()).map(DateUtils::formatDateTime).ifPresent(userBuilder::setLastConnectionDate);
      ofNullable(userDto.getLastSonarlintConnectionDate())
        .map(DateUtils::formatDateTime).ifPresent(userBuilder::setSonarLintLastConnectionDate);
      userBuilder.setManaged(TRUE.equals(userInformation.managed()));
    }
    return userBuilder.build();
  }
}
