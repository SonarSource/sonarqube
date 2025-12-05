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
package org.sonar.server.common.github.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GitHubIdentityProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.common.UpdatedValue;
import org.sonar.server.common.gitlab.config.ProvisioningType;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.setting.ThreadLocalSettings;

import static java.lang.String.format;
import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.auth.github.GitHubSettings.GITHUB_ALLOW_USERS_TO_SIGN_UP;
import static org.sonar.auth.github.GitHubSettings.GITHUB_API_URL;
import static org.sonar.auth.github.GitHubSettings.GITHUB_APP_ID;
import static org.sonar.auth.github.GitHubSettings.GITHUB_CLIENT_ID;
import static org.sonar.auth.github.GitHubSettings.GITHUB_CLIENT_SECRET;
import static org.sonar.auth.github.GitHubSettings.GITHUB_ENABLED;
import static org.sonar.auth.github.GitHubSettings.GITHUB_GROUPS_SYNC;
import static org.sonar.auth.github.GitHubSettings.GITHUB_ORGANIZATIONS;
import static org.sonar.auth.github.GitHubSettings.GITHUB_PRIVATE_KEY;
import static org.sonar.auth.github.GitHubSettings.GITHUB_PROVISIONING;
import static org.sonar.auth.github.GitHubSettings.GITHUB_PROVISION_PROJECT_VISIBILITY;
import static org.sonar.auth.github.GitHubSettings.GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE;
import static org.sonar.auth.github.GitHubSettings.GITHUB_WEB_URL;
import static org.sonar.server.common.gitlab.config.ProvisioningType.AUTO_PROVISIONING;
import static org.sonar.server.common.gitlab.config.ProvisioningType.JIT;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonarqube.ws.WsUtils.checkArgument;

@ServerSide
public class GithubConfigurationService {

  private static final List<String> GITHUB_CONFIGURATION_PROPERTIES = List.of(
    GITHUB_ENABLED,
    GITHUB_CLIENT_ID,
    GITHUB_CLIENT_SECRET,
    GITHUB_APP_ID,
    GITHUB_PRIVATE_KEY,
    GITHUB_GROUPS_SYNC,
    GITHUB_API_URL,
    GITHUB_WEB_URL,
    GITHUB_ORGANIZATIONS,
    GITHUB_ALLOW_USERS_TO_SIGN_UP,
    GITHUB_PROVISION_PROJECT_VISIBILITY,
    GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);

  public static final String UNIQUE_GITHUB_CONFIGURATION_ID = "github-configuration";
  private final DbClient dbClient;
  private final ManagedInstanceService managedInstanceService;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final ThreadLocalSettings threadLocalSettings;

  public GithubConfigurationService(DbClient dbClient,
    ManagedInstanceService managedInstanceService, GithubGlobalSettingsValidator githubGlobalSettingsValidator, ThreadLocalSettings threadLocalSettings) {
    this.dbClient = dbClient;
    this.managedInstanceService = managedInstanceService;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.threadLocalSettings = threadLocalSettings;
  }

  public GithubConfiguration updateConfiguration(UpdateGithubConfigurationRequest updateRequest) {
    UpdatedValue<Boolean> provisioningEnabled = updateRequest.provisioningType().map(GithubConfigurationService::isTypeAutoProvisioning);
    throwIfUrlIsUpdatedWithoutPrivateKey(updateRequest);
    try (DbSession dbSession = dbClient.openSession(true)) {
      throwIfConfigurationDoesntExist(dbSession);
      GithubConfiguration currentConfiguration = getConfiguration(updateRequest.githubConfigurationId(), dbSession);

      setIfDefined(dbSession, GITHUB_ENABLED, updateRequest.enabled().map(String::valueOf));
      setIfDefined(dbSession, GITHUB_CLIENT_ID, updateRequest.clientId());
      setIfDefined(dbSession, GITHUB_CLIENT_SECRET, updateRequest.clientSecret());
      setIfDefined(dbSession, GITHUB_APP_ID, updateRequest.applicationId());
      setIfDefined(dbSession, GITHUB_PRIVATE_KEY, updateRequest.privateKey());
      setIfDefined(dbSession, GITHUB_GROUPS_SYNC, updateRequest.synchronizeGroups().map(String::valueOf));
      setIfDefined(dbSession, GITHUB_API_URL, updateRequest.apiUrl());
      setIfDefined(dbSession, GITHUB_WEB_URL, updateRequest.webUrl());
      setIfDefined(dbSession, GITHUB_ORGANIZATIONS, updateRequest.allowedOrganizations().map(orgs -> String.join(",", orgs)));
      setInternalIfDefined(dbSession, GITHUB_PROVISIONING, provisioningEnabled.map(String::valueOf));
      setIfDefined(dbSession, GITHUB_ALLOW_USERS_TO_SIGN_UP, updateRequest.allowUsersToSignUp().map(String::valueOf));
      setIfDefined(dbSession, GITHUB_PROVISION_PROJECT_VISIBILITY, updateRequest.projectVisibility().map(String::valueOf));
      insertOrDeleteAsEmptyIfDefined(dbSession, GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE, updateRequest.userConsentRequiredAfterUpgrade().contains(true));

      deleteExternalGroupsWhenDisablingAutoProvisioning(dbSession, currentConfiguration, updateRequest.provisioningType());
      dbSession.commit();

      GithubConfiguration updatedConfiguration = getConfiguration(UNIQUE_GITHUB_CONFIGURATION_ID, dbSession);
      if (shouldTriggerProvisioning(provisioningEnabled, currentConfiguration, updateRequest)) {
        triggerRun(updatedConfiguration);
      }

      return updatedConfiguration;
    }
  }

