/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.user.converter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.PaginationInformation;
import org.sonar.server.common.user.UsersSearchResponseGenerator;
import org.sonar.server.common.user.service.UserSearchResult;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.response.PageRestResponse;
import org.sonar.server.v2.api.user.model.RestUser;
import org.sonar.server.v2.api.user.model.RestUserForAdmins;
import org.sonar.server.v2.api.user.model.RestUserForAnonymousUsers;
import org.sonar.server.v2.api.user.model.RestUserForLoggedInUsers;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;

public class UsersSearchRestResponseGenerator implements UsersSearchResponseGenerator<UsersSearchRestResponse> {

  private final UserSession userSession;

  public UsersSearchRestResponseGenerator(UserSession userSession) {
    this.userSession = userSession;
  }

  @Override
  public UsersSearchRestResponse toUsersForResponse(List<UserSearchResult> userSearchResults, PaginationInformation paginationInformation) {
    List<RestUser> usersForResponse = toUsersForResponse(userSearchResults);
    PageRestResponse pageRestResponse = new PageRestResponse(paginationInformation.pageIndex(), paginationInformation.pageSize(), paginationInformation.total());
    return new UsersSearchRestResponse(usersForResponse, pageRestResponse);
  }

  private List<RestUser> toUsersForResponse(List<UserSearchResult> userSearchResults) {
    return userSearchResults.stream()
      .map(this::toRestUser)
      .toList();
  }

  public RestUser toRestUser(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();

    String login = userDto.getLogin();
    String name = userDto.getName();
    if (!userSession.isLoggedIn()) {
      return new RestUserForAnonymousUsers(login, login, name);
    }

    String avatar = userSearchResult.avatar().orElse(null);
    Boolean active = userDto.isActive();
    Boolean local = userDto.isLocal();
    String email = userDto.getEmail();
    String externalIdentityProvider = userDto.getExternalIdentityProvider();
    if (userSession.isSystemAdministrator() || Objects.equals(userSession.getUuid(), userDto.getUuid())) {
      String externalLogin = userDto.getExternalLogin();
      Boolean managed = userSearchResult.managed();
      String sqLastConnectionDate = toDateTime(userDto.getLastConnectionDate());
      String slLastConnectionDate = toDateTime(userDto.getLastSonarlintConnectionDate());
      int groupSize = userSearchResult.groups().size();
      int tokensCount = userSearchResult.tokensCount();
      List<String> scmAccounts = userSearchResult.userDto().getSortedScmAccounts();
      return new RestUserForAdmins(
        login,
        login,
        name,
        email,
        active,
        local,
        managed,
        externalLogin,
        externalIdentityProvider,
        avatar,
        sqLastConnectionDate,
        slLastConnectionDate,
        groupSize,
        tokensCount,
        scmAccounts);
    }
    return new RestUserForLoggedInUsers(login, login, name, email, active, local, externalIdentityProvider, avatar);
  }

  private static String toDateTime(@Nullable Long dateTimeMs) {
    return Optional.ofNullable(dateTimeMs).map(DateUtils::formatDateTime).orElse(null);
  }
}
