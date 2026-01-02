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
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.common.NonNullUpdatedValue;
import org.sonar.server.common.UpdatedValue;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_FROM;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_FROM_NAME;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_PREFIX;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_AUTH_METHOD;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_HOST;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_CLIENTID;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_GRANT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_GRANT_DEFAULT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_HOST;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_SCOPE;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_TENANT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_PASSWORD;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_PORT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_SECURE_CONNECTION;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_USERNAME;
import static org.sonarqube.ws.WsUtils.checkArgument;

@ServerSide
public class EmailConfigurationService {

  private static final List<String> EMAIL_CONFIGURATION_PROPERTIES = List.of(
    EMAIL_CONFIG_SMTP_HOST,
    EMAIL_CONFIG_SMTP_PORT,
    EMAIL_CONFIG_SMTP_SECURE_CONNECTION,
    EMAIL_CONFIG_FROM,
    EMAIL_CONFIG_FROM_NAME,
    EMAIL_CONFIG_PREFIX,
    EMAIL_CONFIG_SMTP_AUTH_METHOD,
    EMAIL_CONFIG_SMTP_USERNAME,
    EMAIL_CONFIG_SMTP_PASSWORD,
    EMAIL_CONFIG_SMTP_OAUTH_HOST,
    EMAIL_CONFIG_SMTP_OAUTH_CLIENTID,
    EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET,
    EMAIL_CONFIG_SMTP_OAUTH_TENANT,
    EMAIL_CONFIG_SMTP_OAUTH_SCOPE,
    EMAIL_CONFIG_SMTP_OAUTH_GRANT
    );

  public static final String UNIQUE_EMAIL_CONFIGURATION_ID = "email-configuration";

  private final DbClient dbClient;