  private static boolean shouldTriggerProvisioning(UpdatedValue<Boolean> provisioningEnabled, GithubConfiguration currentConfiguration,
    UpdateGithubConfigurationRequest updateRequest) {
    return shouldTriggerProvisioningAfterTypeChange(provisioningEnabled, currentConfiguration)
      || shouldTriggerProvisioningAfterUserConsent(updateRequest, currentConfiguration);
  }

  private static boolean shouldTriggerProvisioningAfterTypeChange(UpdatedValue<Boolean> provisioningEnabled, GithubConfiguration currentConfiguration) {
    return provisioningEnabled.orElse(false)
      && !currentConfiguration.provisioningType().equals(AUTO_PROVISIONING);
  }

  private static boolean shouldTriggerProvisioningAfterUserConsent(UpdateGithubConfigurationRequest updateRequest,
    GithubConfiguration currentConfiguration) {
    boolean wasUserConsentRequired = currentConfiguration.userConsentRequiredAfterUpgrade();
    boolean userConsentProvidedForAutoProvisioning = !updateRequest.provisioningType().contains(JIT) && updateRequest.userConsentRequiredAfterUpgrade().contains(false);
    return wasUserConsentRequired && userConsentProvidedForAutoProvisioning;
  }

  private static void throwIfUrlIsUpdatedWithoutPrivateKey(UpdateGithubConfigurationRequest request) {
    if (request.apiUrl().isDefined() || request.webUrl().isDefined()) {
      checkArgument(request.privateKey().isDefined(), "For security reasons, API and Web urls can't be updated without providing the private key.");
    }
  }

  private void setIfDefined(DbSession dbSession, String propertyName, UpdatedValue<String> value) {
    value
      .map(definedValue -> new PropertyDto().setKey(propertyName).setValue(definedValue))
      .applyIfDefined(property -> dbClient.propertiesDao().saveProperty(dbSession, property));
    threadLocalSettings.setProperty(propertyName, value.orElse(null));
  }

