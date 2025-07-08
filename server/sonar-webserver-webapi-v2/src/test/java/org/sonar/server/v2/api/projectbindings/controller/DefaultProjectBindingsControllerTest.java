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
package org.sonar.server.v2.api.projectbindings.controller;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.projectbindings.service.ProjectBindingInformation;
import org.sonar.server.common.projectbindings.service.ProjectBindingsSearchRequest;
import org.sonar.server.common.projectbindings.service.ProjectBindingsService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.server.v2.WebApiEndpoints.PROJECT_BINDINGS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultProjectBindingsControllerTest {

  public static final String UUID = "uuid";
  private static final String PROJECT_UUID = "projectUuid";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final ProjectBindingsService projectBindingsService = mock();

  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultProjectBindingsController(userSession, projectBindingsService));

  @Test
  void getProjectBinding_whenNoProjectBinding_returnsNotFound() throws Exception {
    userSession.logIn();
    when(projectBindingsService.findProjectBindingByUuid(UUID)).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT + "/uuid"))
      .andExpectAll(
        status().isNotFound(),
        content().json("""
          {
            "message": "You do not have access to this resource or it does not exist."
          }
          """));
  }

  @Test
  void getProjectBinding_whenNoProject_returnsServerError() throws Exception {
    userSession.logIn();
    ProjectAlmSettingDto projectAlmSettingDto = mock();
    when(projectAlmSettingDto.getProjectUuid()).thenReturn(PROJECT_UUID);
    when(projectBindingsService.findProjectBindingByUuid(UUID)).thenReturn(Optional.of(projectAlmSettingDto));
    when(projectBindingsService.findProjectFromBinding(projectAlmSettingDto)).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT + "/uuid"))
      .andExpectAll(
        status().isInternalServerError(),
        content().json("""
          {
            "message": "Project (uuid 'projectUuid') not found for binding 'uuid'"
          }
          """));
  }

  @Test
  void getProjectBinding_whenUserDoesntHaveProjectAdminPermissions_returnsNotFound() throws Exception {
    userSession.logIn();
    ProjectAlmSettingDto projectAlmSettingDto = mock();
    when(projectBindingsService.findProjectBindingByUuid(UUID)).thenReturn(Optional.of(projectAlmSettingDto));
    when(projectBindingsService.findProjectFromBinding(projectAlmSettingDto)).thenReturn(Optional.ofNullable(mock(ProjectDto.class)));

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT + "/uuid"))
      .andExpectAll(
        status().isNotFound(),
        content().json("""
          {
            "message": "You do not have access to this resource or it does not exist."
          }
          """));
  }

  @Test
  void getProjectBinding_whenProjectBindingAndPermissions_returnsIt() throws Exception {
    ProjectAlmSettingDto projectAlmSettingDto = mockProjectAlmSettingDto("1");

    ProjectDto projectDto = mock();
    when(projectDto.getKey()).thenReturn("projectKey_1");

    userSession.logIn().addProjectPermission(ADMIN, projectDto);
    when(projectBindingsService.findProjectBindingByUuid(UUID)).thenReturn(Optional.of(projectAlmSettingDto));
    when(projectBindingsService.findProjectFromBinding(projectAlmSettingDto)).thenReturn(Optional.ofNullable(projectDto));

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT + "/uuid"))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "uuid_1",
            "devOpsPlatformSettingId": "almSettingUuid_1",
            "projectId": "projectUuid_1",
            "projectKey": "projectKey_1",
            "repository": "almRepo_1",
            "slug": "almSlug_1"
          }
          """));
  }

  @Test
  void searchProjectBindings_whenUserDoesntHaveProjectProvisionPermission_returnsForbidden() throws Exception {
    userSession.logIn();

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repository", "repo")
        .param("dopSettingId", "id"))
      .andExpectAll(
        status().isForbidden(),
        content().json("""
          {
            "message": "Insufficient privileges"
          }
          """));

  }

  @Test
  void searchProjectBindings_whenParametersUsed_shouldForwardWithParameters() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);
    when(projectBindingsService.findProjectBindingsByRequest(any())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repository", "repo")
        .param("dopSettingId", "id")
        .param("pageIndex", "12")
        .param("pageSize", "42"))
      .andExpect(status().isOk());

    ArgumentCaptor<ProjectBindingsSearchRequest> requestCaptor = ArgumentCaptor.forClass(ProjectBindingsSearchRequest.class);
    verify(projectBindingsService).findProjectBindingsByRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().repository()).isEqualTo("repo");
    assertThat(requestCaptor.getValue().dopSettingId()).isEqualTo("id");
    assertThat(requestCaptor.getValue().page()).isEqualTo(12);
    assertThat(requestCaptor.getValue().pageSize()).isEqualTo(42);
  }

  @Test
  void searchProjectBindings_whenResultsFound_shouldReturnsThem() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);

    ProjectBindingInformation dto1 = projectBindingInformation("1");
    ProjectBindingInformation dto2 = projectBindingInformation("2");

    List<ProjectBindingInformation> expectedResults = List.of(dto1, dto2);
    when(projectBindingsService.findProjectBindingsByRequest(any())).thenReturn(new SearchResults<>(expectedResults, expectedResults.size()));

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repository", "whatever")
        .param("dopSettingId", "doesntmatter")
        .param("pageIndex", "1")
        .param("pageSize", "100"))
      .andExpectAll(
        status().isOk(),
        content().json("""
            {
              "projectBindings": [
                {
                  "id": "uuid_1",
                  "devOpsPlatformSettingId": "almSettingUuid_1",
                  "projectId": "projectUuid_1",
                  "projectKey": "projectKey_1",
                  "repository": "almRepo_1",
                  "slug": "almSlug_1"
                },
                {
                  "id": "uuid_2",
                  "devOpsPlatformSettingId": "almSettingUuid_2",
                  "projectId": "projectUuid_2",
                  "projectKey": "projectKey_2",
                  "repository": "almRepo_2",
                  "slug": "almSlug_2"
                }
              ],
              "page": {
                "pageIndex": 1,
                "pageSize": 100,
                "total": 2
              }
            }
          """));
  }

  @Test
  void searchProjectBindings_whenRepositoryUrlUsed_shouldForwardRepositoryUrlParameter() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);
    when(projectBindingsService.findProjectBindingsByRequest(any())).thenReturn(new SearchResults<>(List.of(), 0));

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repositoryUrl", "https://github.com/org/repo")
        .param("pageIndex", "1")
        .param("pageSize", "50"))
      .andExpect(status().isOk());

    ArgumentCaptor<ProjectBindingsSearchRequest> requestCaptor = ArgumentCaptor.forClass(ProjectBindingsSearchRequest.class);
    verify(projectBindingsService).findProjectBindingsByRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue().repositoryUrl()).isEqualTo("https://github.com/org/repo");
    assertThat(requestCaptor.getValue().repository()).isNull();
    assertThat(requestCaptor.getValue().dopSettingId()).isNull();
    assertThat(requestCaptor.getValue().page()).isEqualTo(1);
    assertThat(requestCaptor.getValue().pageSize()).isEqualTo(50);
  }

  @Test
  void searchProjectBindings_whenRepositoryUrlWithRepositoryParameter_shouldReturnBadRequest() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repositoryUrl", "https://github.com/org/repo")
        .param("repository", "repo"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void searchProjectBindings_whenRepositoryUrlWithDopSettingIdParameter_shouldReturnBadRequest() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repositoryUrl", "https://github.com/org/repo")
        .param("dopSettingId", "setting123"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void searchProjectBindings_whenRepositoryUrlReturnsResults_shouldReturnThem() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);

    ProjectBindingInformation dto1 = projectBindingInformation("1");
    List<ProjectBindingInformation> expectedResults = List.of(dto1);
    when(projectBindingsService.findProjectBindingsByRequest(any())).thenReturn(new SearchResults<>(expectedResults, expectedResults.size()));

    mockMvc
      .perform(get(PROJECT_BINDINGS_ENDPOINT)
        .param("repositoryUrl", "https://github.com/org/repo")
        .param("pageIndex", "1")
        .param("pageSize", "50"))
      .andExpectAll(
        status().isOk(),
        content().json("""
            {
              "projectBindings": [
                {
                  "id": "uuid_1",
                  "devOpsPlatformSettingId": "almSettingUuid_1",
                  "projectId": "projectUuid_1",
                  "projectKey": "projectKey_1",
                  "repository": "almRepo_1",
                  "slug": "almSlug_1"
                }
              ],
              "page": {
                "pageIndex": 1,
                "pageSize": 50,
                "total": 1
              }
            }
          """));
  }

  private static ProjectAlmSettingDto mockProjectAlmSettingDto(String i) {
    ProjectAlmSettingDto dto = mock();
    when(dto.getUuid()).thenReturn("uuid_" + i);
    when(dto.getAlmSettingUuid()).thenReturn("almSettingUuid_" + i);
    when(dto.getProjectUuid()).thenReturn("projectUuid_" + i);
    when(dto.getAlmRepo()).thenReturn("almRepo_" + i);
    when(dto.getAlmSlug()).thenReturn("almSlug_" + i);
    return dto;
  }

  private static ProjectBindingInformation projectBindingInformation(String i) {
    return new ProjectBindingInformation("uuid_" + i,
      "almSettingUuid_" + i,
      "projectUuid_" + i,
      "projectKey_" + i,
      "almRepo_" + i,
      "almSlug_" + i);
  }

}
