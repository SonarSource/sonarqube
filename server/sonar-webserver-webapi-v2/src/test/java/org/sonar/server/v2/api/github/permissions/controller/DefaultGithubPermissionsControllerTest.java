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
package org.sonar.server.v2.api.github.permissions.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.server.common.github.permissions.GithubPermissionsMapping;
import org.sonar.server.common.github.permissions.GithubPermissionsMappingService;
import org.sonar.server.common.github.permissions.PermissionMappingChange;
import org.sonar.server.common.github.permissions.SonarqubePermissions;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.github.permissions.model.RestGithubPermissionsMapping;
import org.sonar.server.v2.api.github.permissions.response.GithubPermissionsMappingRestResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.GITHUB_PERMISSIONS_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultGithubPermissionsControllerTest {

  public static final String GITHUB_ROLE = "role1";
  private static final Gson gson = new GsonBuilder().create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final GithubPermissionsMappingService githubPermissionsMappingService = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultGithubPermissionsController(userSession, githubPermissionsMappingService));

  @Test
  public void fetchMapping_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(get(GITHUB_PERMISSIONS_ENDPOINT))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchMapping_whenMappingSet_shouldReturnMapping() throws Exception {
    userSession.logIn().setSystemAdministrator();

    List<GithubPermissionsMapping> mapping = List.of(
      new GithubPermissionsMapping(GITHUB_ROLE, false, new SonarqubePermissions(true, false, true, false, true, false)),
      new GithubPermissionsMapping("role2", true, new SonarqubePermissions(false, true, false, true, false, true)));
    when(githubPermissionsMappingService.getPermissionsMapping()).thenReturn(mapping);

    MvcResult mvcResult = mockMvc.perform(get(GITHUB_PERMISSIONS_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GithubPermissionsMappingRestResponse response = gson.fromJson(mvcResult.getResponse().getContentAsString(), GithubPermissionsMappingRestResponse.class);
    assertThat(response.githubPermissionsMappings()).isEqualTo(toRestResources(mapping));
  }

  private static List<RestGithubPermissionsMapping> toRestResources(List<GithubPermissionsMapping> permissionsMapping) {
    return permissionsMapping.stream()
      .map(DefaultGithubPermissionsControllerTest::toRestGithubPermissionMapping)
      .toList();
  }

  private static RestGithubPermissionsMapping toRestGithubPermissionMapping(GithubPermissionsMapping permissionMapping) {
    return new RestGithubPermissionsMapping(permissionMapping.roleName(), permissionMapping.roleName(), permissionMapping.isBaseRole(), permissionMapping.permissions());
  }

  @Test
  public void updateMapping_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
        patch(GITHUB_PERMISSIONS_ENDPOINT + "/" + GITHUB_ROLE)
          .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
          .content("""
            {
                "user": true,
                "codeViewer": false,
                "admin": true
            }
            """))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void updateMapping_shouldUpdateMapping() throws Exception {
    userSession.logIn().setSystemAdministrator();
    GithubPermissionsMapping updatedRolePermissions = new GithubPermissionsMapping(GITHUB_ROLE, false, new SonarqubePermissions(true, false, false, true, true, false));

    when(githubPermissionsMappingService.getPermissionsMappingForGithubRole(GITHUB_ROLE)).thenReturn(updatedRolePermissions);

    MvcResult mvcResult = mockMvc.perform(
        patch(GITHUB_PERMISSIONS_ENDPOINT + "/" + GITHUB_ROLE)
          .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
          .content("""
            {
              "permissions": {
                "user": true,
                "codeViewer": false,
                "admin": true
              }
            }
            """))
      .andExpect(status().isOk())
      .andReturn();

    RestGithubPermissionsMapping response = gson.fromJson(mvcResult.getResponse().getContentAsString(), RestGithubPermissionsMapping.class);

    RestGithubPermissionsMapping expectedResponse = new RestGithubPermissionsMapping(GITHUB_ROLE, GITHUB_ROLE, false, new SonarqubePermissions(true, false, false, true, true, false));
    assertThat(response).isEqualTo(expectedResponse);

    ArgumentCaptor<Set<PermissionMappingChange>> permissionMappingChangesCaptor = ArgumentCaptor.forClass(Set.class);
    verify(githubPermissionsMappingService).updatePermissionsMappings(permissionMappingChangesCaptor.capture());
    assertThat(permissionMappingChangesCaptor.getValue())
      .containsExactlyInAnyOrder(
        new PermissionMappingChange(GITHUB_ROLE, "codeviewer", Operation.REMOVE),
        new PermissionMappingChange(GITHUB_ROLE, "user", Operation.ADD),
        new PermissionMappingChange(GITHUB_ROLE, "admin", Operation.ADD)
      );
  }

}
