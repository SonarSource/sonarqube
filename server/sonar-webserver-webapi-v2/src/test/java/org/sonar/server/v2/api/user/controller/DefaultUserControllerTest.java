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
package org.sonar.server.v2.api.user.controller;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.user.service.UserSearchResult;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.common.user.service.UsersSearchRequest;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.response.PageRestResponse;
import org.sonar.server.v2.api.user.converter.UsersSearchRestResponseGenerator;
import org.sonar.server.v2.api.user.model.RestUser;
import org.sonar.server.v2.api.user.request.UserCreateRestRequest;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.v2.WebApiEndpoints.USER_ENDPOINT;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_INDEX;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_SIZE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultUserControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final UserService userService = mock(UserService.class);
  private final UsersSearchRestResponseGenerator responseGenerator = mock(UsersSearchRestResponseGenerator.class);
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultUserController(userSession, userService, responseGenerator));

  private static final Gson gson = new Gson();

  @Test
  public void search_whenNoParameters_shouldUseDefaultAndForwardToUserService() throws Exception {
    when(userService.findUsers(any())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc.perform(get(USER_ENDPOINT))
      .andExpect(status().isOk());

    ArgumentCaptor<UsersSearchRequest> requestCaptor = ArgumentCaptor.forClass(UsersSearchRequest.class);
    verify(userService).findUsers(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getPageSize()).isEqualTo(Integer.valueOf(DEFAULT_PAGE_SIZE));
    assertThat(requestCaptor.getValue().getPage()).isEqualTo(Integer.valueOf(DEFAULT_PAGE_INDEX));
    assertThat(requestCaptor.getValue().isDeactivated()).isFalse();
  }

  @Test
  public void search_whenParametersUsed_shouldForwardWithParameters() throws Exception {
    when(userService.findUsers(any())).thenReturn(new SearchResults<>(List.of(), 0));
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(get(USER_ENDPOINT)
        .param("active", "false")
        .param("managed", "true")
        .param("q", "q")
        .param("sonarQubeLastConnectionDateFrom", "2020-01-01T00:00:00+0100")
        .param("sonarQubeLastConnectionDateTo", "2020-01-01T00:00:00+0100")
        .param("sonarLintLastConnectionDateFrom", "2020-01-01T00:00:00+0100")
        .param("sonarLintLastConnectionDateTo", "2020-01-01T00:00:00+0100")
        .param("pageSize", "100")
        .param("pageIndex", "2"))
      .andExpect(status().isOk());

    ArgumentCaptor<UsersSearchRequest> requestCaptor = ArgumentCaptor.forClass(UsersSearchRequest.class);
    verify(userService).findUsers(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getPageSize()).isEqualTo(100);
    assertThat(requestCaptor.getValue().getPage()).isEqualTo(2);
    assertThat(requestCaptor.getValue().isDeactivated()).isTrue();
  }

  @Test
  public void search_whenAdminParametersUsedButNotAdmin_shouldFail() throws Exception {
    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarQubeLastConnectionDateFrom", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"parameter sonarQubeLastConnectionDateFrom requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarQubeLastConnectionDateTo", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"parameter sonarQubeLastConnectionDateTo requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarLintLastConnectionDateFrom", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"parameter sonarLintLastConnectionDateFrom requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarLintLastConnectionDateTo", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"parameter sonarLintLastConnectionDateTo requires Administer System permission.\"}"));
  }

  @Test
  public void search_whenUserServiceReturnUsers_shouldReturnThem() throws Exception {
    UserSearchResult user1 = generateUserSearchResult("user1", true, true, false, 2, 3);
    UserSearchResult user2 = generateUserSearchResult("user2", true, false, false, 3, 0);
    UserSearchResult user3 = generateUserSearchResult("user3", true, false, true, 1, 1);
    UserSearchResult user4 = generateUserSearchResult("user4", false, true, false, 0, 0);
    List<UserSearchResult> users = List.of(user1, user2, user3, user4);
    SearchResults<UserSearchResult> searchResult = new SearchResults<>(users, users.size());
    when(userService.findUsers(any())).thenReturn(searchResult);
    List<RestUser> restUsers = List.of(toRestUser(user1), toRestUser(user2), toRestUser(user3), toRestUser(user4));
    when(responseGenerator.toUsersForResponse(eq(searchResult.searchResults()), any())).thenReturn(new UsersSearchRestResponse(restUsers, new PageRestResponse(1, 50, 4)));
    userSession.logIn().setSystemAdministrator();

    MvcResult mvcResult = mockMvc.perform(get(USER_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    UsersSearchRestResponse actualUsersSearchRestResponse = gson.fromJson(mvcResult.getResponse().getContentAsString(), UsersSearchRestResponse.class);
    assertThat(actualUsersSearchRestResponse.users())
      .containsExactlyElementsOf(restUsers);
    assertThat(actualUsersSearchRestResponse.pageRestResponse().total()).isEqualTo(users.size());

  }

  private UserSearchResult generateUserSearchResult(String id, boolean active, boolean local, boolean managed, int groupsCount, int tokensCount) {
    UserDto userDto = new UserDto()
      .setLogin("login_" + id)
      .setUuid("uuid_" + id)
      .setName("name_" + id)
      .setEmail(id + "@email.com")
      .setActive(active)
      .setLocal(local)
      .setExternalLogin("externalLogin_" + id)
      .setExternalId("externalId_" + id)
      .setExternalIdentityProvider("externalIdentityProvider_" + id)
      .setLastConnectionDate(0L)
      .setLastSonarlintConnectionDate(1L);

    List<String> groups = new ArrayList<>();
    IntStream.range(1, groupsCount).forEach(i -> groups.add("group" + i));

    return new UserSearchResult(userDto, managed, Optional.of("avatar_" + id), groups, tokensCount);
  }

  private RestUser toRestUser(UserSearchResult userSearchResult) {
    return new RestUser(
      userSearchResult.userDto().getLogin(),
      userSearchResult.userDto().getLogin(),
      userSearchResult.userDto().getName(),
      userSearchResult.userDto().getEmail(),
      userSearchResult.userDto().isActive(),
      userSearchResult.userDto().isLocal(),
      userSearchResult.managed(),
      userSearchResult.userDto().getExternalLogin(),
      userSearchResult.userDto().getExternalIdentityProvider(),
      userSearchResult.avatar().orElse(""),
      formatDateTime(userSearchResult.userDto().getLastConnectionDate()),
      formatDateTime(userSearchResult.userDto().getLastSonarlintConnectionDate()),
      userSearchResult.groups().size(),
      userSearchResult.tokensCount(),
      userSearchResult.userDto().getSortedScmAccounts());
  }

  @Test
  public void deactivate_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void deactivate_whenUserServiceThrowsNotFoundException_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(new NotFoundException("User not found.")).when(userService).deactivate("userToDelete", false);

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"User not found.\"}"));
  }

  @Test
  public void deactivate_whenUserServiceThrowsBadRequestException_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("Not allowed")).when(userService).deactivate("userToDelete", false);

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Not allowed\"}"));
  }

  @Test
  public void deactivate_whenUserTryingToDeactivateThemself_shouldReturnBadRequest() throws Exception {
    userSession.logIn("userToDelete").setSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Self-deactivation is not possible\"}"));
  }

  @Test
  public void deactivate_whenAnonymizeParameterIsNotBoolean_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete").param("anonymize", "maybe"))
      .andExpect(
        status().isBadRequest());
  }

  @Test
  public void deactivate_whenAnonymizeIsNotSpecified_shouldDeactivateUserWithoutAnonymization() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete"))
      .andExpect(status().isNoContent());

    verify(userService).deactivate("userToDelete", false);
  }

  @Test
  public void deactivate_whenAnonymizeFalse_shouldDeactivateUserWithoutAnonymization() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete").param("anonymize", "false"))
      .andExpect(status().isNoContent());

    verify(userService).deactivate("userToDelete", false);
  }

  @Test
  public void deactivate_whenAnonymizeTrue_shouldDeactivateUserWithAnonymization() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/userToDelete").param("anonymize", "true"))
      .andExpect(status().isNoContent());

    verify(userService).deactivate("userToDelete", true);
  }

  @Test
  public void fetchUser_whenUserServiceThrowsNotFoundException_returnsNotFound() throws Exception {
    when(userService.fetchUser("userLogin")).thenThrow(new NotFoundException("Not found"));
    mockMvc.perform(get(USER_ENDPOINT + "/userLogin"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Not found\"}")
      );

  }

  @Test
  public void fetchUser_whenUserExists_shouldReturnUser() throws Exception {
    UserSearchResult user = generateUserSearchResult("user1", true, true, false, 2, 3);
    RestUser restUser = toRestUser(user);
    when(userService.fetchUser("userLogin")).thenReturn(user);
    when(responseGenerator.toRestUser(user)).thenReturn(restUser);
    MvcResult mvcResult = mockMvc.perform(get(USER_ENDPOINT + "/userLogin"))
      .andExpect(status().isOk())
      .andReturn();
    RestUser responseUser = gson.fromJson(mvcResult.getResponse().getContentAsString(), RestUser.class);
    assertThat(responseUser).isEqualTo(restUser);
  }

  @Test
  public void create_whenNotAnAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
      post(USER_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(gson.toJson(new UserCreateRestRequest(null, null, "login", "name", null, null))))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void create_whenNoLogin_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
        post(USER_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(new UserCreateRestRequest(null, null, null, "name", null, null))))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Value {} for field login was rejected. Error: must not be null\"}"));
  }

  @Test
  public void create_whenNoName_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
        post(USER_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(new UserCreateRestRequest(null, null, "login", null, null, null))))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Value {} for field name was rejected. Error: must not be null\"}"));
  }

  @Test
  public void create_whenUserServiceThrow_shouldReturnServerError() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(userService.createUser(any())).thenThrow(new IllegalArgumentException("IllegalArgumentException"));

    mockMvc.perform(
        post(USER_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(new UserCreateRestRequest("e@mail.com", true, "login", "name", "password", List.of("scm")))))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"IllegalArgumentException\"}"));
  }

  @Test
  public void create_whenUserServiceReturnUser_shouldReturnIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    UserSearchResult userSearchResult = generateUserSearchResult("1", true, true, false, 1, 2);
    UserDto userDto = userSearchResult.userDto();
    when(userService.createUser(any())).thenReturn(userSearchResult);
    when(responseGenerator.toRestUser(userSearchResult)).thenReturn(toRestUser(userSearchResult));

    MvcResult mvcResult = mockMvc.perform(
        post(USER_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(gson.toJson(new UserCreateRestRequest(
            userDto.getEmail(), userDto.isLocal(), userDto.getLogin(), userDto.getName(), "password", userDto.getSortedScmAccounts()))))
      .andExpect(status().isOk())
      .andReturn();
    RestUser responseUser = gson.fromJson(mvcResult.getResponse().getContentAsString(), RestUser.class);
    assertThat(responseUser).isEqualTo(toRestUser(userSearchResult));
  }

}
