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
package org.sonar.server.v2.api.membership.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.group.service.GroupMembershipSearchRequest;
import org.sonar.server.common.group.service.GroupMembershipService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.organization.OrganizationService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.membership.response.GroupsMembershipSearchRestResponse;
import org.sonar.server.v2.api.membership.response.GroupMembershipRestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.GROUP_MEMBERSHIPS_ENDPOINT;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_INDEX;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_SIZE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultGroupMembershipControllerTest {
  private static final String GROUP_UUID = "1234";
  private static final String GROUP_MEMBERSHIP_UUID = "1234";
  private static final String USER_UUID = "abcd";
  private static final String CREATE_PAYLOAD = """
    {
      "userId": "%s",
      "groupId": "%s"
    }
    """.formatted(USER_UUID, GROUP_UUID);

  private static final Gson GSON = new GsonBuilder().create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final GroupMembershipService groupMembershipService = mock();
  private final ManagedInstanceChecker managedInstanceChecker = mock();
  private final OrganizationService organizationService = mock();

  private final MockMvc mockMvc = ControllerTester
    .getMockMvc(new DefaultGroupMembershipController(userSession, groupMembershipService, managedInstanceChecker, organizationService));

  @Test
  public void create_whenCallersIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
        post(GROUP_MEMBERSHIPS_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(CREATE_PAYLOAD))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void create_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
        post(GROUP_MEMBERSHIPS_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(CREATE_PAYLOAD))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void create_whenUserIsAnAdmin_shouldReturnCreatedGroup() throws Exception {
    userSession.logIn().setSystemAdministrator();

    UserGroupDto userGroupDto = new UserGroupDto().setUuid(GROUP_MEMBERSHIP_UUID).setGroupUuid(GROUP_UUID).setUserUuid(USER_UUID);

    when(groupMembershipService.addMembership(GROUP_UUID, USER_UUID)).thenReturn(userGroupDto);

    mockMvc.perform(
        post(GROUP_MEMBERSHIPS_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(CREATE_PAYLOAD))
      .andExpectAll(
        status().isCreated(),
        content().json("""
              {
                "id": "%s",
                "userId": "%s",
                "groupId": "%s"
              }
              
          """.formatted(GROUP_MEMBERSHIP_UUID, USER_UUID, GROUP_UUID)));
  }

  @Test
  public void delete_whenCallersIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
        delete(GROUP_MEMBERSHIPS_ENDPOINT + "/" + GROUP_MEMBERSHIP_UUID)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(CREATE_PAYLOAD))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void delete_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
        delete(GROUP_MEMBERSHIPS_ENDPOINT + "/" + GROUP_MEMBERSHIP_UUID)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(CREATE_PAYLOAD))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void delete_whenUserIsAnAdmin_shouldDelete() throws Exception {
    userSession.logIn().setSystemAdministrator();
    UserGroupDto userGroupDto = new UserGroupDto().setUuid(GROUP_MEMBERSHIP_UUID).setGroupUuid(GROUP_UUID).setUserUuid(USER_UUID);

    mockMvc.perform(
        delete(GROUP_MEMBERSHIPS_ENDPOINT + "/" + GROUP_MEMBERSHIP_UUID)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content(CREATE_PAYLOAD))
      .andExpectAll(
        status().isNoContent(),
        content().string("")
      );

    verify(groupMembershipService).removeMembership(GROUP_MEMBERSHIP_UUID);
  }

  @Test
  public void search_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
        get(GROUP_MEMBERSHIPS_ENDPOINT))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void search_whenNoParameters_shouldUseDefaultAndForwardToGroupMembershipService() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupMembershipService.searchMembers(any(), anyInt(), anyInt())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc.perform(get(GROUP_MEMBERSHIPS_ENDPOINT)).andExpect(status().isOk());

    verify(groupMembershipService).searchMembers(new GroupMembershipSearchRequest(null, null), Integer.parseInt(DEFAULT_PAGE_INDEX), Integer.parseInt(DEFAULT_PAGE_SIZE));
  }

  @Test
  public void search_whenParametersUsed_shouldForwardWithParameters() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupMembershipService.searchMembers(any(), anyInt(), anyInt())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc.perform(get(GROUP_MEMBERSHIPS_ENDPOINT)
        .param("userId", USER_UUID)
        .param("groupId", GROUP_UUID)
        .param("pageSize", "100")
        .param("pageIndex", "2"))
      .andExpect(status().isOk());

    verify(groupMembershipService).searchMembers(new GroupMembershipSearchRequest(GROUP_UUID, USER_UUID), 2, 100);
  }

  @Test
  public void search_whenGroupMembershipServiceReturnGroupMemberships_shouldReturnThem() throws Exception {
    userSession.logIn().setSystemAdministrator();

    UserGroupDto userGroupDto1 = generateUserGroupDto("1");
    UserGroupDto userGroupDto2 = generateUserGroupDto("2");
    UserGroupDto userGroupDto3 = generateUserGroupDto("3");
    List<UserGroupDto> groups = List.of(userGroupDto1, userGroupDto2, userGroupDto3);
    SearchResults<UserGroupDto> searchResult = new SearchResults<>(groups, groups.size());
    when(groupMembershipService.searchMembers(any(), anyInt(), anyInt())).thenReturn(searchResult);

    MvcResult mvcResult = mockMvc.perform(get(GROUP_MEMBERSHIPS_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GroupsMembershipSearchRestResponse actualGroupsSearchRestResponse = GSON.fromJson(mvcResult.getResponse().getContentAsString(), GroupsMembershipSearchRestResponse.class);

    Map<String, GroupMembershipRestResponse> groupIdToGroupResponse = actualGroupsSearchRestResponse.groupMemberships().stream()
      .collect(toMap(GroupMembershipRestResponse::id, identity()));
    assertResponseContains(groupIdToGroupResponse, userGroupDto1);
    assertResponseContains(groupIdToGroupResponse, userGroupDto2);
    assertResponseContains(groupIdToGroupResponse, userGroupDto3);

    assertThat(actualGroupsSearchRestResponse.page().pageIndex()).hasToString(DEFAULT_PAGE_INDEX);
    assertThat(actualGroupsSearchRestResponse.page().pageSize()).hasToString(DEFAULT_PAGE_SIZE);
    assertThat(actualGroupsSearchRestResponse.page().total()).isEqualTo(groups.size());

  }

  private void assertResponseContains(Map<String, GroupMembershipRestResponse> groupIdToGroupResponse, UserGroupDto expectedUserGroupDto) {
    GroupMembershipRestResponse restGroupMembership = groupIdToGroupResponse.get(expectedUserGroupDto.getUuid());
    assertThat(restGroupMembership).isNotNull();
    assertThat(restGroupMembership.groupId()).isEqualTo(expectedUserGroupDto.getGroupUuid());
    assertThat(restGroupMembership.userId()).isEqualTo(expectedUserGroupDto.getUserUuid());
  }

  private UserGroupDto generateUserGroupDto(String id) {
    return new UserGroupDto().setUuid(id).setGroupUuid("uuid_"+id).setUserUuid("user_"+id);
  }
}