  private void insertOrDeleteAsEmptyIfDefined(DbSession dbSession, String propertyName, boolean value) {
    if (value) {
      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(propertyName));
    } else {
      dbClient.propertiesDao().deleteGlobalProperty(propertyName, dbSession);
    }
    threadLocalSettings.setProperty(propertyName, value);
  }

  private void setInternalIfDefined(DbSession dbSession, String propertyName, UpdatedValue<String> value) {
    value.applyIfDefined(v -> dbClient.internalPropertiesDao().save(dbSession, propertyName, v));
  }

  private void deleteExternalGroupsWhenDisablingAutoProvisioning(DbSession dbSession, GithubConfiguration currentConfiguration,
    UpdatedValue<ProvisioningType> provisioningTypeFromUpdate) {
    if (shouldDisableAutoProvisioning(currentConfiguration, provisioningTypeFromUpdate)) {
      dbClient.externalGroupDao().deleteByExternalIdentityProvider(dbSession, GitHubIdentityProvider.KEY);
      dbClient.githubOrganizationGroupDao().deleteAll(dbSession);
      dbSession.commit();
    }
  }

  private static boolean shouldDisableAutoProvisioning(GithubConfiguration currentConfiguration, UpdatedValue<ProvisioningType> provisioningTypeFromUpdate) {
    return provisioningTypeFromUpdate.map(provisioningType -> provisioningType.equals(JIT)).orElse(false)
      && currentConfiguration.provisioningType().equals(AUTO_PROVISIONING);
  }

  public GithubConfiguration getConfiguration(String id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfNotUniqueConfigurationId(id);
      throwIfConfigurationDoesntExist(dbSession);
      return getConfiguration(id, dbSession);
    }
  }

  public Optional<GithubConfiguration> findConfigurations() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (dbClient.propertiesDao().selectGlobalProperty(dbSession, GITHUB_ENABLED) == null) {
        return Optional.empty();
      }
      return Optional.of(getConfiguration(UNIQUE_GITHUB_CONFIGURATION_ID, dbSession));
    }
  }

  private Boolean getBooleanOrFalse(DbSession dbSession, String property) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, property))
      .map(dto -> Boolean.valueOf(dto.getValue())).orElse(false);
  }

  private Boolean getBooleanOrFalseFromEmptyProperty(DbSession dbSession, String property) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, property))
      .isPresent();
  }

  private Boolean getInternalBooleanOrFalse(DbSession dbSession, String property) {
    return dbClient.internalPropertiesDao().selectByKey(dbSession, property)
      .map(Boolean::valueOf)
      .orElse(false);
  }

  private String getStringPropertyOrEmpty(DbSession dbSession, String property) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, property))
      .map(PropertyDto::getValue).orElse("");
  }

  private static void throwIfNotUniqueConfigurationId(String id) {
    if (!UNIQUE_GITHUB_CONFIGURATION_ID.equals(id)) {
      throw new NotFoundException(format("GitHub configuration with id %s not found", id));
    }
  }

  public void deleteConfiguration(String id) {
    throwIfNotUniqueConfigurationId(id);
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfConfigurationDoesntExist(dbSession);
      GITHUB_CONFIGURATION_PROPERTIES.forEach(property -> dbClient.propertiesDao().deleteGlobalProperty(property, dbSession));
      dbClient.internalPropertiesDao().delete(dbSession, GITHUB_PROVISIONING);
      dbClient.externalGroupDao().deleteByExternalIdentityProvider(dbSession, GitHubIdentityProvider.KEY);
      dbSession.commit();
    }
  }

  private void throwIfConfigurationDoesntExist(DbSession dbSession) {
    checkFound(dbClient.propertiesDao().selectGlobalProperty(dbSession, GITHUB_ENABLED), "GitHub configuration doesn't exist.");
  }

  private static ProvisioningType toProvisioningType(boolean provisioningEnabled) {
    return provisioningEnabled ? AUTO_PROVISIONING : JIT;
  }

  public GithubConfiguration createConfiguration(GithubConfiguration configuration) {
    throwIfConfigurationAlreadyExists();

    boolean enableAutoProvisioning = isTypeAutoProvisioning(configuration.provisioningType());
    try (DbSession dbSession = dbClient.openSession(false)) {
      setProperty(dbSession, GITHUB_ENABLED, String.valueOf(configuration.enabled()));
      setProperty(dbSession, GITHUB_CLIENT_ID, configuration.clientId());
      setProperty(dbSession, GITHUB_CLIENT_SECRET, configuration.clientSecret());
      setProperty(dbSession, GITHUB_APP_ID, configuration.applicationId());
      setProperty(dbSession, GITHUB_PRIVATE_KEY, configuration.privateKey());
      setProperty(dbSession, GITHUB_GROUPS_SYNC, String.valueOf(configuration.synchronizeGroups()));
      setProperty(dbSession, GITHUB_API_URL, configuration.apiUrl());
      setProperty(dbSession, GITHUB_WEB_URL, configuration.webUrl());
      setProperty(dbSession, GITHUB_ORGANIZATIONS, String.join(",", configuration.allowedOrganizations()));
      setInternalProperty(dbSession, GITHUB_PROVISIONING, String.valueOf(enableAutoProvisioning));
      setProperty(dbSession, GITHUB_ALLOW_USERS_TO_SIGN_UP, String.valueOf(configuration.allowUsersToSignUp()));
      setProperty(dbSession, GITHUB_PROVISION_PROJECT_VISIBILITY, String.valueOf(configuration.provisionProjectVisibility()));
      setPropertyAsEmpty(dbSession, GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE, configuration.userConsentRequiredAfterUpgrade());
      if (enableAutoProvisioning) {
        triggerRun(configuration);
      }
      GithubConfiguration createdConfiguration = getConfiguration(UNIQUE_GITHUB_CONFIGURATION_ID, dbSession);
      dbSession.commit();
      return createdConfiguration;
    }

  }

  private void throwIfConfigurationAlreadyExists() {
    Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(GITHUB_ENABLED)).ifPresent(property -> {
      throw BadRequestException.create("GitHub configuration already exists. Only one GitHub configuration is supported.");
    });
  }

  private static boolean isTypeAutoProvisioning(ProvisioningType provisioningType) {
    return AUTO_PROVISIONING.equals(provisioningType);
  }

  private void setProperty(DbSession dbSession, String propertyName, @Nullable String value) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(propertyName).setValue(value));
  }

  private void setPropertyAsEmpty(DbSession dbSession, String propertyName, boolean value) {
    if (value) {
      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(propertyName));
    }
  }

  private void setInternalProperty(DbSession dbSession, String propertyName, @Nullable String value) {
    if (StringUtils.isNotEmpty(value)) {
      dbClient.internalPropertiesDao().save(dbSession, propertyName, value);
    }
  }

  private GithubConfiguration getConfiguration(String id, DbSession dbSession) {
    throwIfNotUniqueConfigurationId(id);
    throwIfConfigurationDoesntExist(dbSession);
    return new GithubConfiguration(
      UNIQUE_GITHUB_CONFIGURATION_ID,
      getBooleanOrFalse(dbSession, GITHUB_ENABLED),
      getStringPropertyOrEmpty(dbSession, GITHUB_CLIENT_ID),
      getStringPropertyOrEmpty(dbSession, GITHUB_CLIENT_SECRET),
      getStringPropertyOrEmpty(dbSession, GITHUB_APP_ID),
      getStringPropertyOrEmpty(dbSession, GITHUB_PRIVATE_KEY),
      getBooleanOrFalse(dbSession, GITHUB_GROUPS_SYNC),
      getStringPropertyOrEmpty(dbSession, GITHUB_API_URL),
      getStringPropertyOrEmpty(dbSession, GITHUB_WEB_URL),
      getAllowedOrganizations(dbSession),
      toProvisioningType(getInternalBooleanOrFalse(dbSession, GITHUB_PROVISIONING)),
      getBooleanOrFalse(dbSession, GITHUB_ALLOW_USERS_TO_SIGN_UP),
      getBooleanOrFalse(dbSession, GITHUB_PROVISION_PROJECT_VISIBILITY),
      getBooleanOrFalseFromEmptyProperty(dbSession, GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE));
  }

  private Set<String> getAllowedOrganizations(DbSession dbSession) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, GITHUB_ORGANIZATIONS))
      .map(dto -> Arrays.stream(dto.getValue().split(","))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet()))
      .orElse(Set.of());
  }

  private void triggerRun(GithubConfiguration githubConfiguration) {
    throwIfConfigIncompleteOrInstanceAlreadyManaged(githubConfiguration);
    managedInstanceService.queueSynchronisationTask();
  }

  private void throwIfConfigIncompleteOrInstanceAlreadyManaged(GithubConfiguration configuration) {
    checkInstanceNotManagedByAnotherProvider();
    checkState(AUTO_PROVISIONING.equals(configuration.provisioningType()), "Auto provisioning must be activated");
    checkState(configuration.enabled(), getErrorMessage("GitHub authentication must be turned on"));
  }

  private void checkInstanceNotManagedByAnotherProvider() {
    if (managedInstanceService.isInstanceExternallyManaged()) {
      Optional.of(managedInstanceService.getProviderName()).filter(providerName -> !GitHubIdentityProvider.KEY.equals(providerName))
        .ifPresent(providerName -> {
          throw new IllegalStateException("It is not possible to synchronize SonarQube using GitHub, as it is already managed by " + providerName + ".");
        });
    }
  }

  private static String getErrorMessage(String prefix) {
    return format("%s to enable GitHub provisioning.", prefix);
  }

  public Optional<String> validate(GithubConfiguration configuration) {
    if (!configuration.enabled()) {
      return Optional.empty();
    }
    try {
      githubGlobalSettingsValidator.validate(configuration.applicationId(), configuration.clientId(), configuration.clientSecret(), configuration.privateKey(),
        configuration.apiUrl());
    } catch (Exception e) {
      return Optional.of(e.getMessage());
    }
    return Optional.empty();
  }

}
