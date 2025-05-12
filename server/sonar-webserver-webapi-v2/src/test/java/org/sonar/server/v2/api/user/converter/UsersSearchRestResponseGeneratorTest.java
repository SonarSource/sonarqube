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
import org.sonar.server.common.user.service.UserInformation;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.response.PageRestResponse;
import org.sonar.server.v2.api.user.response.UserRestResponseForAdmins;
import org.sonar.server.v2.api.user.response.UserRestResponseForAnonymousUsers;
import org.sonar.server.v2.api.user.response.UserRestResponseForLoggedInUsers;
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

    UserInformation userInformation1 = mockSearchResult(1, true);
    UserInformation userInformation2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userInformation1, userInformation2), paging);

    UserRestResponseForAdmins expectUser1 = buildExpectedResponseForAdmin(userInformation1);
    UserRestResponseForAdmins expectUser2 = buildExpectedResponseForAdmin(userInformation2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  private static UserRestResponseForAdmins buildExpectedResponseForAdmin(UserInformation userInformation) {
    UserDto userDto = userInformation.userDto();
    return new UserRestResponseForAdmins(
      userDto.getUuid(),
      userDto.getLogin(),
      userDto.getName(),
      userDto.getEmail(),
      userDto.isActive(),
      userDto.isLocal(),
      userInformation.managed(),
      userDto.getExternalLogin(),
      userDto.getExternalIdentityProvider(),
      userDto.getExternalId(),
      userInformation.avatar().orElse(null),
      toDateTime(userDto.getLastConnectionDate()),
      toDateTime(userDto.getLastSonarlintConnectionDate()),
      userInformation.userDto().getSortedScmAccounts()
    );
  }

  @Test
  public void toUsersForResponse_whenNonAdmin_mapsNonAdminFields() {
    when(userSession.isLoggedIn()).thenReturn(true);

    PaginationInformation paging = forPageIndex(1).withPageSize(2).andTotal(3);

    UserInformation userInformation1 = mockSearchResult(1, true);
    UserInformation userInformation2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userInformation1, userInformation2), paging);

    UserRestResponseForLoggedInUsers expectUser1 = buildExpectedResponseForUser(userInformation1);
    UserRestResponseForLoggedInUsers expectUser2 = buildExpectedResponseForUser(userInformation2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  private static UserRestResponseForLoggedInUsers buildExpectedResponseForUser(UserInformation userInformation) {
    UserDto userDto = userInformation.userDto();
    return new UserRestResponseForLoggedInUsers(
      userDto.getLogin(),
      userDto.getName(),
      userDto.isActive(),
      userInformation.avatar().orElse(null)
    );
  }

  @Test
  public void toUsersForResponse_whenAnonymous_returnsOnlyNameAndLogin() {
    PaginationInformation paging = forPageIndex(1).withPageSize(2).andTotal(3);

    UserInformation userInformation1 = mockSearchResult(1, true);
    UserInformation userInformation2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userInformation1, userInformation2), paging);

    UserRestResponseForAnonymousUsers expectUser1 = buildExpectedResponseForAnonymous(userInformation1);
    UserRestResponseForAnonymousUsers expectUser2 = buildExpectedResponseForAnonymous(userInformation2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.page());
  }

  private static UserRestResponseForAnonymousUsers buildExpectedResponseForAnonymous(UserInformation userInformation) {
    UserDto userDto = userInformation.userDto();
    return new UserRestResponseForAnonymousUsers(
      userDto.getLogin(),
      userDto.getName()
    );
  }

  private static String toDateTime(@Nullable Long dateTimeMs) {
    return Optional.ofNullable(dateTimeMs).map(DateUtils::formatDateTime).orElse(null);
  }

  private static UserInformation mockSearchResult(int i, boolean booleanFlagsValue) {
    UserInformation userInformation = mock(UserInformation.class, RETURNS_DEEP_STUBS);
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

    when(userInformation.userDto()).thenReturn(user1);
    when(userInformation.managed()).thenReturn(booleanFlagsValue);
    return userInformation;
  }

  private static void assertPaginationInformationAreCorrect(PaginationInformation paginationInformation, PageRestResponse pageRestResponse) {
    assertThat(pageRestResponse.pageIndex()).isEqualTo(paginationInformation.pageIndex());
    assertThat(pageRestResponse.pageSize()).isEqualTo(paginationInformation.pageSize());
    assertThat(pageRestResponse.total()).isEqualTo(paginationInformation.total());
  }

}
