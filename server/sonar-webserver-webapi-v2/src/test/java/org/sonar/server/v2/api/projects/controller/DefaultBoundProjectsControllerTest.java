/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.projects.controller;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.project.ImportProjectRequest;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.project.ImportedProject;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.v2.WebApiEndpoints.BOUND_PROJECTS_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultBoundProjectsControllerTest {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String PROJECT_ALM_SETTING_UUID = "project-alm-setting-uuid";
  private static final String DOP_REPOSITORY_ID = "dop-repository-id";
  private static final String DOP_PROJECT_ID = "dop-project-id";
  private static final String PROJECT_KEY = "project-key";
  private static final String PROJECT_NAME = "project-name";
  private static final String ALM_SETTING_ID = "alm-setting-id";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final ImportProjectService importProjectService = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(
    new DefaultBoundProjectsController(
      userSession, importProjectService));

  @Test
  void createBoundProject_whenUserDoesntHaveCreateProjectPermission_returnsForbidden() throws Exception {
    userSession.logIn();
    mockMvc.perform(
      post(BOUND_PROJECTS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "projectKey": "project-key",
                  "projectName": "project-name",
                  "devOpsPlatformSettingId": "alm-setting-id",
                  "repositoryIdentifier": "repository-id",
                  "monorepo": true
                }
          """)

    )
      .andExpect(status().isForbidden());
  }

  @Test
  void createdBoundProject_whenImportProjectServiceThrowsIllegalArgumentExceptions_returnsBadRequest() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);
    when(importProjectService.importProject(any()))
      .thenThrow(new IllegalArgumentException("Error message"));
    mockMvc.perform(
      post(BOUND_PROJECTS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "projectKey": "project-key",
                  "projectName": "project-name",
                  "devOpsPlatformSettingId": "alm-setting-id",
                  "repositoryIdentifier": "repository-id",
                  "monorepo": true
                }
          """))
      .andExpectAll(
        status().isBadRequest(),
        content().json("""
          {
            "message": "Error message"
          }
          """));
  }

  @Test
  void createBoundProject_whenProjectIsCreatedSuccessfully_returnResponse() throws Exception {
    userSession.logIn().addPermission(PROVISION_PROJECTS);

    ProjectDto projectDto = mock(ProjectDto.class);
    when(projectDto.getUuid()).thenReturn(PROJECT_UUID);

    ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    when(projectAlmSettingDto.getUuid()).thenReturn(PROJECT_ALM_SETTING_UUID);

    when(importProjectService.importProject(new ImportProjectRequest(
      PROJECT_KEY,
      PROJECT_NAME,
      ALM_SETTING_ID,
      DOP_REPOSITORY_ID,
      DOP_PROJECT_ID,
      "NUMBER_OF_DAYS",
      "10",
      true)))
      .thenReturn(new ImportedProject(
        projectDto,
        projectAlmSettingDto));

    mockMvc.perform(
      post(BOUND_PROJECTS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "projectKey": "project-key",
                  "projectName": "project-name",
                  "devOpsPlatformSettingId": "alm-setting-id",
                  "repositoryIdentifier": "dop-repository-id",
                  "projectIdentifier": "dop-project-id",
                  "newCodeDefinitionType": "NUMBER_OF_DAYS",
                  "newCodeDefinitionValue": "10",
                  "monorepo": true
                }
          """))
      .andExpectAll(
        status().isCreated(),
        content().json("""
          {
            "projectId": "project-uuid",
            "bindingId": "project-alm-setting-uuid"
          }
          """));
  }
}
