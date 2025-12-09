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
package org.sonar.server.v2.api.email.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.email.config.EmailConfiguration;
import org.sonar.server.common.email.config.EmailConfigurationAuthMethod;
import org.sonar.server.common.email.config.EmailConfigurationSecurityProtocol;
import org.sonar.server.common.email.config.EmailConfigurationService;
import org.sonar.server.common.email.config.UpdateEmailConfigurationRequest;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.api.email.config.controller.DefaultEmailConfigurationController;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationResource;
import org.sonar.server.v2.api.email.config.response.EmailConfigurationSearchRestResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.common.email.config.EmailConfigurationService.UNIQUE_EMAIL_CONFIGURATION_ID;
import static org.sonar.server.v2.WebApiEndpoints.EMAIL_CONFIGURATION_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultEmailConfigurationControllerTest {

  private static final Gson GSON = new GsonBuilder().create();

  private static final EmailConfiguration EMAIL_BASIC_CONFIGURATION = new EmailConfiguration(
    UNIQUE_EMAIL_CONFIGURATION_ID,
    "host",
    "port",
    EmailConfigurationSecurityProtocol.SSLTLS,
    "fromAddress",
    "fromName",
    "subjectPrefix",
    EmailConfigurationAuthMethod.BASIC,
    "username",
    "basicPassword",
    null,
    null,
    null,
    null
  );

  private static final EmailConfiguration EMAIL_OAUTH_CONFIGURATION = new EmailConfiguration(
    UNIQUE_EMAIL_CONFIGURATION_ID,
    "host",
    "port",
    EmailConfigurationSecurityProtocol.STARTTLS,
    "fromAddress",
    "fromName",
    "subjectPrefix",
    EmailConfigurationAuthMethod.OAUTH,
    "username",
    null,
    "oauthAuthenticationHost",
    "oauthClientId",
    "oauthClientSecret",
    "oauthTenant"
  );

  private static final String EXPECTED_BASIC_CONFIGURATION = """
    {
      "host": "host",
      "port": "port",
      "securityProtocol": "SSLTLS",
      "fromAddress": "fromAddress",
      "fromName": "fromName",
      "subjectPrefix": "subjectPrefix",
      "authMethod": "BASIC",
      "username": "username",
      "isBasicPasswordSet": true
    }
    """;

  private static final String EXPECTED_OAUTH_CONFIGURATION = """
    {
      "host": "host",
      "port": "port",
      "securityProtocol": "STARTTLS",
      "fromAddress": "fromAddress",
      "fromName": "fromName",
      "subjectPrefix": "subjectPrefix",
      "authMethod": "OAUTH",
      "username": "username",
      "isBasicPasswordSet": false,
      "oauthAuthenticationHost": "oauthAuthenticationHost",
      "isOauthClientIdSet": true,
      "isOauthClientSecretSet": true,
      "oauthTenant": "oauthTenant"
    }
    """;

  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();
  private final EmailConfigurationService emailConfigurationService = mock();
  private final MockMvc mockMvc = ControllerTester.getMockMvc(new DefaultEmailConfigurationController(userSession, emailConfigurationService));

  @Test
  void createEmailConfiguration_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
        post(EMAIL_CONFIGURATION_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content("""
            {
               "host": "host",
               "port": "port",
               "securityProtocol": "NONE",
               "fromAddress": "fromAddress",
               "fromName": "fromName",
               "subjectPrefix": "subjectPrefix",
               "authMethod": "BASIC",
               "username": "username",
               "basicPassword": "basicPassword"
            }
            """))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"host", "port", "fromAddress", "fromName", "subjectPrefix", "username"})
  void create_whenRequiredFieldEmpty_shouldReturnBadRequest(String field) throws Exception {
    userSession.logIn().setSystemAdministrator();

    String payload = format("""
      {
         "host": "%s",
         "port": "%s",
         "securityProtocol": "NONE",
         "fromAddress": "%s",
         "fromName": "%s",
         "subjectPrefix": "%s",
         "authMethod": "BASIC",
         "username": "%s"
      }
      """,
      field.equals("host") ? "" : "host",
      field.equals("port") ? "" : "port",
      field.equals("fromAddress") ? "" : "fromAddress",
      field.equals("fromName") ? "" : "fromName",
      field.equals("subjectPrefix") ? "" : "subjectPrefix",
      field.equals("username") ? "" : "username");

    mockMvc.perform(post(EMAIL_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(payload))
      .andExpectAll(
        status().isBadRequest(),
        content().json(format("{\"message\":\"Value  for field %s was rejected. Error: must not be empty.\"}", field)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"host", "port", "fromAddress", "fromName", "subjectPrefix", "username"})
  void create_whenRequiredStringFieldNull_shouldReturnBadRequest(String field) throws Exception {
    userSession.logIn().setSystemAdministrator();

    String payload = format("""
      {
         %s
         %s
         %s
         %s
         %s
         %s
         "securityProtocol": "NONE",
         "authMethod": "BASIC"
      }
      """,
      field.equals("host") ? "" : "\"host\" : \"host\",",
      field.equals("port") ? "" : "\"port\" : \"port\",",
      field.equals("fromAddress") ? "" : "\"fromAddress\" : \"fromAddress\",",
      field.equals("fromName") ? "" : "\"fromName\" : \"fromName\",",
      field.equals("subjectPrefix") ? "" : "\"subjectPrefix\" : \"subjectPrefix\",",
      field.equals("username") ? "" : "\"username\" : \"username\",");

    mockMvc.perform(post(EMAIL_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(payload))
      .andExpectAll(
        status().isBadRequest(),
        content().json(format("{\"message\":\"Value {} for field %s was rejected. Error: must not be empty.\"}", field)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"securityProtocol", "authMethod"})
  void create_whenRequiredEnumFieldNull_shouldReturnBadRequest(String field) throws Exception {
    userSession.logIn().setSystemAdministrator();

    String payload = format("""
      {
         %s
         %s
         "host": "host",
         "port": "port",
         "fromAddress": "fromAddress",
         "fromName": "fromName",
         "subjectPrefix": "subjectPrefix",
         "username": "username"
      }
      """,
      field.equals("securityProtocol") ? "" : "\"securityProtocol\" : \"NONE\",",
      field.equals("authMethod") ? "" : "\"authMethod\" : \"BASIC\",");

    mockMvc.perform(post(EMAIL_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(payload))
      .andExpectAll(
        status().isBadRequest(),
        content().json(format("{\"message\":\"Value {} for field %s was rejected. Error: must not be null.\"}", field)));
  }

  @Test
  void create_whenBasicConfigCreated_returnsItWithoutSecrets() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.createConfiguration(any())).thenReturn(EMAIL_BASIC_CONFIGURATION);

    mockMvc.perform(
      post(EMAIL_CONFIGURATION_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content("""
         {
           "host": "host",
           "port": "port",
           "securityProtocol": "SSLTLS",
           "fromAddress": "fromAddress",
           "fromName": "fromName",
           "subjectPrefix": "subjectPrefix",
           "authMethod": "BASIC",
           "username": "username",
           "basicPassword": "basicPassword"
         }
        """))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
             "id": "email-configuration",
             "host": "host",
             "port": "port",
             "securityProtocol": "SSLTLS",
             "fromAddress": "fromAddress",
             "fromName": "fromName",
             "subjectPrefix": "subjectPrefix",
             "authMethod": "BASIC",
             "username": "username",
             "isBasicPasswordSet": true
          }
          """));

    verify(emailConfigurationService).createConfiguration(any());
  }

  @Test
  void create_whenOauthConfigCreated_returnsItWithoutSecrets() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.createConfiguration(any())).thenReturn(EMAIL_OAUTH_CONFIGURATION);

    mockMvc.perform(
        post(EMAIL_CONFIGURATION_ENDPOINT)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .content("""
         {
           "host": "host",
           "port": "port",
           "securityProtocol": "STARTTLS",
           "fromAddress": "fromAddress",
           "fromName": "fromName",
           "subjectPrefix": "subjectPrefix",
           "authMethod": "OAUTH",
           "username": "username",
           "oauthAuthenticationHost": "oauthAuthenticationHost",
           "oauthClientId": "oauthClientId",
           "oauthClientSecret": "oauthClientSecret",
           "oauthTenant": "oauthTenant"
         }
        """))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {
             "id": "email-configuration",
             "host": "host",
             "port": "port",
             "securityProtocol": "STARTTLS",
             "fromAddress": "fromAddress",
             "fromName": "fromName",
             "subjectPrefix": "subjectPrefix",
             "authMethod": "OAUTH",
             "username": "username",
             "isBasicPasswordSet": false,
             "oauthAuthenticationHost": "oauthAuthenticationHost",
             "isOauthClientIdSet": true,
             "isOauthClientSecretSet": true,
             "oauthTenant": "oauthTenant"
          }
          """));

    verify(emailConfigurationService).createConfiguration(any());
  }

  @Test
  void getEmailConfiguration_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(get(EMAIL_CONFIGURATION_ENDPOINT + "/whatever-id"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  void getEmailConfiguration_whenConfigNotFound_throws() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.getConfiguration("not-existing-id")).thenThrow(new NotFoundException("Not found"));

    mockMvc.perform(get(EMAIL_CONFIGURATION_ENDPOINT + "/not-existing-id"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Not found\"}"));

    verify(emailConfigurationService).getConfiguration("not-existing-id");
  }

  @Test
  void getEmailConfiguration_whenConfigFound_returnsIt() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.getConfiguration("existing-id")).thenReturn(EMAIL_BASIC_CONFIGURATION);

    mockMvc.perform(get(EMAIL_CONFIGURATION_ENDPOINT + "/existing-id"))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_BASIC_CONFIGURATION));

    verify(emailConfigurationService).getConfiguration("existing-id");
  }

  @Test
  void searchEmailConfigurations_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(get(EMAIL_CONFIGURATION_ENDPOINT + "/whatever-id"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  void searchEmailConfigurations_whenNoParams_shouldReturnDefault() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.findConfigurations()).thenReturn(Optional.of(EMAIL_BASIC_CONFIGURATION));

    MvcResult mvcResult = mockMvc.perform(get(EMAIL_CONFIGURATION_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    EmailConfigurationSearchRestResponse response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), EmailConfigurationSearchRestResponse.class);

    assertThat(response.page().pageSize()).isEqualTo(1000);
    assertThat(response.page().pageIndex()).isEqualTo(1);
    assertThat(response.page().total()).isEqualTo(1);
    assertThat(response.emailConfigurations()).containsExactly(toEmailConfigurationResource(EMAIL_BASIC_CONFIGURATION));
    verify(emailConfigurationService).findConfigurations();
  }

  private EmailConfigurationResource toEmailConfigurationResource(EmailConfiguration emailConfiguration) {
    return new EmailConfigurationResource(
      emailConfiguration.id(),
      emailConfiguration.host(),
      emailConfiguration.port(),
      toRestSecurityProtocol(emailConfiguration.securityProtocol()),
      emailConfiguration.fromAddress(),
      emailConfiguration.fromName(),
      emailConfiguration.subjectPrefix(),
      toRestAuthMethod(emailConfiguration.authMethod()),
      emailConfiguration.username(),
      emailConfiguration.basicPassword() != null,
      emailConfiguration.oauthAuthenticationHost(),
      emailConfiguration.oauthClientId() != null,
      emailConfiguration.oauthClientSecret() != null,
      emailConfiguration.oauthTenant()
    );
  }

  private static org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol toRestSecurityProtocol(EmailConfigurationSecurityProtocol securityProtocol) {
    return org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol.valueOf(securityProtocol.name());
  }

  private static org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod toRestAuthMethod(EmailConfigurationAuthMethod authMethod) {
    return org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod.valueOf(authMethod.name());
  }

  @Test
  void searchEmailConfigurations_whenNoParamAndNoConfig_shouldReturnEmptyList() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.findConfigurations()).thenReturn(Optional.empty());

    MvcResult mvcResult = mockMvc.perform(get(EMAIL_CONFIGURATION_ENDPOINT))
      .andExpect(status().isOk())
      .andReturn();

    EmailConfigurationSearchRestResponse response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), EmailConfigurationSearchRestResponse.class);

    assertThat(response.page().pageSize()).isEqualTo(1000);
    assertThat(response.page().pageIndex()).isEqualTo(1);
    assertThat(response.page().total()).isZero();
    assertThat(response.emailConfigurations()).isEmpty();
    verify(emailConfigurationService).findConfigurations();
  }

  @Test
  void updateConfiguration_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(patch(EMAIL_CONFIGURATION_ENDPOINT + "/whatever-id")
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content("{}"))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"host", "port", "fromAddress", "fromName", "subjectPrefix", "username"})
  void update_whenRequiredFieldEmpty_shouldReturnBadRequest(String field) throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(patch(EMAIL_CONFIGURATION_ENDPOINT + "/" + UNIQUE_EMAIL_CONFIGURATION_ID)
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content(format("""
          {
            "%s": ""
          }
          """, field)))
      .andExpectAll(
        status().isBadRequest(),
        content().json(format("{\"message\":\"Value  for field %s was rejected. Error: must not be empty.\"}", field)));
  }

  @Test
  void updateConfiguration_whenAllFieldsUpdated_performUpdates() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.updateConfiguration(any())).thenReturn(EMAIL_OAUTH_CONFIGURATION);

    mockMvc.perform(patch(EMAIL_CONFIGURATION_ENDPOINT + "/" + UNIQUE_EMAIL_CONFIGURATION_ID)
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content("""
          {
            "host": "host",
            "port": "port",
            "securityProtocol": "STARTTLS",
            "fromAddress": "fromAddress",
            "fromName": "fromName",
            "subjectPrefix": "subjectPrefix",
            "authMethod": "OAUTH",
            "username": "username",
            "basicPassword": "basicPassword",
            "oauthAuthenticationHost": "oauthAuthenticationHost",
            "oauthClientId": "oauthClientId",
            "oauthClientSecret": "oauthClientSecret",
            "oauthTenant": "oauthTenant"
          }
          """))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_OAUTH_CONFIGURATION));

    verify(emailConfigurationService).updateConfiguration(new UpdateEmailConfigurationRequest(
      UNIQUE_EMAIL_CONFIGURATION_ID,
      NonNullUpdatedValue.withValueOrThrow("host"),
      NonNullUpdatedValue.withValueOrThrow("port"),
      NonNullUpdatedValue.withValueOrThrow(EmailConfigurationSecurityProtocol.STARTTLS),
      NonNullUpdatedValue.withValueOrThrow("fromAddress"),
      NonNullUpdatedValue.withValueOrThrow("fromName"),
      NonNullUpdatedValue.withValueOrThrow("subjectPrefix"),
      NonNullUpdatedValue.withValueOrThrow(EmailConfigurationAuthMethod.OAUTH),
      NonNullUpdatedValue.withValueOrThrow("username"),
      NonNullUpdatedValue.withValueOrThrow("basicPassword"),
      NonNullUpdatedValue.withValueOrThrow("oauthAuthenticationHost"),
      NonNullUpdatedValue.withValueOrThrow("oauthClientId"),
      NonNullUpdatedValue.withValueOrThrow("oauthClientSecret"),
      NonNullUpdatedValue.withValueOrThrow("oauthTenant")
    ));
  }

  @Test
  void updateConfiguration_whenSomeFieldsUpdated_performUpdates() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(emailConfigurationService.updateConfiguration(any())).thenReturn(EMAIL_OAUTH_CONFIGURATION);

    mockMvc.perform(patch(EMAIL_CONFIGURATION_ENDPOINT + "/" + UNIQUE_EMAIL_CONFIGURATION_ID)
        .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
        .content("""
          {
            "host": "host",
            "oauthTenant": "oauthTenant"
          }
          """))
      .andExpectAll(
        status().isOk(),
        content().json(EXPECTED_OAUTH_CONFIGURATION));

    verify(emailConfigurationService).updateConfiguration(new UpdateEmailConfigurationRequest(
      UNIQUE_EMAIL_CONFIGURATION_ID,
      NonNullUpdatedValue.withValueOrThrow("host"),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.undefined(),
      NonNullUpdatedValue.withValueOrThrow("oauthTenant")
    ));
  }

  @Test
  void delete_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    mockMvc.perform(
        delete(EMAIL_CONFIGURATION_ENDPOINT + "/" + UNIQUE_EMAIL_CONFIGURATION_ID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }

  @Test
  void delete_whenConfigIsDeleted_returnsNoContent() throws Exception {
    userSession.logIn().setSystemAdministrator();

    mockMvc.perform(
        delete(EMAIL_CONFIGURATION_ENDPOINT + "/" + UNIQUE_EMAIL_CONFIGURATION_ID))
      .andExpectAll(
        status().isNoContent());

    verify(emailConfigurationService).deleteConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID);
  }

  @Test
  void delete_whenConfigNotFound_returnsNotFound() throws Exception {
    userSession.logIn().setSystemAdministrator();
    doThrow(new NotFoundException("Not found")).when(emailConfigurationService).deleteConfiguration("not-existing-id");

    mockMvc.perform(
        delete(EMAIL_CONFIGURATION_ENDPOINT + "/not-existing-id"))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Not found\"}"));
  }

}
