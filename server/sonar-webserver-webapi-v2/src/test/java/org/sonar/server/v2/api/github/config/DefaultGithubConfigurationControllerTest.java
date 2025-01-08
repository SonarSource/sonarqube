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
package org.sonar.server.v2.api.github.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.github.config.GithubConfiguration;
import org.sonar.server.common.github.config.GithubConfigurationService;
import org.sonar.server.common.github.config.UpdateGithubConfigurationRequest;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.github.config.controller.DefaultGithubConfigurationController;
import org.sonar.server.v2.api.github.config.resource.GithubConfigurationResource;
import org.sonar.server.v2.api.github.config.response.GithubConfigurationSearchRestResponse;
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
import static org.sonar.server.v2.WebApiEndpoints.GITHUB_CONFIGURATION_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultGithubConfigurationControllerTest {
  private static final Gson GSON = new GsonBuilder().create();

  private static final GithubConfiguration GITHUB_CONFIGURATION = new GithubConfiguration(
    "existing-id",
    true,
    "client-id",
    "client-secret",
    "application-id",
    "private-key",
    true,
    "api.url.com",
    "www.url.com",
    Set.of("org1", "org2"),
    AUTO_PROVISIONING,
    true,
    true,
    true
  );

  private static final GithubConfigurationResource EXPECTED_GITHUB_CONF_RESOURCE = new GithubConfigurationResource(
    GITHUB_CONFIGURATION.id(),
    GITHUB_CONFIGURATION.enabled(),
    GITHUB_CONFIGURATION.applicationId(),
    GITHUB_CONFIGURATION.synchronizeGroups(),
    GITHUB_CONFIGURATION.apiUrl(),
    GITHUB_CONFIGURATION.webUrl(),
    List.of("org1", "org2"),
    ProvisioningType.valueOf(GITHUB_CONFIGURATION.provisioningType().name()),
    GITHUB_CONFIGURATION.allowUsersToSignUp(),
    GITHUB_CONFIGURATION.provisionProjectVisibility(),
    GITHUB_CONFIGURATION.userConsentRequiredAfterUpgrade(),
    "error-message");

  private static final String EXPECTED_CONFIGURATION = """
    {
      "id": "existing-id",
      "enabled": true,
      "applicationId": "application-id",
      "synchronizeGroups": true,
      "apiUrl": "api.url.com",
      "webUrl": "www.url.com",
      "allowedOrganizations": [
        "org1",
        "org2"
      ],
      "provisioningType": "AUTO_PROVISIONING",
      "allowUsersToSignUp": true,
      "projectVisibility": true,
      "userConsentRequiredAfterUpgrade": true,
      "errorMessage": "error-message"
    }
    """;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final GithubConfigurationService githubConfigurationService = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultGithubConfigurationController(userSession, githubConfigurationService));

  @Before
  public void setUp() {
    when(githubConfigurationService.validate(any())).thenReturn(Optional.of("error-message"));
  }

  @Test
  public void fetchConfiguration_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(get(GITHUB_CONFIGURATION_ENDPOINT + "/1"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void fetchConfiguration_whenConfigNotFound_throws() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.getConfiguration("not-existing")).thenThrow(new NotFoundException("bla"));

    mockMvc.perform(get(GITHUB_CONFIGURATION_ENDPOINT + "/not-existing"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"bla\"}"));
  }

  @Test
  public void fetchConfiguration_whenConfigFound_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.getConfiguration("existing-id")).thenReturn(GITHUB_CONFIGURATION);

    mockMvc.perform(get(GITHUB_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_CONFIGURATION));
  }

  @Test
  public void search_whenNoParameters_shouldUseDefaultAndForwardToGroupMembershipService() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.findConfigurations()).thenReturn(Optional.of(GITHUB_CONFIGURATION));

    MvcResult mvcResult = mockMvc.perform(get(GITHUB_CONFIGURATION_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GithubConfigurationSearchRestResponse response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), GithubConfigurationSearchRestResponse.class);

    assertThat(response.page().pageSize()).isEqualTo(1000);
    assertThat(response.page().pageIndex()).isEqualTo(1);
    assertThat(response.page().total()).isEqualTo(1);
    assertThat(response.githubConfigurations()).containsExactly(EXPECTED_GITHUB_CONF_RESOURCE);
  }

  @Test
  public void search_whenNoParametersAndNoConfig_shouldReturnEmptyList() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.findConfigurations()).thenReturn(Optional.empty());

    MvcResult mvcResult = mockMvc.perform(get(GITHUB_CONFIGURATION_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    GithubConfigurationSearchRestResponse response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), GithubConfigurationSearchRestResponse.class);

    assertThat(response.page().pageSize()).isEqualTo(1000);
    assertThat(response.page().pageIndex()).isEqualTo(1);
    assertThat(response.page().total()).isZero();
    assertThat(response.githubConfigurations()).isEmpty();
  }

  @Test
  public void updateConfiguration_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(patch(GITHUB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("{}"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void updateConfiguration_whenAllFieldsUpdated_performUpdates() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.updateConfiguration(any())).thenReturn(GITHUB_CONFIGURATION);

    String payload = """
      {
        "enabled": true,
        "clientId": "new-client-id",
        "clientSecret": "new-client-secret",
        "applicationId": "new-application-id",
        "privateKey": "new-private-key",
        "synchronizeGroups": false,
        "apiUrl": "new-api.url.com",
        "webUrl": "new-www.url.com",
        "allowedOrganizations": [
          "new-org1",
          "new-org2"
        ],
        "provisioningType": "AUTO_PROVISIONING",
        "allowUsersToSignUp": false,
        "projectVisibility": false,
        "userConsentRequiredAfterUpgrade": false
      }
      """;

    mockMvc.perform(patch(GITHUB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content(payload))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_CONFIGURATION));

    verify(githubConfigurationService).updateConfiguration(new UpdateGithubConfigurationRequest(
      "existing-id",
      NonNullUpdatedValue.withValueOrThrow(true),
      NonNullUpdatedValue.withValueOrThrow("new-client-id"),
      NonNullUpdatedValue.withValueOrThrow("new-client-secret"),
      NonNullUpdatedValue.withValueOrThrow("new-application-id"),
      NonNullUpdatedValue.withValueOrThrow("new-private-key"),
      NonNullUpdatedValue.withValueOrThrow(false),
      NonNullUpdatedValue.withValueOrThrow("new-api.url.com"),
      NonNullUpdatedValue.withValueOrThrow("new-www.url.com"),
      NonNullUpdatedValue.withValueOrThrow(Set.of("new-org1", "new-org2")),
      NonNullUpdatedValue.withValueOrThrow(AUTO_PROVISIONING),
      NonNullUpdatedValue.withValueOrThrow(false),
      NonNullUpdatedValue.withValueOrThrow(false),
      NonNullUpdatedValue.withValueOrThrow(false)
    ));
  }

  @Test
  public void updateConfiguration_whenSomeFieldsUpdated_performUpdates() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.updateConfiguration(any())).thenReturn(GITHUB_CONFIGURATION);

    String payload = """
      {
        "enabled": false,
        "provisioningType": "JIT",
        "allowUsersToSignUp": false
      }
      """;

    mockMvc.perform(patch(GITHUB_CONFIGURATION_ENDPOINT + "/existing-id")
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content(payload))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_CONFIGURATION));

    verify(githubConfigurationService).updateConfiguration(new UpdateGithubConfigurationRequest(
      "existing-id",
      NonNullUpdatedValue.withValueOrThrow(false),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.withValueOrThrow(JIT),
      NonNullUpdatedValue.withValueOrThrow(false),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined()
    ));
  }

  @Test
  public void create_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
      post(GITHUB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
             "enabled": true,
             "clientId": "new-client-id",
             "clientSecret": "new-client-secret",
             "applicationId": "new-application-id",
             "privateKey": "new-private-key",
             "synchronizeGroups": false,
             "apiUrl": "new-api.url.com",
             "webUrl": "new-www.url.com",
             "allowedOrganizations": [
               "new-org1",
               "new-org2"
             ],
             "provisioningType": "AUTO_PROVISIONING",
             "allowUsersToSignUp": false,
             "projectVisibility": false,
             "userConsentRequiredAfterUpgrade": false
           }
          """))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void create_whenConfigCreated_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.createConfiguration(any())).thenReturn(GITHUB_CONFIGURATION);

    mockMvc.perform(
      post(GITHUB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
           {
             "enabled": true,
             "clientId": "client-id",
             "clientSecret": "client-secret",
             "applicationId": "application-id",
             "privateKey": "private-key",
             "synchronizeGroups": true,
             "apiUrl": "api.url.com",
             "webUrl": "www.url.com",
             "allowedOrganizations": [
               "org1",
               "org2"
             ],
             "provisioningType": "AUTO_PROVISIONING",
             "allowUsersToSignUp": true,
             "projectVisibility": true,
             "userConsentRequiredAfterUpgrade": true
           }
          """))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
             "enabled": true,
             "applicationId": "application-id",
             "synchronizeGroups": true,
             "apiUrl": "api.url.com",
             "webUrl": "www.url.com",
             "allowedOrganizations": [
               "org1",
               "org2"
             ],
             "provisioningType": "AUTO_PROVISIONING",
             "allowUsersToSignUp": true,
             "projectVisibility": true,
             "userConsentRequiredAfterUpgrade": true
          }
          """));

  }
  @Test
  public void create_whenConfigCreatedWithoutOptionalParams_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(githubConfigurationService.createConfiguration(any())).thenReturn(new GithubConfiguration(
      "existing-id",
      true,
      "client-id",
      "client-secret",
      "application-id",
      "private-key",
      true,
      "api.url.com",
      "www.url.com",
      Set.of(),
      AUTO_PROVISIONING,
      false,
      false,
      false
    ));

    mockMvc.perform(
      post(GITHUB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
            {
               "enabled": true,
               "clientId": "client-id",
               "clientSecret": "client-secret",
               "applicationId": "application-id",
               "privateKey": "private-key",
               "synchronizeGroups": true,
               "apiUrl": "api.url.com",
               "webUrl": "www.url.com",
               "allowedOrganizations": [],
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
             "synchronizeGroups": true,
             "apiUrl": "api.url.com",
             "webUrl": "www.url.com",
             "allowedOrganizations": [],
             "provisioningType": "AUTO_PROVISIONING",
             "allowUsersToSignUp": false,
             "projectVisibility": false,
             "userConsentRequiredAfterUpgrade": false
          }
          """));

  }

  @Test
  public void create_whenRequiredParameterIsMissing_shouldReturnBadRequest() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
      post(GITHUB_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
          {
             "enabled": true,
             "clientId": "client-id",
             "clientSecret": "client-secret",
             "privateKey": "private-key",
             "synchronizeGroups": true,
             "apiUrl": "api.url.com",
             "webUrl": "www.url.com",
             "allowedOrganizations": [
               "org1",
               "org2"
             ],
             "provisioningType": "AUTO_PROVISIONING",
             "allowUsersToSignUp": true,
             "projectVisibility": true,
             "userConsentRequiredAfterUpgrade": true
          }
          """))
      .andExpectAll(
        status().isBadRequest(),
        content().json(
          "{\"message\":\"Value {} for field applicationId was rejected. Error: must not be empty.\"}"));

  }

  @Test
  public void delete_whenUserIsNotAdministrator_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
      delete(GITHUB_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  public void delete_whenConfigIsDeleted_returnsNoContent() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
      delete(GITHUB_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isNoContent());

    verify(githubConfigurationService).deleteConfiguration("existing-id");
  }

  @Test
  public void delete_whenConfigNotFound_returnsNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(new NotFoundException("Not found")).when(githubConfigurationService).deleteConfiguration("not-existing");

    mockMvc.perform(
      delete(GITHUB_CONFIGURATION_ENDPOINT + "/not-existing"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Not found\"}"));
  }

}
