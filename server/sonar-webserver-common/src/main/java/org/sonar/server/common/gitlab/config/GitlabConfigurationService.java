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
package org.sonar.server.common.gitlab.config;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.gitlab.GitLabIdentityProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.common.UpdatedValue;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.setting.ThreadLocalSettings;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator.ValidationMode.AUTH_ONLY;
import static org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator.ValidationMode.COMPLETE;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOWED_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_APPLICATION_ID;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_TOKEN;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SECRET;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SYNC_USER_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;
import static org.sonar.server.common.gitlab.config.ProvisioningType.AUTO_PROVISIONING;
import static org.sonar.server.common.gitlab.config.ProvisioningType.JIT;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

@ServerSide
public class GitlabConfigurationService {

  private static final List<String> GITLAB_CONFIGURATION_PROPERTIES = List.of(
    GITLAB_AUTH_ENABLED,
    GITLAB_AUTH_APPLICATION_ID,
    GITLAB_AUTH_URL,
    GITLAB_AUTH_SECRET,
    GITLAB_AUTH_SYNC_USER_GROUPS,
    GITLAB_AUTH_ALLOWED_GROUPS,
    GITLAB_AUTH_PROVISIONING_ENABLED,
    GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP,
    GITLAB_AUTH_PROVISIONING_TOKEN);

  public static final String UNIQUE_GITLAB_CONFIGURATION_ID = "gitlab-configuration";
  private final DbClient dbClient;
  private final ManagedInstanceService managedInstanceService;
  private final GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  private final ThreadLocalSettings threadLocalSettings;

  public GitlabConfigurationService(DbClient dbClient,
    ManagedInstanceService managedInstanceService, GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator, ThreadLocalSettings threadLocalSettings) {
    this.dbClient = dbClient;
    this.managedInstanceService = managedInstanceService;
    this.gitlabGlobalSettingsValidator = gitlabGlobalSettingsValidator;
    this.threadLocalSettings = threadLocalSettings;
  }

  public GitlabConfiguration updateConfiguration(UpdateGitlabConfigurationRequest updateRequest) {
    UpdatedValue<Boolean> provisioningEnabled = updateRequest.provisioningType().map(GitlabConfigurationService::shouldEnableAutoProvisioning);
    throwIfUrlIsUpdatedWithoutSecrets(updateRequest);
    try (DbSession dbSession = dbClient.openSession(true)) {
      throwIfConfigurationDoesntExist(dbSession);
      GitlabConfiguration currentConfiguration = getConfiguration(updateRequest.gitlabConfigurationId(), dbSession);

      ProvisioningType provisioningType = updateRequest.provisioningType().orElse(currentConfiguration.provisioningType());
      Set<String> allowedGroups = updateRequest.allowedGroups().orElse(currentConfiguration.allowedGroups());
      throwIfAllowedGroupsEmptyAndAutoProvisioning(provisioningType, allowedGroups);

      setIfDefined(dbSession, GITLAB_AUTH_ENABLED, updateRequest.enabled().map(String::valueOf));
      setIfDefined(dbSession, GITLAB_AUTH_APPLICATION_ID, updateRequest.applicationId());
      setIfDefined(dbSession, GITLAB_AUTH_URL, updateRequest.url());
      setIfDefined(dbSession, GITLAB_AUTH_SECRET, updateRequest.secret());
      setIfDefined(dbSession, GITLAB_AUTH_SYNC_USER_GROUPS, updateRequest.synchronizeGroups().map(String::valueOf));
      setIfDefined(dbSession, GITLAB_AUTH_ALLOWED_GROUPS, updateRequest.allowedGroups().map(groups -> String.join(",", groups)));
      setIfDefined(dbSession, GITLAB_AUTH_PROVISIONING_ENABLED, provisioningEnabled.map(String::valueOf));
      setIfDefined(dbSession, GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, updateRequest.allowUsersToSignUp().map(String::valueOf));
      setIfDefined(dbSession, GITLAB_AUTH_PROVISIONING_TOKEN, updateRequest.provisioningToken());
      boolean shouldTriggerProvisioning = provisioningEnabled.orElse(false) && !currentConfiguration.provisioningType().equals(AUTO_PROVISIONING);
      deleteExternalGroupsWhenDisablingAutoProvisioning(dbSession, currentConfiguration, updateRequest.provisioningType());
      GitlabConfiguration updatedConfiguration = getConfiguration(UNIQUE_GITLAB_CONFIGURATION_ID, dbSession);
      if (shouldTriggerProvisioning) {
        triggerRun(updatedConfiguration);
      }
      dbSession.commit();
      return updatedConfiguration;
    }
  }

  private static void throwIfUrlIsUpdatedWithoutSecrets(UpdateGitlabConfigurationRequest request) {
    if (request.url().isDefined()) {
      checkArgument(request.secret().isDefined(), "For security reasons, the URL can't be updated without providing the secret.");
    }
  }

