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
package org.sonar.server.common.email.config;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.common.NonNullUpdatedValue.undefined;
import static org.sonar.server.common.NonNullUpdatedValue.withValueOrThrow;
import static org.sonar.server.common.email.config.EmailConfigurationService.UNIQUE_EMAIL_CONFIGURATION_ID;
import static org.sonar.server.common.email.config.UpdateEmailConfigurationRequest.builder;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_HOST;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_CLIENTID;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_HOST;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_TENANT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_PASSWORD;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_USERNAME;

class EmailConfigurationServiceIT {

  private static final String OAUTH_URLS_ERROR_MESSAGE = "For security reasons, OAuth urls can't be updated without providing the client secret.";
  private static final String BASIC_URLS_ERROR_MESSAGE = "For security reasons, the host can't be updated without providing the password.";

  private static final EmailConfigurationBuilder EMAIL_BASIC_CONFIG_BUILDER = EmailConfigurationBuilder.builder()
    .id(UNIQUE_EMAIL_CONFIGURATION_ID)
    .host("host")
    .port("port")
    .securityProtocol(EmailConfigurationSecurityProtocol.NONE)
    .fromAddress("fromAddress")
    .fromName("fromName")
    .subjectPrefix("subjectPrefix")
    .authMethod(EmailConfigurationAuthMethod.BASIC)
    .username("username")
    .basicPassword("basicPassword")
    .oauthAuthenticationHost("oauthAuthenticationHost")
    .oauthClientId("oauthClientId")
    .oauthClientSecret("oauthClientSecret")
    .oauthTenant("oauthTenant");

  private static final EmailConfigurationBuilder EMAIL_OAUTH_CONFIG_BUILDER = EmailConfigurationBuilder.builder()
    .id(UNIQUE_EMAIL_CONFIGURATION_ID)
    .host("hostOAuth")
    .port("portOAuth")
    .securityProtocol(EmailConfigurationSecurityProtocol.SSLTLS)
    .fromAddress("fromAddressOAuth")
    .fromName("fromNameOAuth")
    .subjectPrefix("subjectPrefixOAuth")
    .authMethod(EmailConfigurationAuthMethod.OAUTH)
    .username("usernameOAuth")
    .basicPassword("basicPasswordOAuth")
    .oauthAuthenticationHost("oauthAuthenticationHostOAuth")
    .oauthClientId("oauthClientIdOAuth")
    .oauthClientSecret("oauthClientSecretOAuth")
    .oauthTenant("oauthTenantOAuth");

  @RegisterExtension
  public DbTester dbTester = DbTester.create();

  private EmailConfigurationService underTest;

  @BeforeEach
  void setUp() {
    underTest = new EmailConfigurationService(dbTester.getDbClient());
  }

  @Test
  void createConfiguration_whenConfigExists_shouldFail() {
    dbTester.getDbClient().internalPropertiesDao().save(dbTester.getSession(), EMAIL_CONFIG_SMTP_HOST, "localhost");
    dbTester.commit();

    EmailConfiguration emailConfiguration = EMAIL_BASIC_CONFIG_BUILDER.build();
    assertThatThrownBy(() -> underTest.createConfiguration(emailConfiguration))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Email configuration already exists. Only one Email configuration is supported.");
  }

