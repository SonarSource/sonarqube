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
package org.sonar.server.v2.api.gitlab.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.UpdatedValue;
import org.sonar.server.common.gitlab.config.GitlabConfiguration;
import org.sonar.server.common.gitlab.config.GitlabConfigurationService;
import org.sonar.server.common.gitlab.config.UpdateGitlabConfigurationRequest;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.gitlab.config.controller.DefaultGitlabConfigurationController;
import org.sonar.server.v2.api.gitlab.config.resource.GitlabConfigurationResource;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationSearchRestResponse;
import org.sonar.server.v2.api.model.ProvisioningType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.common.gitlab.config.ProvisioningType.AUTO_PROVISIONING;
import static org.sonar.server.common.gitlab.config.ProvisioningType.JIT;
import static org.sonar.server.v2.WebApiEndpoints.GITLAB_CONFIGURATION_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultGitlabConfigurationControllerTest {
  private static final Gson GSON = new GsonBuilder().create();

  private static final GitlabConfiguration GITLAB_CONFIGURATION = new GitlabConfiguration(
    "existing-id",
    true,
    "application-id",
    "www.url.com",
    "secret",
    true,
    Set.of("group1", "group2"),
    true,
    AUTO_PROVISIONING,
    "provisioning-token"
  );

  private static final GitlabConfigurationResource EXPECTED_GITLAB_CONF_RESOURCE = new GitlabConfigurationResource(
    GITLAB_CONFIGURATION.id(),
    GITLAB_CONFIGURATION.enabled(),
    GITLAB_CONFIGURATION.applicationId(),
    GITLAB_CONFIGURATION.url(),
    GITLAB_CONFIGURATION.synchronizeGroups(),
    List.of("group1", "group2"),
    GITLAB_CONFIGURATION.allowUsersToSignUp(),
    ProvisioningType.valueOf(GITLAB_CONFIGURATION.provisioningType().name()),
    !GITLAB_CONFIGURATION.provisioningToken().isEmpty(),
    "error-message");

  private static final String EXPECTED_CONFIGURATION = """
    {
      "id": "existing-id",
      "enabled": true,
      "applicationId": "application-id",
      "url": "www.url.com",
      "synchronizeGroups": true,
      "allowedGroups": [
        "group1",
        "group2"
      ],
      "provisioningType": "AUTO_PROVISIONING",
      "allowUsersToSignUp": true,
      "errorMessage": "error-message",
      "isProvisioningTokenSet": true
    }
    """;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final GitlabConfigurationService gitlabConfigurationService = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultGitlabConfigurationController(userSession, gitlabConfigurationService));

  @Before
  public void setUp() {
    when(gitlabConfigurationService.validate(any())).thenReturn(Optional.of("error-message"));
  }

  @Test
  public void fetchConfiguration_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(get(GITLAB_CONFIGURATION_ENDPOINT + "/1"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchConfiguration_whenConfigNotFound_throws() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.getConfiguration("not-existing")).thenThrow(new NotFoundException("bla"));

    mockMvc.perform(get(GITLAB_CONFIGURATION_ENDPOINT + "/not-existing"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"bla\"}"));
  }

  @Test
  public void fetchConfiguration_whenConfigFound_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.getConfiguration("existing-id")).thenReturn(GITLAB_CONFIGURATION);

    mockMvc.perform(get(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_CONFIGURATION));
  }

  @Test
  public void search_whenNoParameters_shouldUseDefaultAndForwardToGroupMembershipService() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.findConfigurations()).thenReturn(Optional.of(GITLAB_CONFIGURATION));

    MvcResult mvcResult = mockMvc.perform(get(GITLAB_CONFIGURATION_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GitlabConfigurationSearchRestResponse gitlabConfigurationResource = GSON.fromJson(mvcResult.getResponse().getContentAsString(), GitlabConfigurationSearchRestResponse.class);

    assertThat(gitlabConfigurationResource.page().pageSize()).isEqualTo(1000);
    assertThat(gitlabConfigurationResource.page().pageIndex()).isEqualTo(1);
    assertThat(gitlabConfigurationResource.page().total()).isEqualTo(1);
    assertThat(gitlabConfigurationResource.gitlabConfigurations()).containsExactly(EXPECTED_GITLAB_CONF_RESOURCE);
  }

  @Test
  public void search_whenNoParametersAndNoConfig_shouldReturnEmptyList() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.findConfigurations()).thenReturn(Optional.empty());

    MvcResult mvcResult = mockMvc.perform(get(GITLAB_CONFIGURATION_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GitlabConfigurationSearchRestResponse gitlabConfigurationResource = GSON.fromJson(mvcResult.getResponse().getContentAsString(), GitlabConfigurationSearchRestResponse.class);

    assertThat(gitlabConfigurationResource.page().pageSize()).isEqualTo(1000);
    assertThat(gitlabConfigurationResource.page().pageIndex()).isEqualTo(1);
    assertThat(gitlabConfigurationResource.page().total()).isZero();
    assertThat(gitlabConfigurationResource.gitlabConfigurations()).isEmpty();
  }

  @Test
  public void updateConfiguration_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(patch(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("{}"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void updateConfiguration_whenAllowedGroupsIsNull_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(patch(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("""
        {
          "enabled": true,
          "applicationId": "application-id",
          "secret": "123",
          "url": "www.url.com",
          "synchronizeGroups": true,
          "allowedGroups": null,
          "provisioningType": "AUTO_PROVISIONING",
          "allowUsersToSignUp": true
        }
        """))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"allowedGroups must not be null\"}"));

  }

  @Test
  public void updateConfiguration_whenAllFieldsUpdated_performUpdates() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.updateConfiguration(any())).thenReturn(GITLAB_CONFIGURATION);

    String payload = """
      {
            "enabled": true,
            "applicationId": "application-id",
            "url": "www.url.com",
            "secret": "newSecret",
            "synchronizeGroups": true,
            "allowedGroups": [
              "group1",
              "group2"
            ],
            "provisioningType": "AUTO_PROVISIONING",
            "allowUsersToSignUp": true,
            "provisioningToken": "token"
      }
      """;

    mockMvc.perform(patch(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content(payload))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_CONFIGURATION));

    verify(gitlabConfigurationService).updateConfiguration(new UpdateGitlabConfigurationRequest(
      "existing-id",
      NonNullUpdatedValue.withValueOrThrow(true),
      NonNullUpdatedValue.withValueOrThrow("application-id"),
      NonNullUpdatedValue.withValueOrThrow("www.url.com"),
      NonNullUpdatedValue.withValueOrThrow("newSecret"),
      NonNullUpdatedValue.withValueOrThrow(true),
      NonNullUpdatedValue.withValueOrThrow(Set.of("group1", "group2")),
      NonNullUpdatedValue.withValueOrThrow(true),
      UpdatedValue.withValue("token"),
      NonNullUpdatedValue.withValueOrThrow(AUTO_PROVISIONING)
    ));
  }

  @Test
  public void updateConfiguration_whenSomeFieldsUpdated_performUpdates() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.updateConfiguration(any())).thenReturn(GITLAB_CONFIGURATION);

    String payload = """
      {
            "enabled": false,
            "provisioningType": "JIT",
            "allowUsersToSignUp": false,
            "provisioningToken": null
      }
      """;

    mockMvc.perform(patch(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content(payload))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_CONFIGURATION));

    verify(gitlabConfigurationService).updateConfiguration(new UpdateGitlabConfigurationRequest(
      "existing-id",
      NonNullUpdatedValue.withValueOrThrow(false),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.withValueOrThrow(false),
      UpdatedValue.withValue(null),
      NonNullUpdatedValue.withValueOrThrow(JIT)
    ));
  }

  @Test
  public void create_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
      post(GITLAB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
            {
               "enabled": true,
               "applicationId": "application-id",
               "url": "www.url.com",
               "secret": "123",
               "synchronizeGroups": true,
               "allowedGroups": [
                  "group1",
                  "group2"
                ],
               "provisioningType": "AUTO_PROVISIONING",
               "allowUsersToSignUp": true
             }
          """))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void create_whenConfigCreated_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.createConfiguration(any())).thenReturn(GITLAB_CONFIGURATION);

    mockMvc.perform(
      post(GITLAB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
            {
              "enabled": true,
              "applicationId": "application-id",
              "secret": "123",
              "url": "www.url.com",
              "synchronizeGroups": true,
              "allowedGroups": [
                "group1",
                "group2"
              ],
              "provisioningType": "AUTO_PROVISIONING",
              "provisioningToken": "token",
              "allowUsersToSignUp": true
            }

          """))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "existing-id",
            "enabled": true,
            "applicationId": "application-id",
            "url": "www.url.com",
            "synchronizeGroups": true,
            "allowedGroups": [
              "group1",
              "group2"
            ],
            "provisioningType": "AUTO_PROVISIONING",
            "allowUsersToSignUp": true,
            "isProvisioningTokenSet": true
          }
          """));

  }
  @Test
  public void create_whenConfigCreatedWithoutOptionalParams_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(gitlabConfigurationService.createConfiguration(any())).thenReturn(new GitlabConfiguration(
      "existing-id",
      true,
      "application-id",
      "www.url.com",
      "secret",
      true,
      Set.of("group1", "group2"),
      false,
      AUTO_PROVISIONING,
      null
    ));

    mockMvc.perform(
      post(GITLAB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
            {
              "enabled": true,
              "applicationId": "application-id",
              "secret": "123",
              "url": "www.url.com",
              "synchronizeGroups": true,
              "allowedGroups": [
                "group1",
                "group2"
              ],
              "provisioningType": "AUTO_PROVISIONING"
            }

          """))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
            "id": "existing-id",
            "enabled": true,
            "applicationId": "application-id",
            "url": "www.url.com",
            "synchronizeGroups": true,
            "allowedGroups": [
              "group1",
              "group2"
            ],
            "provisioningType": "AUTO_PROVISIONING",
            "allowUsersToSignUp": false,
            "isProvisioningTokenSet": false
          }
          """));

  }

  @Test
  public void create_whenAllowedGroupsIsNull_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();
    mockMvc.perform(
        post(GITLAB_CONFIGURATION_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content("""
            {
              "enabled": true,
              "applicationId": "application-id",
              "secret": "123",
              "url": "www.url.com",
              "synchronizeGroups": true,
              "allowedGroups": null,
              "provisioningType": "JIT"
            }

          """))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Value {} for field allowedGroups was rejected. Error: must not be null.\"}"));

  }

  @Test
  public void create_whenRequiredParameterIsMissing_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
      post(GITLAB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
            "enabled": true,
            "applicationId": "application-id",
            "url": "www.url.com",
            "synchronizeGroups": true,
            "allowedGroups": [
              "group1",
              "group2"
            ],
            "provisioningType": "AUTO_PROVISIONING",
            "allowUsersToSignUp": true
          }
          """))
      .andExpectAll(
        status().isBadRequest(),
        content().json(
          "{\"message\":\"Value {} for field secret was rejected. Error: must not be empty.\"}"));

  }

  @Test
  public void delete_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
      delete(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void delete_whenConfigIsDeleted_returnsNoContent() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
      delete(GITLAB_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isNoContent());

    verify(gitlabConfigurationService).deleteConfiguration("existing-id");
  }

  @Test
  public void delete_whenConfigNotFound_returnsNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(new NotFoundException("Not found")).when(gitlabConfigurationService).deleteConfiguration("not-existing");

    mockMvc.perform(
      delete(GITLAB_CONFIGURATION_ENDPOINT + "/not-existing"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Not found\"}"));
  }

}