  private void setIfDefined(DbSession dbSession, String propertyName, UpdatedValue<String> value) {
    value
      .map(definedValue -> new PropertyDto().setKey(propertyName).setValue(definedValue))
      .applyIfDefined(property -> dbClient.propertiesDao().saveProperty(dbSession, property));
    threadLocalSettings.setProperty(propertyName, value.orElse(null));
  }

  private void deleteExternalGroupsWhenDisablingAutoProvisioning(
    DbSession dbSession,
    GitlabConfiguration currentConfiguration,
    UpdatedValue<ProvisioningType> provisioningTypeFromUpdate) {
    boolean disableAutoProvisioning = provisioningTypeFromUpdate.map(provisioningType -> provisioningType.equals(JIT)).orElse(false)
      && currentConfiguration.provisioningType().equals(AUTO_PROVISIONING);
    if (disableAutoProvisioning) {
      dbClient.externalGroupDao().deleteByExternalIdentityProvider(dbSession, GitLabIdentityProvider.KEY);
    }
  }

  public GitlabConfiguration getConfiguration(String id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfNotUniqueConfigurationId(id);
      throwIfConfigurationDoesntExist(dbSession);
      return getConfiguration(id, dbSession);
    }
  }

  public Optional<GitlabConfiguration> findConfigurations() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (dbClient.propertiesDao().selectGlobalProperty(dbSession, GITLAB_AUTH_ENABLED) == null) {
        return Optional.empty();
      }
      return Optional.of(getConfiguration(UNIQUE_GITLAB_CONFIGURATION_ID, dbSession));
    }
  }

  private Boolean getBooleanOrFalse(DbSession dbSession, String property) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, property))
      .map(dto -> Boolean.valueOf(dto.getValue())).orElse(false);
  }

  private String getStringPropertyOrEmpty(DbSession dbSession, String property) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, property))
      .map(PropertyDto::getValue).orElse("");
  }

  private String getStringPropertyOrNull(DbSession dbSession, String property) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, property))
      .map(dto -> Strings.emptyToNull(dto.getValue())).orElse(null);
  }

  private static void throwIfNotUniqueConfigurationId(String id) {
    if (!UNIQUE_GITLAB_CONFIGURATION_ID.equals(id)) {
      throw new NotFoundException(format("Gitlab configuration with id %s not found", id));
    }
  }

  public void deleteConfiguration(String id) {
    throwIfNotUniqueConfigurationId(id);
    try (DbSession dbSession = dbClient.openSession(false)) {
      throwIfConfigurationDoesntExist(dbSession);
      GITLAB_CONFIGURATION_PROPERTIES.forEach(property -> dbClient.propertiesDao().deleteGlobalProperty(property, dbSession));
      dbClient.externalGroupDao().deleteByExternalIdentityProvider(dbSession, GitLabIdentityProvider.KEY);
      dbSession.commit();
    }
  }

  private void throwIfConfigurationDoesntExist(DbSession dbSession) {
    checkFound(dbClient.propertiesDao().selectGlobalProperty(dbSession, GITLAB_AUTH_ENABLED), "GitLab configuration doesn't exist.");
  }

  private static ProvisioningType toProvisioningType(boolean provisioningEnabled) {
    return provisioningEnabled ? AUTO_PROVISIONING : JIT;
  }

  public GitlabConfiguration createConfiguration(GitlabConfiguration configuration) {
    throwIfConfigurationAlreadyExists();
    throwIfAllowedGroupsEmptyAndAutoProvisioning(configuration.provisioningType(), configuration.allowedGroups());

    boolean enableAutoProvisioning = shouldEnableAutoProvisioning(configuration.provisioningType());
    try (DbSession dbSession = dbClient.openSession(false)) {
      setProperty(dbSession, GITLAB_AUTH_ENABLED, String.valueOf(configuration.enabled()));
      setProperty(dbSession, GITLAB_AUTH_APPLICATION_ID, configuration.applicationId());
      setProperty(dbSession, GITLAB_AUTH_URL, configuration.url());
      setProperty(dbSession, GITLAB_AUTH_SECRET, configuration.secret());
      setProperty(dbSession, GITLAB_AUTH_SYNC_USER_GROUPS, String.valueOf(configuration.synchronizeGroups()));
      setProperty(dbSession, GITLAB_AUTH_ALLOWED_GROUPS, String.join(",", configuration.allowedGroups()));
      setProperty(dbSession, GITLAB_AUTH_PROVISIONING_ENABLED, String.valueOf(enableAutoProvisioning));
      setProperty(dbSession, GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, String.valueOf(configuration.allowUsersToSignUp()));
      setProperty(dbSession, GITLAB_AUTH_PROVISIONING_TOKEN, configuration.provisioningToken());
      if (enableAutoProvisioning) {
        triggerRun(configuration);
      }
      GitlabConfiguration createdConfiguration = getConfiguration(UNIQUE_GITLAB_CONFIGURATION_ID, dbSession);
      dbSession.commit();
      return createdConfiguration;
    }

  }

  private void throwIfConfigurationAlreadyExists() {
    Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(GITLAB_AUTH_ENABLED)).ifPresent(property -> {
      throw BadRequestException.create("GitLab configuration already exists. Only one Gitlab configuration is supported.");
    });
  }

  private static void throwIfAllowedGroupsEmptyAndAutoProvisioning(ProvisioningType provisioningType, Set<String> allowedGroups) {
    if (provisioningType == AUTO_PROVISIONING && allowedGroups.isEmpty()) {
      throw new IllegalArgumentException("allowedGroups cannot be empty when Auto-provisioning is enabled.");
    }
  }

  private static boolean shouldEnableAutoProvisioning(ProvisioningType provisioningType) {
    return AUTO_PROVISIONING.equals(provisioningType);
  }

  private void setProperty(DbSession dbSession, String propertyName, @Nullable String value) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(propertyName).setValue(value));
  }

  private GitlabConfiguration getConfiguration(String id, DbSession dbSession) {
    throwIfNotUniqueConfigurationId(id);
    throwIfConfigurationDoesntExist(dbSession);
    return new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      getBooleanOrFalse(dbSession, GITLAB_AUTH_ENABLED),
      getStringPropertyOrEmpty(dbSession, GITLAB_AUTH_APPLICATION_ID),
      getStringPropertyOrEmpty(dbSession, GITLAB_AUTH_URL),
      getStringPropertyOrEmpty(dbSession, GITLAB_AUTH_SECRET),
      getBooleanOrFalse(dbSession, GITLAB_AUTH_SYNC_USER_GROUPS),
      getAllowedGroups(dbSession),
      getBooleanOrFalse(dbSession, GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP),
      toProvisioningType(getBooleanOrFalse(dbSession, GITLAB_AUTH_PROVISIONING_ENABLED)),
      getStringPropertyOrNull(dbSession, GITLAB_AUTH_PROVISIONING_TOKEN));
  }

  private Set<String> getAllowedGroups(DbSession dbSession) {
    return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, GITLAB_AUTH_ALLOWED_GROUPS))
      .map(dto -> Arrays.stream(dto.getValue().split(","))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet()))
      .orElse(Set.of());
  }

  public void triggerRun() {
    GitlabConfiguration configuration = getConfiguration(UNIQUE_GITLAB_CONFIGURATION_ID);
    triggerRun(configuration);
  }

  private void triggerRun(GitlabConfiguration gitlabConfiguration) {
    throwIfConfigIncompleteOrInstanceAlreadyManaged(gitlabConfiguration);
    managedInstanceService.queueSynchronisationTask();
  }

  private void throwIfConfigIncompleteOrInstanceAlreadyManaged(GitlabConfiguration configuration) {
    checkInstanceNotManagedByAnotherProvider();
    checkState(AUTO_PROVISIONING.equals(configuration.provisioningType()), "Auto provisioning must be activated");
    checkState(configuration.enabled(), getErrorMessage("GitLab authentication must be turned on"));
    checkState(isNotBlank(configuration.provisioningToken()), getErrorMessage("Provisioning token must be set"));
  }

  private void checkInstanceNotManagedByAnotherProvider() {
    if (managedInstanceService.isInstanceExternallyManaged()) {
      Optional.of(managedInstanceService.getProviderName()).filter(providerName -> !"gitlab".equals(providerName))
        .ifPresent(providerName -> {
          throw new IllegalStateException("It is not possible to synchronize SonarQube using GitLab, as it is already managed by "
            + providerName + ".");
        });
    }
  }

  private static String getErrorMessage(String prefix) {
    return format("%s to enable GitLab provisioning.", prefix);
  }

  public Optional<String> validate(GitlabConfiguration gitlabConfiguration) {
    if (!gitlabConfiguration.enabled()) {
      return Optional.empty();
    }
    String url = (gitlabConfiguration.url() + "/api/v4").replace("//", "/");
    try {
      gitlabGlobalSettingsValidator.validate(
        toValidationMode(gitlabConfiguration.provisioningType()),
        url,
        gitlabConfiguration.provisioningToken());
    } catch (Exception e) {
      return Optional.of(e.getMessage());
    }
    return Optional.empty();
  }

  private static GitlabGlobalSettingsValidator.ValidationMode toValidationMode(ProvisioningType provisioningType) {
    return AUTO_PROVISIONING.equals(provisioningType) ? COMPLETE : AUTH_ONLY;
  }
}