  public EmailConfigurationService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public EmailConfiguration createConfiguration(EmailConfiguration configuration) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfConfigurationAlreadyExists(dbSession);
      throwIfParamsConstraintsAreNotMetForCreation(configuration);

      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_HOST, configuration.host());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_PORT, configuration.port());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_SECURE_CONNECTION, configuration.securityProtocol().name());
      setInternalProperty(dbSession, EMAIL_CONFIG_FROM, configuration.fromAddress());
      setInternalProperty(dbSession, EMAIL_CONFIG_FROM_NAME, configuration.fromName());
      setInternalProperty(dbSession, EMAIL_CONFIG_PREFIX, configuration.subjectPrefix());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_AUTH_METHOD, configuration.authMethod().name());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_USERNAME, configuration.username());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_PASSWORD, configuration.basicPassword());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_HOST, configuration.oauthAuthenticationHost());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_CLIENTID, configuration.oauthClientId());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET, configuration.oauthClientSecret());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_TENANT, configuration.oauthTenant());
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_SCOPE, EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT);
      setInternalProperty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_GRANT, EMAIL_CONFIG_SMTP_OAUTH_GRANT_DEFAULT);

      EmailConfiguration createdConfiguration = getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID, dbSession);
      dbSession.commit();
      return createdConfiguration;
    }
  }

  private static void throwIfParamsConstraintsAreNotMetForCreation(EmailConfiguration configuration) {
    if (configuration.authMethod() == EmailConfigurationAuthMethod.OAUTH) {
      checkArgument(StringUtils.isNotEmpty(configuration.oauthAuthenticationHost()), "OAuth authentication host is required.");
      checkArgument(StringUtils.isNotEmpty(configuration.oauthClientId()), "OAuth client id is required.");
      checkArgument(StringUtils.isNotEmpty(configuration.oauthClientSecret()), "OAuth client secret is required.");
      checkArgument(StringUtils.isNotEmpty(configuration.oauthTenant()), "OAuth tenant is required.");
    } else if (configuration.authMethod() == EmailConfigurationAuthMethod.BASIC) {
      checkArgument(StringUtils.isNotEmpty(configuration.basicPassword()), "Password is required.");
    }
  }

  private void throwIfConfigurationAlreadyExists(DbSession dbSession) {
    if (configurationExists(dbSession)) {
      throw BadRequestException.create("Email configuration already exists. Only one Email configuration is supported.");
    }
  }

  public EmailConfiguration getConfiguration(String id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfNotUniqueConfigurationId(id);
      throwIfConfigurationDoesntExist(dbSession);
      return getConfiguration(id, dbSession);
    }
  }

  private EmailConfiguration getConfiguration(String id, DbSession dbSession) {
    throwIfNotUniqueConfigurationId(id);
    throwIfConfigurationDoesntExist(dbSession);
    return new EmailConfiguration(
      UNIQUE_EMAIL_CONFIGURATION_ID,
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_HOST),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_PORT),
      EmailConfigurationSecurityProtocol.valueOf(getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_SECURE_CONNECTION)),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_FROM),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_FROM_NAME),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_PREFIX),
      EmailConfigurationAuthMethod.valueOf(getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_AUTH_METHOD)),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_USERNAME),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_PASSWORD),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_HOST),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_CLIENTID),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_TENANT),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_SCOPE),
      getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_OAUTH_GRANT));
  }

  private static void throwIfNotUniqueConfigurationId(String id) {
    if (!UNIQUE_EMAIL_CONFIGURATION_ID.equals(id)) {
      throw new NotFoundException(format("Email configuration with id %s not found", id));
    }
  }

  private String getStringInternalPropertyOrEmpty(DbSession dbSession, String property) {
    return dbClient.internalPropertiesDao().selectByKey(dbSession, property).orElse("");
  }

  public Optional<EmailConfiguration> findConfigurations() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (configurationExists(dbSession)) {
        return Optional.of(getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID, dbSession));
      }
      return Optional.empty();
    }
  }

  public EmailConfiguration updateConfiguration(UpdateEmailConfigurationRequest updateRequest) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      throwIfConfigurationDoesntExist(dbSession);
      EmailConfiguration existingConfig = getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID, dbSession);
      throwIfUrlIsUpdatedWithoutCredentials(existingConfig, updateRequest);
      throwIfParamsConstraintsAreNotMetForUpdate(existingConfig, updateRequest);

      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_HOST, updateRequest.host());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_PORT, updateRequest.port());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_SECURE_CONNECTION, updateRequest.securityProtocol().map(EmailConfigurationSecurityProtocol::name));
      setInternalIfDefined(dbSession, EMAIL_CONFIG_FROM, updateRequest.fromAddress());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_FROM_NAME, updateRequest.fromName());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_PREFIX, updateRequest.subjectPrefix());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_AUTH_METHOD, updateRequest.authMethod().map(EmailConfigurationAuthMethod::name));
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_USERNAME, updateRequest.username());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_PASSWORD, updateRequest.basicPassword());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_OAUTH_HOST, updateRequest.oauthAuthenticationHost());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_OAUTH_CLIENTID, updateRequest.oauthClientId());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET, updateRequest.oauthClientSecret());
      setInternalIfDefined(dbSession, EMAIL_CONFIG_SMTP_OAUTH_TENANT, updateRequest.oauthTenant());

      dbSession.commit();

      return getConfiguration(UNIQUE_EMAIL_CONFIGURATION_ID, dbSession);
    }
  }

  private static void throwIfUrlIsUpdatedWithoutCredentials(EmailConfiguration existingConfig, UpdateEmailConfigurationRequest request) {
    if (isOauthDefinedByExistingConfigOrRequest(existingConfig, request)) {
      // For OAuth config, we make sure that the client secret is provided when the host or authentication host is updated
      if (isRequestParameterDefined(request.host()) || isRequestParameterDefined(request.oauthAuthenticationHost())) {
        checkArgument(isRequestParameterDefined(request.oauthClientSecret()), "For security reasons, OAuth urls can't be updated without providing the client secret.");
      }
    } else {
      // For Basic config, we make sure that the password is provided when the host is updated
      if (isRequestParameterDefined(request.host())) {
        checkArgument(isRequestParameterDefined(request.basicPassword()), "For security reasons, the host can't be updated without providing the password.");
      }
    }
  }

  private static void throwIfParamsConstraintsAreNotMetForUpdate(EmailConfiguration existingConfig, UpdateEmailConfigurationRequest updateRequest) {
    checkArgument(isFieldDefinedByExistingConfigOrRequest(existingConfig.username(), updateRequest.username()),
      "Username is required.");
    if (isOauthDefinedByExistingConfigOrRequest(existingConfig, updateRequest)) {
      checkArgument(isFieldDefinedByExistingConfigOrRequest(existingConfig.oauthAuthenticationHost(), updateRequest.oauthAuthenticationHost()),
        "OAuth authentication host is required.");
      checkArgument(isFieldDefinedByExistingConfigOrRequest(existingConfig.oauthClientId(), updateRequest.oauthClientId()),
        "OAuth client id is required.");
      checkArgument(isFieldDefinedByExistingConfigOrRequest(existingConfig.oauthClientSecret(), updateRequest.oauthClientSecret()),
        "OAuth client secret is required.");
      checkArgument(isFieldDefinedByExistingConfigOrRequest(existingConfig.oauthTenant(), updateRequest.oauthTenant()),
        "OAuth tenant is required.");
    } else {
      checkArgument(isFieldDefinedByExistingConfigOrRequest(existingConfig.basicPassword(), updateRequest.basicPassword()),
        "Password is required.");
    }
  }

  private static boolean isFieldDefinedByExistingConfigOrRequest(@Nullable String existingParam, NonNullUpdatedValue<String> requestParam) {
    return StringUtils.isNotEmpty(existingParam) || (requestParam.isDefined() && !requestParam.contains(""));
  }

  private static boolean isOauthDefinedByExistingConfigOrRequest(EmailConfiguration existingConfig, UpdateEmailConfigurationRequest request) {
    // Either the request update the config to OAuth, or the existing config is OAuth
    if (isRequestParameterDefined(request.authMethod())) {
      return request.authMethod().contains(EmailConfigurationAuthMethod.OAUTH);
    }
    return existingConfig.authMethod() == EmailConfigurationAuthMethod.OAUTH;
  }

  private static boolean isRequestParameterDefined(@Nullable NonNullUpdatedValue<?> parameter) {
    return parameter != null && parameter.isDefined();
  }

  public void deleteConfiguration(String id) {
    throwIfNotUniqueConfigurationId(id);
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfConfigurationDoesntExist(dbSession);
      EMAIL_CONFIGURATION_PROPERTIES.forEach(propertyKey -> dbClient.internalPropertiesDao().delete(dbSession, propertyKey));
      dbSession.commit();
    }
  }

  private void throwIfConfigurationDoesntExist(DbSession dbSession) {
    if (!configurationExists(dbSession)) {
      throw new NotFoundException("Email configuration doesn't exist.");
    }
  }

  private boolean configurationExists(DbSession dbSession) {
    String property = getStringInternalPropertyOrEmpty(dbSession, EMAIL_CONFIG_SMTP_HOST);
    return StringUtils.isNotEmpty(property);
  }

  private void setInternalIfDefined(DbSession dbSession, String propertyKey, @Nullable UpdatedValue<String> value) {
    if (value != null) {
      value.applyIfDefined(propertyValue -> setInternalProperty(dbSession, propertyKey, propertyValue));
    }
  }

  private void setInternalProperty(DbSession dbSession, String propertyKey, @Nullable String value) {
    if (StringUtils.isNotEmpty(value)) {
      dbClient.internalPropertiesDao().save(dbSession, propertyKey, value);
    } else {
      dbClient.internalPropertiesDao().delete(dbSession, propertyKey);
    }
  }
}