  @ParameterizedTest
  @MethodSource("configCreationParamConstraints")
  void createConfiguration_whenFieldsAreMissing_shouldThrow(EmailConfigurationAuthMethod authMethod, String missingField, String errorMessage) {
    EmailConfiguration config = new EmailConfiguration(
      UNIQUE_EMAIL_CONFIGURATION_ID,
      "smtpHost",
      "smtpPort",
      EmailConfigurationSecurityProtocol.NONE,
      "fromAddress",
      "fromName",
      "subjectPrefix",
      authMethod,
      "username",
      "basicPassword".equals(missingField) ? null : "basicPassword",
      "oauthAuthenticationHost".equals(missingField) ? null : "oauthAuthenticationHost",
      "oauthClientId".equals(missingField) ? null : "oauthClientId",
      "oauthClientSecret".equals(missingField) ? null : "oauthClientSecret",
      "oauthTenant".equals(missingField) ? null : "oauthTenant"
    );

    assertThatThrownBy(() -> underTest.createConfiguration(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(errorMessage);
  }

  static Object[][] configCreationParamConstraints() {
    return new Object[][]{
      {EmailConfigurationAuthMethod.BASIC, "basicPassword", "Password is required."},
      {EmailConfigurationAuthMethod.OAUTH, "oauthAuthenticationHost", "OAuth authentication host is required."},
      {EmailConfigurationAuthMethod.OAUTH, "oauthClientId", "OAuth client id is required."},
      {EmailConfigurationAuthMethod.OAUTH, "oauthClientSecret", "OAuth client secret is required."},
      {EmailConfigurationAuthMethod.OAUTH, "oauthTenant", "OAuth tenant is required."}
    };
  }

  @Test
  void createConfiguration_whenConfigDoesNotExist_shouldCreateConfig() {
    EmailConfiguration configuration = EMAIL_BASIC_CONFIG_BUILDER.build();
    EmailConfiguration createdConfig = underTest.createConfiguration(configuration);

    assertThatConfigurationIsCorrect(configuration, createdConfig);
  }

  @Test
  void getConfiguration_whenWrongId_shouldThrow() {
    assertThatThrownBy(() -> underTest.getConfiguration("wrongId"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Email configuration with id wrongId not found");
  }

  @Test
  void getConfiguration_whenNoConfig_shouldThrow() {
    assertThatThrownBy(() -> underTest.getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Email configuration doesn't exist.");
  }

  @Test
  void getConfiguration_whenConfigExists_shouldReturnConfig() {
    EmailConfiguration configuration = EMAIL_BASIC_CONFIG_BUILDER.build();
    underTest.createConfiguration(configuration);

    EmailConfiguration retrievedConfig = underTest.getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID);

    assertThatConfigurationIsCorrect(configuration, retrievedConfig);
  }

  @Test
  void findConfiguration_whenNoConfig_shouldReturnEmpty() {
    assertThat(underTest.findConfigurations()).isEmpty();
  }

  @Test
  void findConfiguration_whenConfigExists_shouldReturnConfig() {
    EmailConfiguration configuration = underTest.createConfiguration(EMAIL_BASIC_CONFIG_BUILDER.build());

    assertThat(underTest.findConfigurations()).contains(configuration);
  }

  @Test
  void updateConfiguration_whenConfigDoesNotExist_shouldThrow() {
    UpdateEmailConfigurationRequest updateRequest = getUpdateEmailConfigurationRequestFromConfig(EMAIL_OAUTH_CONFIG_BUILDER.build());

    assertThatThrownBy(() -> underTest.updateConfiguration(updateRequest))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Email configuration doesn't exist.");
  }

  @ParameterizedTest
  @MethodSource("configUpdateParamConstraints")
  void updateConfiguration_shouldApplyParamConstraints(ConfigTypeAndOrigin configTypeAndOrigin, List<Param> params, boolean shouldThrow, String errorMessage) {
    UpdateEmailConfigurationRequest updateRequest = prepareUpdateRequestFromParams(configTypeAndOrigin, params);

    if (shouldThrow) {
      assertThatThrownBy(() -> underTest.updateConfiguration(updateRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(errorMessage);
    } else {
      EmailConfiguration updatedConfig = underTest.updateConfiguration(updateRequest);
      assertUpdatesMadeFromParams(configTypeAndOrigin, params, updatedConfig);
    }
  }

  static Object[][] configUpdateParamConstraints() {
    return new Object[][]{
      // OAuth URLs update constraints
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_CONFIG, List.of(), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("host", ParamOrigin.REQUEST, "newHost")), true, OAUTH_URLS_ERROR_MESSAGE},
      {ConfigTypeAndOrigin.OAUTH_BY_CONFIG, List.of(new Param("host", ParamOrigin.REQUEST, "newHost")), true, OAUTH_URLS_ERROR_MESSAGE},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("host", ParamOrigin.REQUEST, "newHost"), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_CONFIG, List.of(new Param("host", ParamOrigin.REQUEST, "newHost"), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthAuthenticationHost", ParamOrigin.REQUEST, "newAuthHost")), true, OAUTH_URLS_ERROR_MESSAGE},
      {ConfigTypeAndOrigin.OAUTH_BY_CONFIG, List.of(new Param("oauthAuthenticationHost", ParamOrigin.REQUEST, "newAuthHost")), true, OAUTH_URLS_ERROR_MESSAGE},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthAuthenticationHost", ParamOrigin.REQUEST, "newAuthHost"), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_CONFIG, List.of(new Param("oauthAuthenticationHost", ParamOrigin.REQUEST, "newAuthHost"), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), false, ""},
      // Basic URLs update constraints
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(), false, ""},
      {ConfigTypeAndOrigin.BASIC_BY_CONFIG, List.of(), false, ""},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("host", ParamOrigin.REQUEST, "newHost")), true, BASIC_URLS_ERROR_MESSAGE},
      {ConfigTypeAndOrigin.BASIC_BY_CONFIG, List.of(new Param("host", ParamOrigin.REQUEST, "newHost")), true, BASIC_URLS_ERROR_MESSAGE},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("host", ParamOrigin.REQUEST, "newHost"), new Param("basicPassword", ParamOrigin.REQUEST, "newPassword")), false, ""},
      {ConfigTypeAndOrigin.BASIC_BY_CONFIG, List.of(new Param("host", ParamOrigin.REQUEST, "newHost"), new Param("basicPassword", ParamOrigin.REQUEST, "newPassword")), false, ""},
      // OAuth param existence update constraints
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthAuthenticationHost", ParamOrigin.CONFIG, "")), true, "OAuth authentication host is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthAuthenticationHost", ParamOrigin.CONFIG, ""), new Param("oauthAuthenticationHost", ParamOrigin.REQUEST, ""), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), true, "OAuth authentication host is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthAuthenticationHost", ParamOrigin.CONFIG, ""), new Param("oauthAuthenticationHost", ParamOrigin.REQUEST, "newHost"), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthClientId", ParamOrigin.CONFIG, "")), true, "OAuth client id is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthClientId", ParamOrigin.CONFIG, ""), new Param("oauthClientId", ParamOrigin.REQUEST, "")), true, "OAuth client id is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthClientId", ParamOrigin.CONFIG, ""), new Param("oauthClientId", ParamOrigin.REQUEST, "newId")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthClientSecret", ParamOrigin.CONFIG, "")), true, "OAuth client secret is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthClientSecret", ParamOrigin.CONFIG, ""), new Param("oauthClientSecret", ParamOrigin.REQUEST, "")), true, "OAuth client secret is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthClientSecret", ParamOrigin.CONFIG, ""), new Param("oauthClientSecret", ParamOrigin.REQUEST, "newSecret")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthTenant", ParamOrigin.CONFIG, "")), true, "OAuth tenant is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthTenant", ParamOrigin.CONFIG, ""), new Param("oauthTenant", ParamOrigin.REQUEST, "")), true, "OAuth tenant is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("oauthTenant", ParamOrigin.CONFIG, ""), new Param("oauthTenant", ParamOrigin.REQUEST, "newTenant")), false, ""},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("username", ParamOrigin.CONFIG, "")), true, "Username is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("username", ParamOrigin.CONFIG, ""), new Param("username", ParamOrigin.REQUEST, "")), true, "Username is required."},
      {ConfigTypeAndOrigin.OAUTH_BY_REQUEST, List.of(new Param("username", ParamOrigin.CONFIG, ""), new Param("username", ParamOrigin.REQUEST, "newUsername")), false, ""},
      // Basic param existence update constraints
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("username", ParamOrigin.CONFIG, "")), true, "Username is required."},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("username", ParamOrigin.CONFIG, ""), new Param("username", ParamOrigin.REQUEST, "")), true, "Username is required."},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("username", ParamOrigin.CONFIG, ""), new Param("username", ParamOrigin.REQUEST, "newUsername")), false, ""},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("basicPassword", ParamOrigin.CONFIG, "")), true, "Password is required."},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("basicPassword", ParamOrigin.CONFIG, ""), new Param("basicPassword", ParamOrigin.REQUEST, "")), true, "Password is required."},
      {ConfigTypeAndOrigin.BASIC_BY_REQUEST, List.of(new Param("basicPassword", ParamOrigin.CONFIG, ""), new Param("basicPassword", ParamOrigin.REQUEST, "newPassword")), false, ""},
    };
  }

  @Test
  void updateConfiguration_whenConfigExists_shouldUpdateConfig() {
    underTest.createConfiguration(EMAIL_BASIC_CONFIG_BUILDER.build());
    EmailConfiguration newConfig = EMAIL_OAUTH_CONFIG_BUILDER.build();
    UpdateEmailConfigurationRequest updateRequest = getUpdateEmailConfigurationRequestFromConfig(newConfig);

    EmailConfiguration updatedConfig = underTest.updateConfiguration(updateRequest);

    assertThatConfigurationIsCorrect(newConfig, updatedConfig);
  }

  private static UpdateEmailConfigurationRequest getUpdateEmailConfigurationRequestFromConfig(EmailConfiguration updatedConfig) {
    return builder()
      .emailConfigurationId(updatedConfig.id())
      .host(withValueOrThrow(updatedConfig.host()))
      .port(withValueOrThrow(updatedConfig.port()))
      .securityProtocol(withValueOrThrow(updatedConfig.securityProtocol()))
      .fromAddress(withValueOrThrow(updatedConfig.fromAddress()))
      .fromName(withValueOrThrow(updatedConfig.fromName()))
      .subjectPrefix(withValueOrThrow(updatedConfig.subjectPrefix()))
      .authMethod(withValueOrThrow(updatedConfig.authMethod()))
      .username(withValueOrThrow(updatedConfig.username()))
      .basicPassword(withValueOrThrow(updatedConfig.basicPassword()))
      .oauthAuthenticationHost(withValueOrThrow(updatedConfig.oauthAuthenticationHost()))
      .oauthClientId(withValueOrThrow(updatedConfig.oauthClientId()))
      .oauthClientSecret(withValueOrThrow(updatedConfig.oauthClientSecret()))
      .oauthTenant(withValueOrThrow(updatedConfig.oauthTenant()))
      .build();
  }

  private void assertThatConfigurationIsCorrect(EmailConfiguration expectedConfig, EmailConfiguration actualConfig) {
    assertThat(actualConfig.id()).isEqualTo(expectedConfig.id());
    assertThat(actualConfig.host()).isEqualTo(expectedConfig.host());
    assertThat(actualConfig.port()).isEqualTo(expectedConfig.port());
    assertThat(actualConfig.securityProtocol()).isEqualTo(expectedConfig.securityProtocol());
    assertThat(actualConfig.fromAddress()).isEqualTo(expectedConfig.fromAddress());
    assertThat(actualConfig.fromName()).isEqualTo(expectedConfig.fromName());
    assertThat(actualConfig.subjectPrefix()).isEqualTo(expectedConfig.subjectPrefix());
    assertThat(actualConfig.authMethod()).isEqualTo(expectedConfig.authMethod());
    assertThat(actualConfig.username()).isEqualTo(expectedConfig.username());
    assertThat(actualConfig.basicPassword()).isEqualTo(expectedConfig.basicPassword());
    assertThat(actualConfig.oauthAuthenticationHost()).isEqualTo(expectedConfig.oauthAuthenticationHost());
    assertThat(actualConfig.oauthClientId()).isEqualTo(expectedConfig.oauthClientId());
    assertThat(actualConfig.oauthClientSecret()).isEqualTo(expectedConfig.oauthClientSecret());
    assertThat(actualConfig.oauthTenant()).isEqualTo(expectedConfig.oauthTenant());
    assertThat(actualConfig.oauthScope()).isEqualTo(expectedConfig.oauthScope());
    assertThat(actualConfig.oauthGrant()).isEqualTo(expectedConfig.oauthGrant());
  }

  @Test
  void deleteConfiguration_whenConfigDoesNotExist_shouldThrow() {
    assertThatThrownBy(() -> underTest.deleteConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Email configuration doesn't exist.");
  }

  @Test
  void deleteConfiguration_whenConfigExists_shouldDeleteConfig() {
    underTest.createConfiguration(EMAIL_BASIC_CONFIG_BUILDER.build());

    underTest.deleteConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID);

    assertThatThrownBy(() -> underTest.getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Email configuration doesn't exist.");
  }


  private UpdateEmailConfigurationRequest prepareUpdateRequestFromParams(ConfigTypeAndOrigin configTypeAndOrigin, List<Param> params) {
    createOriginalConfiguration(configTypeAndOrigin, params);
    UpdateEmailConfigurationRequest.Builder requestBuilder = getOriginalBuilder();

    switch (configTypeAndOrigin) {
      case OAUTH_BY_REQUEST, OAUTH_BY_CONFIG:
        requestBuilder.authMethod(withValueOrThrow(EmailConfigurationAuthMethod.OAUTH));
        break;
      case BASIC_BY_REQUEST, BASIC_BY_CONFIG:
        requestBuilder.authMethod(withValueOrThrow(EmailConfigurationAuthMethod.BASIC));
        break;
      default:
        throw new IllegalArgumentException(format("Invalid test input: config %s not supported", configTypeAndOrigin.name()));
    }

    for (Param param : params) {
      if (param.paramOrigin.equals(ParamOrigin.REQUEST)) {
        switch (param.paramName()) {
          case "host":
            requestBuilder.host(withValueOrThrow(param.value()));
            break;
          case "basicPassword":
            requestBuilder.basicPassword(withValueOrThrow(param.value()));
            break;
          case "username":
            requestBuilder.username(withValueOrThrow(param.value()));
            break;
          case "oauthAuthenticationHost":
            requestBuilder.oauthAuthenticationHost(withValueOrThrow(param.value()));
            break;
          case "oauthClientSecret":
            requestBuilder.oauthClientSecret(withValueOrThrow(param.value()));
            break;
          case "oauthClientId":
            requestBuilder.oauthClientId(withValueOrThrow(param.value()));
            break;
          case "oauthTenant":
            requestBuilder.oauthTenant(withValueOrThrow(param.value()));
            break;
          default:
            throw new IllegalArgumentException(format("Invalid test input: param %s not supported.", param.paramName()));
        }
      }
    }

    return requestBuilder.build();
  }

  private void createOriginalConfiguration(ConfigTypeAndOrigin configTypeAndOrigin, List<Param> params) {
    EmailConfigurationBuilder configBuilder;

    if (configTypeAndOrigin == ConfigTypeAndOrigin.OAUTH_BY_CONFIG || configTypeAndOrigin == ConfigTypeAndOrigin.OAUTH_BY_REQUEST) {
      configBuilder = EMAIL_OAUTH_CONFIG_BUILDER;
    } else {
      configBuilder = EMAIL_BASIC_CONFIG_BUILDER;
    }

    underTest.createConfiguration(configBuilder.build());

    // We manually alter the Param config to bypass service constraints of EmailConfigurationService.createConfiguration()
    Map<String, String> paramNameToPropertyKey = Map.of(
      "username", EMAIL_CONFIG_SMTP_USERNAME,
      "basicPassword", EMAIL_CONFIG_SMTP_PASSWORD,
      "oauthAuthenticationHost", EMAIL_CONFIG_SMTP_OAUTH_HOST,
      "oauthClientId", EMAIL_CONFIG_SMTP_OAUTH_CLIENTID,
      "oauthClientSecret", EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET,
      "oauthTenant", EMAIL_CONFIG_SMTP_OAUTH_TENANT
    );
    params.stream()
      .filter(param -> param.paramOrigin.equals(ParamOrigin.CONFIG))
      .forEach(param -> setInternalProperty(dbTester.getSession(), paramNameToPropertyKey.get(param.paramName()), param.value));
    dbTester.commit();

  }

  private void setInternalProperty(DbSession dbSession, String propertyName, @Nullable String value) {
    if (StringUtils.isBlank(value)) {
      dbTester.getDbClient().internalPropertiesDao().delete(dbSession, propertyName);
    } else {
      dbTester.getDbClient().internalPropertiesDao().save(dbSession, propertyName, value);
    }
  }

  private static UpdateEmailConfigurationRequest.Builder getOriginalBuilder() {
    return builder()
      .emailConfigurationId(UNIQUE_EMAIL_CONFIGURATION_ID)
      .host(undefined())
      .port(undefined())
      .securityProtocol(undefined())
      .fromAddress(undefined())
      .fromName(undefined())
      .subjectPrefix(undefined())
      .authMethod(undefined())
      .username(undefined())
      .basicPassword(undefined())
      .oauthAuthenticationHost(undefined())
      .oauthClientId(undefined())
      .oauthClientSecret(undefined())
      .oauthTenant(undefined());
  }

  private void assertUpdatesMadeFromParams(ConfigTypeAndOrigin configTypeAndOrigin, List<Param> params, EmailConfiguration updatedConfig) {
    for (Param param : params) {
      if (param.paramOrigin.equals(ParamOrigin.REQUEST)) {
        switch (param.paramName()) {
          case "host":
            assertThat(updatedConfig.host()).isEqualTo(param.value());
            break;
          case "basicPassword":
            assertThat(updatedConfig.basicPassword()).isEqualTo(param.value());
            break;
          case "username":
            assertThat(updatedConfig.username()).isEqualTo(param.value());
            break;
          case "oauthAuthenticationHost":
            assertThat(updatedConfig.oauthAuthenticationHost()).isEqualTo(param.value());
            break;
          case "oauthClientId":
            assertThat(updatedConfig.oauthClientId()).isEqualTo(param.value());
            break;
          case "oauthClientSecret":
            assertThat(updatedConfig.oauthClientSecret()).isEqualTo(param.value());
            break;
          case "oauthTenant":
            assertThat(updatedConfig.oauthTenant()).isEqualTo(param.value());
            break;
          default:
            throw new IllegalArgumentException(format("Invalid test input: param %s not supported.", param.paramName()));
        }
      }
    }

    if (configTypeAndOrigin == ConfigTypeAndOrigin.OAUTH_BY_REQUEST) {
      assertThat(updatedConfig.authMethod()).isEqualTo(EmailConfigurationAuthMethod.OAUTH);
    }
    if (configTypeAndOrigin == ConfigTypeAndOrigin.BASIC_BY_REQUEST) {
      assertThat(updatedConfig.authMethod()).isEqualTo(EmailConfigurationAuthMethod.BASIC);
    }
  }

  private enum ConfigTypeAndOrigin {
    BASIC_BY_CONFIG, BASIC_BY_REQUEST, OAUTH_BY_CONFIG, OAUTH_BY_REQUEST;
  }

  private enum ParamOrigin {
    REQUEST, CONFIG;
  }

  private record Param(String paramName, ParamOrigin paramOrigin, @Nullable String value) {}

}

