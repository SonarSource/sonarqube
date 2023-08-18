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
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.PaginationInformation;
import org.sonar.server.common.user.service.UserSearchResult;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.response.PageRestResponse;
import org.sonar.server.v2.api.user.model.RestUserForAdmins;
import org.sonar.server.v2.api.user.model.RestUserForAnonymousUsers;
import org.sonar.server.v2.api.user.model.RestUserForLoggedInUsers;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.common.PaginationInformation.forPageIndex;

@RunWith(MockitoJUnitRunner.class)
public class UsersSearchRestResponseGeneratorTest {

  @Mock
  private UserSession userSession;

  @InjectMocks
  private UsersSearchRestResponseGenerator usersSearchRestResponseGenerator;

  @Test
  public void toUsersForResponse_whenNoResults_mapsCorrectly() {
    PaginationInformation paging = forPageIndex(1).withPageSize(2).andTotal(3);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(), paging);

    assertThat(usersForResponse.users()).isEmpty();
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  @Test
  public void toUsersForResponse_whenAdmin_mapsAllFields() {
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.isSystemAdministrator()).thenReturn(true);

    PaginationInformation paging = forPageIndex(1).withPageSize(2).andTotal(3);

    UserSearchResult userSearchResult1 = mockSearchResult(1, true);
    UserSearchResult userSearchResult2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userSearchResult1, userSearchResult2), paging);

    RestUserForAdmins expectUser1 = buildExpectedResponseForAdmin(userSearchResult1);
    RestUserForAdmins expectUser2 = buildExpectedResponseForAdmin(userSearchResult2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  private static RestUserForAdmins buildExpectedResponseForAdmin(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();
    return new RestUserForAdmins(
      userDto.getLogin(),
      userDto.getLogin(),
      userDto.getName(),
      userDto.getEmail(),
      userDto.isActive(),
      userDto.isLocal(),
      userSearchResult.managed(),
      userDto.getExternalLogin(),
      userDto.getExternalIdentityProvider(),
      userSearchResult.avatar().orElse(null),
      toDateTime(userDto.getLastConnectionDate()),
      toDateTime(userDto.getLastSonarlintConnectionDate()),
      userSearchResult.groups().size(),
      userSearchResult.tokensCount(),
      userSearchResult.userDto().getSortedScmAccounts()
    );
  }

  @Test
  public void toUsersForResponse_whenNonAdmin_mapsNonAdminFields() {
    when(userSession.isLoggedIn()).thenReturn(true);

    PaginationInformation paging = forPageIndex(1).withPageSize(2).andTotal(3);

    UserSearchResult userSearchResult1 = mockSearchResult(1, true);
    UserSearchResult userSearchResult2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userSearchResult1, userSearchResult2), paging);

    RestUserForLoggedInUsers expectUser1 = buildExpectedResponseForUser(userSearchResult1);
    RestUserForLoggedInUsers expectUser2 = buildExpectedResponseForUser(userSearchResult2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  private static RestUserForLoggedInUsers buildExpectedResponseForUser(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();
    return new RestUserForLoggedInUsers(
      userDto.getLogin(),
      userDto.getLogin(),
      userDto.getName(),
      userDto.getEmail(),
      userDto.isActive(),
      userDto.isLocal(),
      userDto.getExternalIdentityProvider(),
      userSearchResult.avatar().orElse(null)
    );
  }

  @Test
  public void toUsersForResponse_whenAnonymous_returnsOnlyNameAndLogin() {
    PaginationInformation paging = forPageIndex(1).withPageSize(2).andTotal(3);

    UserSearchResult userSearchResult1 = mockSearchResult(1, true);
    UserSearchResult userSearchResult2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userSearchResult1, userSearchResult2), paging);

    RestUserForAnonymousUsers expectUser1 = buildExpectedResponseForAnonymous(userSearchResult1);
    RestUserForAnonymousUsers expectUser2 = buildExpectedResponseForAnonymous(userSearchResult2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  private static RestUserForAnonymousUsers buildExpectedResponseForAnonymous(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();
    return new RestUserForAnonymousUsers(
      userDto.getLogin(),
      userDto.getLogin(),
      userDto.getName()
    );
  }

  private static String toDateTime(@Nullable Long dateTimeMs) {
    return Optional.ofNullable(dateTimeMs).map(DateUtils::formatDateTime).orElse(null);
  }

  private static UserSearchResult mockSearchResult(int i, boolean booleanFlagsValue) {
    UserSearchResult userSearchResult = mock(UserSearchResult.class, RETURNS_DEEP_STUBS);
    UserDto user1 = new UserDto()
      .setUuid("uuid_" + i)
      .setLogin("login_" + i)
      .setName("name_" + i)
      .setEmail("email@" + i)
      .setExternalId("externalId" + i)
      .setExternalLogin("externalLogin" + 1)
      .setExternalIdentityProvider("exernalIdp_" + i)
      .setLastConnectionDate(100L + i)
      .setLastSonarlintConnectionDate(200L + i)
      .setLocal(booleanFlagsValue)
      .setActive(booleanFlagsValue);

    when(userSearchResult.userDto()).thenReturn(user1);
    when(userSearchResult.managed()).thenReturn(booleanFlagsValue);
    when(userSearchResult.tokensCount()).thenReturn(i);
    when(userSearchResult.groups().size()).thenReturn(i * 100);
    return userSearchResult;
  }

  private static void assertPaginationInformationAreCorrect(PaginationInformation paginationInformation, PageRestResponse pageRestResponse) {
    assertThat(pageRestResponse.pageIndex()).isEqualTo(paginationInformation.pageIndex());
    assertThat(pageRestResponse.pageSize()).isEqualTo(paginationInformation.pageSize());
    assertThat(pageRestResponse.total()).isEqualTo(paginationInformation.total());
  }

}
