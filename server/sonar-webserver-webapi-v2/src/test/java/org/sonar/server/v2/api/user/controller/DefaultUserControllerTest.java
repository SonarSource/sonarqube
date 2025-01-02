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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.user.service.UserInformation;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.common.user.service.UsersSearchRequest;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.model.RestError;
import org.sonar.server.v2.api.response.PageRestResponse;
import org.sonar.server.v2.api.user.converter.UsersSearchRestResponseGenerator;
import org.sonar.server.v2.api.user.request.UserCreateRestRequest;
import org.sonar.server.v2.api.user.response.UserRestResponse;
import org.sonar.server.v2.api.user.response.UserRestResponseForAdmins;
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
import static org.sonar.api.utils.DateUtils.parseOffsetDateTime;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.sonar.server.v2.WebApiEndpoints.USER_ENDPOINT;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_INDEX;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_SIZE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultUserControllerTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final UserService userService = mock(UserService.class);
  private final UsersSearchRestResponseGenerator responseGenerator = mock(UsersSearchRestResponseGenerator.class);
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultUserController(userSession, userService, responseGenerator));

  private static final Gson gson = new GsonBuilder().registerTypeAdapter(UserRestResponse.class, new RestUserDeserializer()).create();

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
        .param("externalIdentity", "externalIdentity")
        .param("sonarQubeLastConnectionDateFrom", "2020-01-01T00:00:00+0100")
        .param("sonarQubeLastConnectionDateTo", "2020-01-01T00:00:00+0100")
        .param("sonarLintLastConnectionDateFrom", "2020-01-01T00:00:00+0100")
        .param("sonarLintLastConnectionDateTo", "2020-01-01T00:00:00+0100")
        .param("groupId", "groupId1")
        .param("groupId!", "groupId2")
        .param("pageSize", "100")
        .param("pageIndex", "2"))
      .andExpect(status().isOk());

    ArgumentCaptor<UsersSearchRequest> requestCaptor = ArgumentCaptor.forClass(UsersSearchRequest.class);
    verify(userService).findUsers(requestCaptor.capture());

    assertThat(requestCaptor.getValue().isDeactivated()).isTrue();
    assertThat(requestCaptor.getValue().isManaged()).isTrue();
    assertThat(requestCaptor.getValue().getQuery()).isEqualTo("q");
    assertThat(requestCaptor.getValue().getExternalLogin()).contains("externalIdentity");
    assertThat(requestCaptor.getValue().getLastConnectionDateFrom()).contains(parseOffsetDateTime("2020-01-01T00:00:00+0100"));
    assertThat(requestCaptor.getValue().getLastConnectionDateTo()).contains(parseOffsetDateTime("2020-01-01T00:00:00+0100"));
    assertThat(requestCaptor.getValue().getSonarLintLastConnectionDateFrom()).contains(parseOffsetDateTime("2020-01-01T00:00:00+0100"));
    assertThat(requestCaptor.getValue().getSonarLintLastConnectionDateTo()).contains(parseOffsetDateTime("2020-01-01T00:00:00+0100"));
    assertThat(requestCaptor.getValue().getGroupUuid()).contains("groupId1");
    assertThat(requestCaptor.getValue().getExcludedGroupUuid()).contains("groupId2");
    assertThat(requestCaptor.getValue().getPageSize()).isEqualTo(100);
    assertThat(requestCaptor.getValue().getPage()).isEqualTo(2);
  }

  @Test
  public void search_whenAdminParametersUsedButNotAdmin_shouldFail() throws Exception {
    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarQubeLastConnectionDateFrom", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter sonarQubeLastConnectionDateFrom requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarQubeLastConnectionDateTo", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter sonarQubeLastConnectionDateTo requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarLintLastConnectionDateFrom", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter sonarLintLastConnectionDateFrom requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
      .param("sonarLintLastConnectionDateTo", "2020-01-01T00:00:00+0100"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter sonarLintLastConnectionDateTo requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
        .param("externalIdentity", "externalIdentity"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter externalIdentity requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
        .param("groupId", "groupId"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter groupId requires Administer System permission.\"}"));

    mockMvc.perform(get(USER_ENDPOINT)
        .param("groupId!", "groupId"))
      .andExpectAll(
        status().isForbidden(),
        content().string("{\"message\":\"Parameter groupId! requires Administer System permission.\"}"));
  }

  @Test
  public void search_whenUserServiceReturnUsers_shouldReturnThem() throws Exception {
    UserInformation user1 = generateUserSearchResult("user1", true, true, false, 2, 3);
    UserInformation user2 = generateUserSearchResult("user2", true, false, false, 3, 0);
    UserInformation user3 = generateUserSearchResult("user3", true, false, true, 1, 1);
    UserInformation user4 = generateUserSearchResult("user4", false, true, false, 0, 0);
    List<UserInformation> users = List.of(user1, user2, user3, user4);
    SearchResults<UserInformation> searchResult = new SearchResults<>(users, users.size());
    when(userService.findUsers(any())).thenReturn(searchResult);
    List<UserRestResponse> restUserForAdmins = List.of(toRestUser(user1), toRestUser(user2), toRestUser(user3), toRestUser(user4));
    when(responseGenerator.toUsersForResponse(eq(searchResult.searchResults()), any())).thenReturn(new UsersSearchRestResponse(restUserForAdmins, new PageRestResponse(1, 50, 4)));
    userSession.logIn().setSystemAdministrator();

    MvcResult mvcResult = mockMvc.perform(get(USER_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    UsersSearchRestResponse actualUsersSearchRestResponse = gson.fromJson(mvcResult.getResponse().getContentAsString(), UsersSearchRestResponse.class);
    assertThat(actualUsersSearchRestResponse.users())
      .containsExactlyElementsOf(restUserForAdmins);
    assertThat(actualUsersSearchRestResponse.page().total()).isEqualTo(users.size());

  }

  static class RestUserDeserializer extends TypeAdapter<UserRestResponse> {

    @Override
    public void write(JsonWriter out, UserRestResponse value) {
      throw new IllegalStateException("not implemented");
    }

    @Override
    public UserRestResponse read(JsonReader reader) {
      return gson.fromJson(reader, UserRestResponseForAdmins.class);
    }
  }

  private UserInformation generateUserSearchResult(String id, boolean active, boolean local, boolean managed, int groupsCount, int tokensCount) {
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

    return new UserInformation(userDto, managed, Optional.of("avatar_" + id), groups, tokensCount);
  }

  private UserRestResponseForAdmins toRestUser(UserInformation userInformation) {
    return new UserRestResponseForAdmins(
      userInformation.userDto().getLogin(),
      userInformation.userDto().getLogin(),
      userInformation.userDto().getName(),
      userInformation.userDto().getEmail(),
      userInformation.userDto().isActive(),
      userInformation.userDto().isLocal(),
      userInformation.managed(),
      userInformation.userDto().getExternalLogin(),
      userInformation.userDto().getExternalIdentityProvider(),
      userInformation.userDto().getExternalId(),
      userInformation.avatar().orElse(""),
      formatDateTime(userInformation.userDto().getLastConnectionDate()),
      formatDateTime(userInformation.userDto().getLastSonarlintConnectionDate()),
      userInformation.userDto().getSortedScmAccounts());
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
    UserSessionRule userToDelete = userSession.logIn("userToDelete").setSystemAdministrator();

    mockMvc.perform(delete(USER_ENDPOINT + "/" + userToDelete.getUuid()))
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
    UserInformation user = generateUserSearchResult("user1", true, true, false, 2, 3);
    UserRestResponseForAdmins restUserForAdmins = toRestUser(user);
    when(userService.fetchUser("userLogin")).thenReturn(user);
    when(responseGenerator.toRestUser(user)).thenReturn(restUserForAdmins);
    MvcResult mvcResult = mockMvc.perform(get(USER_ENDPOINT + "/userLogin"))
      .andExpect(status().isOk())
      .andReturn();
    UserRestResponseForAdmins responseUser = gson.fromJson(mvcResult.getResponse().getContentAsString(), UserRestResponseForAdmins.class);
    assertThat(responseUser).isEqualTo(restUserForAdmins);
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
        content().json("{\"message\":\"Value {} for field login was rejected. Error: must not be null.\"}"));
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
        content().json("{\"message\":\"Value {} for field name was rejected. Error: must not be null.\"}"));
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
    UserInformation userInformation = generateUserSearchResult("1", true, true, false, 1, 2);
    UserDto userDto = userInformation.userDto();
    when(userService.createUser(any())).thenReturn(userInformation);
    when(responseGenerator.toRestUser(userInformation)).thenReturn(toRestUser(userInformation));

    MvcResult mvcResult = mockMvc.perform(
      post(USER_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(gson.toJson(new UserCreateRestRequest(
          userDto.getEmail(), userDto.isLocal(), userDto.getLogin(), userDto.getName(), "password", userDto.getSortedScmAccounts()))))
      .andExpect(status().isOk())
      .andReturn();
    UserRestResponseForAdmins responseUser = gson.fromJson(mvcResult.getResponse().getContentAsString(), UserRestResponseForAdmins.class);
    assertThat(responseUser).isEqualTo(toRestUser(userInformation));
  }

  @Test
  public void updateUser_whenUserDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(userService.updateUser(eq("userLogin"), any(UpdateUser.class))).thenThrow(new NotFoundException("Not found"));
    mockMvc.perform(patch(USER_ENDPOINT + "/userLogin")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Not found\"}"));
  }

  @Test
  public void updateUser_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
      patch(USER_ENDPOINT + "/userLogin")
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content("{}"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void updateUser_whenEmailIsProvided_shouldUpdateUserAndReturnUpdatedValue() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"email\":\"newemail@example.com\"}");
    assertThat(userUpdate.email()).isEqualTo("newemail@example.com");
    assertThat(userUpdate.name()).isNull();
    assertThat(userUpdate.scmAccounts()).isNull();
  }

  @Test
  public void updateUser_whenNameIsProvided_shouldUpdateUserAndReturnUpdatedValue() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"name\":\"new name\"}");
    assertThat(userUpdate.email()).isNull();
    assertThat(userUpdate.name()).isEqualTo("new name");
    assertThat(userUpdate.scmAccounts()).isNull();
  }

  @Test
  public void updateUser_whenScmAccountsAreProvided_shouldUpdateUserAndReturnUpdatedValue() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"scmAccounts\":[\"account1\",\"account2\"]}");
    assertThat(userUpdate.email()).isNull();
    assertThat(userUpdate.name()).isNull();
    assertThat(userUpdate.scmAccounts()).containsExactly("account1", "account2");
  }

  @Test
  public void updateUser_whenEmailIsInvalid_shouldReturnBadRequest() throws Exception {
    performPatchCallAndExpectBadRequest("{\"email\":\"notavalidemail\"}", "Value notavalidemail for field email was rejected. Error: must be a well-formed email address.");
  }

  @Test
  public void updateUser_whenEmailIsEmpty_shouldReturnBadRequest() throws Exception {
    performPatchCallAndExpectBadRequest("{\"email\":\"\"}", "Value  for field email was rejected. Error: size must be between 1 and 100.");
  }

  @Test
  public void updateUser_whenNameIsTooLong_shouldReturnBadRequest() throws Exception {
    String tooLong = "toolong".repeat(30);
    String payload = "{\"name\":\"" + tooLong + "\"}";
    String message = "Value " + tooLong + " for field name was rejected. Error: size must be between 0 and 200.";
    performPatchCallAndExpectBadRequest(payload, message);
  }

  @Test
  public void updateUser_whenLoginIsProvided_shouldUpdateLogin() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"login\":\"newLogin\"}");
    assertThat(userUpdate.login()).isEqualTo("newLogin");
  }

  @Test
  public void updateUser_whenExternalProviderIsProvided_shouldUpdate() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"externalProvider\":\"newExternalProvider\"}");
    assertThat(userUpdate.externalIdentityProvider()).isEqualTo("newExternalProvider");
  }

  @Test
  public void updateUser_whenExternalProviderLoginIsProvided_shouldUpdate() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"externalLogin\":\"newExternalProviderLogin\"}");
    assertThat(userUpdate.externalIdentityProviderLogin()).isEqualTo("newExternalProviderLogin");
  }

  @Test
  public void updateUser_whenExternalProviderIdIsProvided_shouldUpdate() throws Exception {
    UpdateUser userUpdate = performPatchCallAndVerifyResponse("{\"externalId\":\"newExternalProviderId12334\"}");
    assertThat(userUpdate.externalIdentityProviderId()).isEqualTo("newExternalProviderId12334");
  }

  @Test
  public void updateUser_whenLoginIsEmpty_shouldReturnBadRequest() throws Exception {
    performPatchCallAndExpectBadRequest("{\"login\":\"\"}", "Value  for field login was rejected. Error: size must be between 2 and 100.");
  }

  private void performPatchCallAndExpectBadRequest(String payload, String expectedMessage) throws Exception {
    userSession.logIn().setSystemAdministrator();

    MvcResult mvcResult = mockMvc.perform(patch(USER_ENDPOINT + "/userLogin")
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content(payload))
      .andExpect(
        status().isBadRequest())
      .andReturn();

    RestError error = gson.fromJson(mvcResult.getResponse().getContentAsString(), RestError.class);
    assertThat(error.message()).isEqualTo(expectedMessage);
  }

  private UpdateUser performPatchCallAndVerifyResponse(String payload) throws Exception {
    userSession.logIn().setSystemAdministrator();
    UserInformation mock = mock();
    when(userService.fetchUser("userUuid")).thenReturn(mock);
    UserInformation userInformation = generateUserSearchResult("1", true, true, false, 1, 2);

    when(userService.updateUser(eq("userUuid"), any())).thenReturn(userInformation);
    when(responseGenerator.toRestUser(userInformation)).thenReturn(toRestUser(userInformation));

    MvcResult mvcResult = mockMvc.perform(patch(USER_ENDPOINT + "/userUuid")
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content(payload))
      .andExpect(
        status().isOk())
      .andReturn();

    UserRestResponseForAdmins responseUser = gson.fromJson(mvcResult.getResponse().getContentAsString(), UserRestResponseForAdmins.class);
    assertThat(responseUser).isEqualTo(toRestUser(userInformation));

    ArgumentCaptor<UpdateUser> updateUserCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userService).updateUser(eq("userUuid"), updateUserCaptor.capture());
    return updateUserCaptor.getValue();
  }
}
