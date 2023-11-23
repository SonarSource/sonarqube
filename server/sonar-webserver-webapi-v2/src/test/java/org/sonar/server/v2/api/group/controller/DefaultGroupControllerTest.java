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
package org.sonar.server.v2.api.group.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.group.service.GroupInformation;
import org.sonar.server.common.group.service.GroupSearchRequest;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.group.response.GroupsSearchRestResponse;
import org.sonar.server.v2.api.group.response.GroupRestResponse;
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
import static org.sonar.server.v2.WebApiEndpoints.GROUPS_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_INDEX;
import static org.sonar.server.v2.api.model.RestPage.DEFAULT_PAGE_SIZE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DefaultGroupControllerTest {

  private static final String GROUP_UUID = "1234";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final GroupService groupService = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final ManagedInstanceChecker managedInstanceChecker = mock();
  private final MockMvc mockMvc = ControllerTester
    .getMockMvc(new DefaultGroupController(userSession, dbClient, groupService, managedInstanceChecker));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  public void fetchGroup_whenNotAnAdmin_shouldThrow() throws Exception {
    userSession.logIn();
    mockMvc.perform(get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchGroup_whenGroupExists_returnsTheGroup() throws Exception {

    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");
    GroupInformation groupInformation = new GroupInformation(groupDto, false, false);

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupInformation));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "1234",
            "name": "name",
            "description": "description",
            "managed": false,
            "default": false
          }
          """));
  }

  @Test
  public void fetchGroup_whenGroupIsManagedOrDefault_returnsCorrectValues() throws Exception {

    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");
    GroupInformation groupInformation = new GroupInformation(groupDto, true, true);

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupInformation));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "1234",
            "name": "name",
            "description": "description",
            "managed": true,
            "default": true
          }
          """));
  }

  @Test
  public void fetchGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
      get(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
      get(GROUPS_ENDPOINT + "/" + GROUP_UUID).content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

  @Test
  public void deleteGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
      delete(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void deleteGroup_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
      delete(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void deleteGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
      delete(GROUPS_ENDPOINT + "/" + GROUP_UUID).content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

  @Test
  public void deleteGroup_whenGroupExists_shouldDeleteAndReturn204() throws Exception {
    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");
    GroupInformation groupInformation = new GroupInformation(groupDto, false, false);

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.of(groupInformation));

    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(
      delete(GROUPS_ENDPOINT + "/" + GROUP_UUID))
      .andExpectAll(
        status().isNoContent(),
        content().string(""));
  }

  @Test
  public void patchGroup_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
      patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content("{}"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void patchGroup_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
      patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content("{}"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void patchGroup_whenGroupDoesntExist_shouldReturnNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.findGroupByUuid(dbSession, GROUP_UUID)).thenReturn(Optional.empty());
    mockMvc.perform(
      patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content("{}"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Group '1234' not found\"}"));
  }

  @Test
  public void patchGroup_whenGroupExists_shouldPatchAndReturnNewGroup() throws Exception {
    patchGroupAndAssertResponse("newName", "newDescription");
  }

  @Test
  public void patchGroup_whenGroupExistsAndRemovingDescription_shouldPatchAndReturnNewGroup() throws Exception {
    patchGroupAndAssertResponse("newName", null);
  }

  @Test
  public void patchGroup_whenGroupExistsAndIdempotent_shouldPatch() throws Exception {
    patchGroupAndAssertResponse("newName", "newDescription");
    patchGroupAndAssertResponse("newName", "newDescription");
  }

  private void patchGroupAndAssertResponse(@Nullable String newName, @Nullable String newDescription) throws Exception {
    userSession.logIn().setSystemAdministrator();
    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");
    GroupInformation groupInformation = new GroupInformation(groupDto, false, false);

    GroupDto newDto = new GroupDto().setUuid(GROUP_UUID).setName(newName).setDescription(newDescription);
    GroupInformation newGroupInformation = new GroupInformation(newDto, false, false);

    when(groupService.findGroupByUuid(dbSession, GROUP_UUID))
      .thenReturn(Optional.of(groupInformation));
    when(groupService.updateGroup(dbSession, groupDto, newName, newDescription)).thenReturn(newGroupInformation);

    MvcResult mvcResult = mockMvc.perform(
        patch(GROUPS_ENDPOINT + "/" + GROUP_UUID).contentType(JSON_MERGE_PATCH_CONTENT_TYPE).content(
          """
            {
              "name": "%s",
              "description": %s
            }
            """.formatted(newName, newDescription == null ? "null" : "\"" + newDescription + "\"")))
      .andExpect(status().isOk())
      .andReturn();

    GroupRestResponse groupRestResponse = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), GroupRestResponse.class);
    assertThat(groupRestResponse.id()).isEqualTo(GROUP_UUID);
    assertThat(groupRestResponse.name()).isEqualTo(newName);
    assertThat(groupRestResponse.description()).isEqualTo(newDescription);
  }

  @Test
  public void create_whenInstanceIsManaged_shouldReturnException() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(BadRequestException.create("the instance is managed")).when(managedInstanceChecker).throwIfInstanceIsManaged();
    mockMvc.perform(
      post(GROUPS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
            "name": "name",
            "description": "description"
          }
          """))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"the instance is managed\"}"));
  }

  @Test
  public void create_whenCallersIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
      post(GROUPS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
            "name": "name",
            "description": "description"
          }
          """))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void create_whenUserIsAnAdmin_shouldReturnCreatedGroup() throws Exception {
    userSession.logIn().setSystemAdministrator();

    GroupDto groupDto = new GroupDto().setUuid(GROUP_UUID).setName("name").setDescription("description");
    GroupInformation groupInformation = new GroupInformation(groupDto, false, false);

    when(groupService.createGroup(dbSession, "name", "description")).thenReturn(groupInformation);

    mockMvc.perform(
      post(GROUPS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
            "name": "name",
            "description": "description"
          }
          """))
      .andExpectAll(
        status().isCreated(),
        content().json("""
          {
            "id": "1234",
            "name": "name",
            "description": "description",
            "managed": false,
            "default": false
          }
          """));
  }

  @Test
  public void create_whenNameIsTooLong_returnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    String tooLongName = "a".repeat(501);
    mockMvc.perform(
      post(GROUPS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
            "name": "%s",
            "description": "description"
          }
          """.formatted(tooLongName)))
      .andExpectAll(
        status().isBadRequest());
  }

  @Test
  public void create_whenDescriptionIsTooLong_returnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    String tooLongDescription = "a".repeat(201);
    mockMvc.perform(
      post(GROUPS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
            "name": "name",
            "description": "%s"
          }
          """.formatted(tooLongDescription)))
      .andExpectAll(
        status().isBadRequest());
  }

  @Test
  public void search_whenCallerIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();
    mockMvc.perform(
      get(GROUPS_ENDPOINT))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void search_whenNoParameters_shouldUseDefaultAndForwardToUserService() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.search(eq(dbSession), any())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc.perform(get(GROUPS_ENDPOINT)).andExpect(status().isOk());

    ArgumentCaptor<GroupSearchRequest> requestCaptor = ArgumentCaptor.forClass(GroupSearchRequest.class);
    verify(groupService).search(eq(dbSession), requestCaptor.capture());
    assertThat(requestCaptor.getValue().pageSize()).hasToString(DEFAULT_PAGE_SIZE);
    assertThat(requestCaptor.getValue().page()).hasToString(DEFAULT_PAGE_INDEX);
    assertThat(requestCaptor.getValue().managed()).isNull();
    assertThat(requestCaptor.getValue().query()).isNull();
  }

  @Test
  public void search_whenParametersUsed_shouldForwardWithParameters() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(groupService.search(eq(dbSession), any())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc.perform(get(GROUPS_ENDPOINT)
      .param("managed", "true")
      .param("q", "q")
      .param("pageSize", "100")
      .param("pageIndex", "2"))
      .andExpect(status().isOk());

    ArgumentCaptor<GroupSearchRequest> requestCaptor = ArgumentCaptor.forClass(GroupSearchRequest.class);
    verify(groupService).search(eq(dbSession), requestCaptor.capture());
    assertThat(requestCaptor.getValue().pageSize()).isEqualTo(100);
    assertThat(requestCaptor.getValue().page()).isEqualTo(2);
    assertThat(requestCaptor.getValue().managed()).isTrue();
    assertThat(requestCaptor.getValue().query()).isEqualTo("q");
  }

  @Test
  public void search_whenGroupServiceReturnUsers_shouldReturnThem() throws Exception {
    userSession.logIn().setSystemAdministrator();

    GroupInformation group1 = generateGroupSearchResult("group1", true, true);
    GroupInformation group2 = generateGroupSearchResult("user2", false, false);
    GroupInformation group3 = generateGroupSearchResult("user3", true, false);
    List<GroupInformation> groups = List.of(group1, group2, group3);
    SearchResults<GroupInformation> searchResult = new SearchResults<>(groups, groups.size());
    when(groupService.search(eq(dbSession), any())).thenReturn(searchResult);

    MvcResult mvcResult = mockMvc.perform(get(GROUPS_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GroupsSearchRestResponse actualGroupsSearchRestResponse = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), GroupsSearchRestResponse.class);
    Map<String, GroupRestResponse> groupIdToGroupResponse = actualGroupsSearchRestResponse.groups().stream()
      .collect(Collectors.toMap(GroupRestResponse::id, Function.identity()));

    assertResponseContains(groupIdToGroupResponse, group1);
    assertResponseContains(groupIdToGroupResponse, group2);
    assertResponseContains(groupIdToGroupResponse, group3);

    assertThat(actualGroupsSearchRestResponse.page().pageIndex()).hasToString(DEFAULT_PAGE_INDEX);
    assertThat(actualGroupsSearchRestResponse.page().pageSize()).hasToString(DEFAULT_PAGE_SIZE);
    assertThat(actualGroupsSearchRestResponse.page().total()).isEqualTo(groups.size());

  }

  private void assertResponseContains(Map<String, GroupRestResponse> groupIdToGroupResponse, GroupInformation expectedGroup) {
    GroupRestResponse restGroup = groupIdToGroupResponse.get(expectedGroup.groupDto().getUuid());
    assertThat(restGroup).isNotNull();
    assertThat(restGroup.name()).isEqualTo(expectedGroup.groupDto().getName());
    assertThat(restGroup.description()).isEqualTo(expectedGroup.groupDto().getDescription());
    assertThat(restGroup.managed()).isEqualTo(expectedGroup.isManaged());
    assertThat(restGroup.isDefault()).isEqualTo(expectedGroup.isDefault());
  }

  private GroupInformation generateGroupSearchResult(String id, boolean managed, boolean isDefault) {
    GroupDto groupDto = new GroupDto()
      .setUuid(id)
      .setName("name_" + id)
      .setDescription("description_" + id);
    return new GroupInformation(groupDto, managed, isDefault);
  }
}
