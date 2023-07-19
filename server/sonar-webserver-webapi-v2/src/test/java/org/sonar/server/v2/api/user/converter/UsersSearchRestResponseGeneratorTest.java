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
import org.sonar.api.utils.Paging;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.user.service.UserSearchResult;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.response.PageRestResponse;
import org.sonar.server.v2.api.user.model.RestUser;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersSearchRestResponseGeneratorTest {

  @Mock
  private UserSession userSession;

  @InjectMocks
  private UsersSearchRestResponseGenerator usersSearchRestResponseGenerator;

  @Test
  public void toUsersForResponse_whenNoResults_mapsCorrectly() {
    Paging paging = Paging.forPageIndex(1).withPageSize(2).andTotal(3);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(), paging);

    assertThat(usersForResponse.users()).isEmpty();
    assertPaginationInformationAreCorrect(paging, usersForResponse.pageRestResponse());
  }

  @Test
  public void toUsersForResponse_whenAdmin_mapsAllFields() {
    when(userSession.isLoggedIn()).thenReturn(true);
    when(userSession.isSystemAdministrator()).thenReturn(true);

    Paging paging = Paging.forPageIndex(1).withPageSize(2).andTotal(3);

    UserSearchResult userSearchResult1 = mockSearchResult(1, true);
    UserSearchResult userSearchResult2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userSearchResult1, userSearchResult2), paging);

    RestUser expectUser1 = buildExpectedResponseForAdmin(userSearchResult1);
    RestUser expectUser2 = buildExpectedResponseForAdmin(userSearchResult2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.pageRestResponse());
  }

  private static RestUser buildExpectedResponseForAdmin(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();
    return new RestUser(
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
      userSearchResult.tokensCount()
    );
  }

  @Test
  public void toUsersForResponse_whenNonAdmin_mapsNonAdminFields() {
    when(userSession.isLoggedIn()).thenReturn(true);

    Paging paging = Paging.forPageIndex(1).withPageSize(2).andTotal(3);

    UserSearchResult userSearchResult1 = mockSearchResult(1, true);
    UserSearchResult userSearchResult2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userSearchResult1, userSearchResult2), paging);

    RestUser expectUser1 = buildExpectedResponseForUser(userSearchResult1);
    RestUser expectUser2 = buildExpectedResponseForUser(userSearchResult2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.pageRestResponse());
  }

  private static RestUser buildExpectedResponseForUser(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();
    return new RestUser(
      userDto.getLogin(),
      userDto.getLogin(),
      userDto.getName(),
      userDto.getEmail(),
      userDto.isActive(),
      userDto.isLocal(),
      null,
      null,
      userDto.getExternalIdentityProvider(),
      userSearchResult.avatar().orElse(null),
      null,
      null,
      null,
      null
    );
  }

  @Test
  public void toUsersForResponse_whenAnonymous_returnsOnlyNameAndLogin() {
    Paging paging = Paging.forPageIndex(1).withPageSize(2).andTotal(3);

    UserSearchResult userSearchResult1 = mockSearchResult(1, true);
    UserSearchResult userSearchResult2 = mockSearchResult(2, false);

    UsersSearchRestResponse usersForResponse = usersSearchRestResponseGenerator.toUsersForResponse(List.of(userSearchResult1, userSearchResult2), paging);

    RestUser expectUser1 = buildExpectedResponseForAnonymous(userSearchResult1);
    RestUser expectUser2 = buildExpectedResponseForAnonymous(userSearchResult2);
    assertThat(usersForResponse.users()).containsExactly(expectUser1, expectUser2);
    assertPaginationInformationAreCorrect(paging, usersForResponse.pageRestResponse());
  }

  private static RestUser buildExpectedResponseForAnonymous(UserSearchResult userSearchResult) {
    UserDto userDto = userSearchResult.userDto();
    return new RestUser(
      userDto.getLogin(),
      userDto.getLogin(),
      userDto.getName(),
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
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

  private static void assertPaginationInformationAreCorrect(Paging paging, PageRestResponse pageRestResponse) {
    assertThat(pageRestResponse.pageIndex()).isEqualTo(paging.pageIndex());
    assertThat(pageRestResponse.pageSize()).isEqualTo(paging.pageSize());
    assertThat(pageRestResponse.total()).isEqualTo(paging.total());
  }

}
