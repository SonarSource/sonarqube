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
package org.sonar.server.common.github.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.auth.github.GitHubIdentityProvider;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.provisioning.GithubOrganizationGroupDto;
import org.sonar.db.user.ExternalGroupDto;
import org.sonar.server.common.gitlab.config.ProvisioningType;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.setting.ThreadLocalSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
import static org.sonar.server.common.NonNullUpdatedValue.withValueOrThrow;
import static org.sonar.server.common.github.config.GithubConfigurationService.UNIQUE_GITHUB_CONFIGURATION_ID;
import static org.sonar.server.common.github.config.UpdateGithubConfigurationRequest.builder;
import static org.sonar.server.common.gitlab.config.ProvisioningType.AUTO_PROVISIONING;
import static org.sonar.server.common.gitlab.config.ProvisioningType.JIT;

@RunWith(MockitoJUnitRunner.class)
public class GithubConfigurationServiceIT {

  @Rule
  public DbTester dbTester = DbTester.create();

  @Mock
  private ManagedInstanceService managedInstanceService;

  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;

  @Mock
  private ThreadLocalSettings threadLocalSettings;

  private GithubConfigurationService githubConfigurationService;

  @Before
  public void setUp() {
    when(managedInstanceService.getProviderName()).thenReturn("github");
    githubConfigurationService = new GithubConfigurationService(
      dbTester.getDbClient(),
      managedInstanceService,
      githubGlobalSettingsValidator,
      threadLocalSettings);
  }

  @Test
  public void getConfiguration_whenIdIsNotGithubConfiguration_throwsException() {
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> githubConfigurationService.getConfiguration("not-github-configuration"))
      .withMessage("GitHub configuration with id not-github-configuration not found");

    assertThat(githubConfigurationService.findConfigurations()).isEmpty();
  }

  @Test
  public void getConfiguration_whenNoConfiguration_throwsNotFoundException() {
    assertThatThrownBy(() -> githubConfigurationService.getConfiguration("github-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitHub configuration doesn't exist.");

    assertThat(githubConfigurationService.findConfigurations()).isEmpty();
  }

  @Test
  public void getConfiguration_whenConfigurationSet_returnsConfig() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));

    GithubConfiguration configuration = githubConfigurationService.getConfiguration("github-configuration");

    assertConfigurationFields(configuration);

    assertThat(githubConfigurationService.findConfigurations()).contains(configuration);
  }

  @Test
  public void getConfiguration_whenConfigurationSetAndEmpty_returnsConfig() {
    dbTester.properties().insertProperty(GITHUB_ENABLED, "true", null);
    dbTester.properties().insertProperty(GITHUB_ORGANIZATIONS, "", null);

    GithubConfiguration configuration = githubConfigurationService.getConfiguration("github-configuration");

    assertThat(configuration.id()).isEqualTo("github-configuration");
    assertThat(configuration.enabled()).isTrue();
    assertThat(configuration.clientId()).isEmpty();
    assertThat(configuration.clientSecret()).isEmpty();
    assertThat(configuration.applicationId()).isEmpty();
    assertThat(configuration.privateKey()).isEmpty();
    assertThat(configuration.synchronizeGroups()).isFalse();
    assertThat(configuration.apiUrl()).isEmpty();
    assertThat(configuration.webUrl()).isEmpty();
    assertThat(configuration.allowedOrganizations()).isEmpty();
    assertThat(configuration.provisioningType()).isEqualTo(JIT);
    assertThat(configuration.allowUsersToSignUp()).isFalse();
    assertThat(configuration.provisionProjectVisibility()).isFalse();
    assertThat(configuration.userConsentRequiredAfterUpgrade()).isFalse();
  }

  @Test
  public void updateConfiguration_whenIdIsNotGithubConfiguration_throwsException() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));
    UpdateGithubConfigurationRequest updateGithubConfigurationRequest = builder().githubConfigurationId("not-github-configuration").build();
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> githubConfigurationService.updateConfiguration(updateGithubConfigurationRequest))
      .withMessage("GitHub configuration with id not-github-configuration not found");
  }

  @Test
  public void updateConfiguration_whenConfigurationDoesntExist_throwsException() {
    UpdateGithubConfigurationRequest updateGithubConfigurationRequest = builder().githubConfigurationId("github-configuration").build();
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> githubConfigurationService.updateConfiguration(updateGithubConfigurationRequest))
      .withMessage("GitHub configuration doesn't exist.");
  }

  @Test
  public void updateConfiguration_whenAllUpdateFieldDefined_updatesEverything() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(JIT));

    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(true))
      .clientId(withValueOrThrow("clientId"))
      .clientSecret(withValueOrThrow("clientSecret"))
      .applicationId(withValueOrThrow("applicationId"))
      .privateKey(withValueOrThrow("privateKey"))
      .synchronizeGroups(withValueOrThrow(true))
      .apiUrl(withValueOrThrow("apiUrl"))
      .webUrl(withValueOrThrow("webUrl"))
      .allowedOrganizations(withValueOrThrow(new LinkedHashSet<>(List.of("org1", "org2", "org3"))))
      .provisioningType(withValueOrThrow(AUTO_PROVISIONING))
      .allowUsersToSignUp(withValueOrThrow(true))
      .projectVisibility(withValueOrThrow(true))
      .userConsentRequiredAfterUpgrade(withValueOrThrow(true))
      .build();

    GithubConfiguration githubConfiguration = githubConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasSet(GITHUB_ENABLED, "true");
    verifySettingWasSet(GITHUB_CLIENT_ID, "clientId");
    verifySettingWasSet(GITHUB_CLIENT_SECRET, "clientSecret");
    verifySettingWasSet(GITHUB_APP_ID, "applicationId");
    verifySettingWasSet(GITHUB_PRIVATE_KEY, "privateKey");
    verifySettingWasSet(GITHUB_GROUPS_SYNC, "true");
    verifySettingWasSet(GITHUB_API_URL, "apiUrl");
    verifySettingWasSet(GITHUB_WEB_URL, "webUrl");
    verifySettingWasSet(GITHUB_ORGANIZATIONS, "org1,org2,org3");
    verifyInternalSettingWasSet(GITHUB_PROVISIONING, "true");
    verifySettingWasSet(GITHUB_ALLOW_USERS_TO_SIGN_UP, "true");
    verifySettingWasSet(GITHUB_PROVISION_PROJECT_VISIBILITY, "true");
    verifySettingExistsButEmpty(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);
    verify(managedInstanceService).queueSynchronisationTask();

    assertConfigurationFields(githubConfiguration);
  }

  @Test
  public void updateConfiguration_whenAllUpdateFieldDefinedAndSetToFalse_updatesEverything() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    clearInvocations(managedInstanceService);

    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(false))
      .synchronizeGroups(withValueOrThrow(false))
      .provisioningType(withValueOrThrow(JIT))
      .allowUsersToSignUp(withValueOrThrow(false))
      .build();

    githubConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasSet(GITHUB_ENABLED, "false");
    verifySettingWasSet(GITHUB_GROUPS_SYNC, "false");
    verifyInternalSettingWasSet(GITHUB_PROVISIONING, "false");
    verifySettingWasSet(GITHUB_ALLOW_USERS_TO_SIGN_UP, "false");
    verifyNoMoreInteractions(managedInstanceService);

  }

  @Test
  public void updateConfiguration_whenSwitchingFromAutoToJit_shouldNotScheduleSyncAndCallManagedInstanceChecker() {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("12", "12", GitHubIdentityProvider.KEY));
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("34", "34", GitHubIdentityProvider.KEY));
    dbTester.getDbClient().githubOrganizationGroupDao().insert(dbSession, new GithubOrganizationGroupDto("14", "org1", "group1"));
    dbSession.commit();

    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    reset(managedInstanceService);

    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .provisioningType(withValueOrThrow(JIT))
      .build();

    githubConfigurationService.updateConfiguration(updateRequest);

    verifyNoMoreInteractions(managedInstanceService);
    assertThat(dbTester.getDbClient().externalGroupDao().selectByIdentityProvider(dbTester.getSession(), GitHubIdentityProvider.KEY)).isEmpty();
    assertThat(dbTester.getDbClient().githubOrganizationGroupDao().findAll(dbTester.getSession())).isEmpty();
  }

  @Test
  public void updateConfiguration_whenSwitchingToAutoProvisioningAndTheConfigIsNotEnabled_shouldThrow() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(JIT));

    UpdateGithubConfigurationRequest disableRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(false))
      .build();

    githubConfigurationService.updateConfiguration(disableRequest);

    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .provisioningType(withValueOrThrow(AUTO_PROVISIONING))
      .build();

    assertThatThrownBy(() -> githubConfigurationService.updateConfiguration(updateRequest))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("GitHub authentication must be turned on to enable GitHub provisioning.");
    verify(managedInstanceService, times(0)).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenURLChangesWithoutSecret_shouldFail() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(JIT));

    UpdateGithubConfigurationRequest updateUrlRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .apiUrl(withValueOrThrow("http://malicious.url"))
      .build();

    assertThatThrownBy(() -> githubConfigurationService.updateConfiguration(updateUrlRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("For security reasons, API and Web urls can't be updated without providing the private key.");
  }

  @Test
  public void updateConfiguration_whenURLChangesWithAllSecrets_shouldUpdate() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(JIT));

    UpdateGithubConfigurationRequest updateUrlRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .apiUrl(withValueOrThrow("http://new.url"))
      .privateKey(withValueOrThrow("new-private-key"))
      .build();

    githubConfigurationService.updateConfiguration(updateUrlRequest);

    verifySettingWasSet(GITHUB_API_URL, "http://new.url");
    verifySettingWasSet(GITHUB_PRIVATE_KEY, "new-private-key");
  }

  @Test
  public void updateConfiguration_whenDisablingUserConsentFlagAndJITProvisioning_shouldNotScheduleSync() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));
    reset(managedInstanceService);
    verifySettingExistsButEmpty(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);


    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .userConsentRequiredAfterUpgrade(withValueOrThrow(false))
      .provisioningType(withValueOrThrow(JIT))
      .build();

    githubConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasDeleted(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);
    verify(managedInstanceService, never()).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenDisablingUserConsentFlagAndAUTOProvisioning_shouldScheduleSync() {
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));
    reset(managedInstanceService);
    verifySettingExistsButEmpty(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);


    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .userConsentRequiredAfterUpgrade(withValueOrThrow(false))
      .build();

    githubConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasDeleted(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);
    verify(managedInstanceService).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenDisablingUserConsentFlagAndUserConsentFlagAlreadyDisabled_shouldNotScheduleSync() {
    githubConfigurationService.createConfiguration(buildGithubConfiguration(AUTO_PROVISIONING, false));
    reset(managedInstanceService);


    UpdateGithubConfigurationRequest updateRequest = builder()
      .githubConfigurationId(UNIQUE_GITHUB_CONFIGURATION_ID)
      .userConsentRequiredAfterUpgrade(withValueOrThrow(false))
      .build();

    githubConfigurationService.updateConfiguration(updateRequest);

    verify(managedInstanceService, never()).queueSynchronisationTask();
  }

  private static void assertConfigurationFields(GithubConfiguration configuration) {
    assertThat(configuration).isNotNull();
    assertThat(configuration.id()).isEqualTo("github-configuration");
    assertThat(configuration.enabled()).isTrue();
    assertThat(configuration.clientId()).isEqualTo("clientId");
    assertThat(configuration.clientSecret()).isEqualTo("clientSecret");
    assertThat(configuration.applicationId()).isEqualTo("applicationId");
    assertThat(configuration.privateKey()).isEqualTo("privateKey");
    assertThat(configuration.synchronizeGroups()).isTrue();
    assertThat(configuration.apiUrl()).isEqualTo("apiUrl");
    assertThat(configuration.webUrl()).isEqualTo("webUrl");
    assertThat(configuration.allowedOrganizations()).containsExactlyInAnyOrder("org1", "org2", "org3");
    assertThat(configuration.provisioningType()).isEqualTo(AUTO_PROVISIONING);
    assertThat(configuration.allowUsersToSignUp()).isTrue();
    assertThat(configuration.provisionProjectVisibility()).isTrue();
    assertThat(configuration.userConsentRequiredAfterUpgrade()).isTrue();
  }

  @Test
  public void createConfiguration_whenConfigurationAlreadyExists_shouldThrow() {
    GithubConfiguration githubConfiguration = buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING);
    githubConfigurationService.createConfiguration(githubConfiguration);

    assertThatThrownBy(() -> githubConfigurationService.createConfiguration(githubConfiguration))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("GitHub configuration already exists. Only one GitHub configuration is supported.");
  }

  @Test
  public void createConfiguration_whenAutoProvisioning_shouldCreateCorrectConfigurationAndScheduleSync() {
    GithubConfiguration configuration = buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING);

    GithubConfiguration createdConfiguration = githubConfigurationService.createConfiguration(configuration);

    assertConfigurationIsCorrect(configuration, createdConfiguration);

    verifyCommonSettings(configuration);

    verify(managedInstanceService).queueSynchronisationTask();

  }

  @Test
  public void createConfiguration_whenInstanceIsExternallyManaged_shouldThrow() {
    GithubConfiguration configuration = buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING);

    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(managedInstanceService.getProviderName()).thenReturn("not-github");

    assertThatIllegalStateException()
      .isThrownBy(() -> githubConfigurationService.createConfiguration(configuration))
      .withMessage("It is not possible to synchronize SonarQube using GitHub, as it is already managed by not-github.");

  }

  @Test
  public void createConfiguration_whenJitProvisioning_shouldCreateCorrectConfiguration() {
    GithubConfiguration configuration = buildGithubConfigurationWithUserConsentTrue(JIT);

    GithubConfiguration createdConfiguration = githubConfigurationService.createConfiguration(configuration);

    assertConfigurationIsCorrect(configuration, createdConfiguration);

    verifyCommonSettings(configuration);
    verifyNoInteractions(managedInstanceService);

  }

  private void verifyCommonSettings(GithubConfiguration configuration) {
    verifySettingWasSet(GITHUB_ENABLED, String.valueOf(configuration.enabled()));
    verifySettingWasSet(GITHUB_CLIENT_ID, configuration.clientId());
    verifySettingWasSet(GITHUB_CLIENT_SECRET, configuration.clientSecret());
    verifySettingWasSet(GITHUB_APP_ID, configuration.applicationId());
    verifySettingWasSet(GITHUB_PRIVATE_KEY, configuration.privateKey());
    verifySettingWasSet(GITHUB_GROUPS_SYNC, String.valueOf(configuration.synchronizeGroups()));
    verifySettingWasSet(GITHUB_API_URL, configuration.apiUrl());
    verifySettingWasSet(GITHUB_WEB_URL, configuration.webUrl());
    verifySettingWasSet(GITHUB_ORGANIZATIONS, String.join(",", configuration.allowedOrganizations()));
    verifyInternalSettingWasSet(GITHUB_PROVISIONING, String.valueOf(configuration.provisioningType() == AUTO_PROVISIONING));
    verifySettingWasSet(GITHUB_ALLOW_USERS_TO_SIGN_UP, String.valueOf(configuration.allowUsersToSignUp()));
    verifySettingWasSet(GITHUB_PROVISION_PROJECT_VISIBILITY, String.valueOf(configuration.provisionProjectVisibility()));
    verifySettingExistsButEmpty(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);
  }

  private void verifySettingWasSet(String setting, @Nullable String value) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(setting).getValue()).isEqualTo(value);
  }

  private void verifyInternalSettingWasSet(String setting, @Nullable String value) {
    assertThat(dbTester.getDbClient().internalPropertiesDao().selectByKey(dbTester.getSession(), setting)).contains(value);
  }

  private void verifySettingExistsButEmpty(String setting) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(setting)).isNotNull();
  }

  private void verifySettingWasDeleted(String setting) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(setting)).isNull();
  }

  @Test
  public void deleteConfiguration_whenIdIsNotGithubConfiguration_throwsException() {
    assertThatThrownBy(() -> githubConfigurationService.deleteConfiguration("not-github-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitHub configuration with id not-github-configuration not found");
  }

  @Test
  public void deleteConfiguration_whenConfigurationDoesntExist_throwsException() {
    assertThatThrownBy(() -> githubConfigurationService.deleteConfiguration("github-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitHub configuration doesn't exist.");
  }

  @Test
  public void deleteConfiguration_whenConfigurationExists_shouldDeleteConfiguration() {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("12", "12", GitHubIdentityProvider.KEY));
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("34", "34", GitHubIdentityProvider.KEY));
    dbSession.commit();
    githubConfigurationService.createConfiguration(buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING));
    githubConfigurationService.deleteConfiguration("github-configuration");

    assertPropertyIsDeleted(GITHUB_ENABLED);
    assertPropertyIsDeleted(GITHUB_CLIENT_ID);
    assertPropertyIsDeleted(GITHUB_CLIENT_SECRET);
    assertPropertyIsDeleted(GITHUB_APP_ID);
    assertPropertyIsDeleted(GITHUB_PRIVATE_KEY);
    assertPropertyIsDeleted(GITHUB_GROUPS_SYNC);
    assertPropertyIsDeleted(GITHUB_API_URL);
    assertPropertyIsDeleted(GITHUB_WEB_URL);
    assertPropertyIsDeleted(GITHUB_ORGANIZATIONS);
    assertInternalPropertyIsDeleted(GITHUB_PROVISIONING);
    assertPropertyIsDeleted(GITHUB_ALLOW_USERS_TO_SIGN_UP);
    assertPropertyIsDeleted(GITHUB_PROVISION_PROJECT_VISIBILITY);
    assertPropertyIsDeleted(GITHUB_USER_CONSENT_FOR_PERMISSIONS_REQUIRED_AFTER_UPGRADE);

    assertThat(dbTester.getDbClient().externalGroupDao().selectByIdentityProvider(dbTester.getSession(), GitHubIdentityProvider.KEY)).isEmpty();
  }

  private void assertPropertyIsDeleted(String property) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(property)).isNull();
  }

  private void assertInternalPropertyIsDeleted(String property) {
    assertThat(dbTester.getDbClient().internalPropertiesDao().selectByKey(dbTester.getSession(), property)).isEmpty();
  }

  @Test
  public void validate_whenConfigurationIsDisabled_shouldNotValidate() {
    GithubConfiguration githubConfiguration = buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING);
    when(githubConfiguration.enabled()).thenReturn(false);

    githubConfigurationService.validate(githubConfiguration);

    verifyNoInteractions(githubGlobalSettingsValidator);
  }

  @Test
  public void validate_whenConfigurationIsValidAndJIT_returnEmptyOptional() {
    GithubConfiguration configuration = buildGithubConfigurationWithUserConsentTrue(JIT);
    when(configuration.enabled()).thenReturn(true);

    githubConfigurationService.validate(configuration);

    verify(githubGlobalSettingsValidator)
      .validate(configuration.applicationId(), configuration.clientId(), configuration.clientSecret(), configuration.privateKey(), configuration.apiUrl());
  }

  @Test
  public void validate_whenConfigurationIsValidAndAutoProvisioning_returnEmptyOptional() {
    GithubConfiguration configuration = buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING);
    when(configuration.enabled()).thenReturn(true);

    githubConfigurationService.validate(configuration);

    verify(githubGlobalSettingsValidator)
      .validate(configuration.applicationId(), configuration.clientId(), configuration.clientSecret(), configuration.privateKey(), configuration.apiUrl());
  }

  @Test
  public void validate_whenConfigurationIsInValid_returnsExceptionMessage() {
    GithubConfiguration configuration = buildGithubConfigurationWithUserConsentTrue(AUTO_PROVISIONING);
    when(configuration.enabled()).thenReturn(true);

    Exception exception = new IllegalStateException("Invalid configuration");
    when(githubConfigurationService.validate(configuration)).thenThrow(exception);

    Optional<String> message = githubConfigurationService.validate(configuration);

    assertThat(message).contains("Invalid configuration");
  }

  private static GithubConfiguration buildGithubConfigurationWithUserConsentTrue(ProvisioningType provisioningType) {
    return buildGithubConfiguration(provisioningType, true);
  }

  private static GithubConfiguration buildGithubConfiguration(ProvisioningType provisioningType, boolean userConsentRequiredAfterUpgrade) {
    GithubConfiguration githubConfiguration = mock();
    when(githubConfiguration.id()).thenReturn("github-configuration");
    when(githubConfiguration.enabled()).thenReturn(true);
    when(githubConfiguration.clientId()).thenReturn("clientId");
    when(githubConfiguration.clientSecret()).thenReturn("clientSecret");
    when(githubConfiguration.applicationId()).thenReturn("applicationId");
    when(githubConfiguration.privateKey()).thenReturn("privateKey");
    when(githubConfiguration.synchronizeGroups()).thenReturn(true);
    when(githubConfiguration.apiUrl()).thenReturn("apiUrl");
    when(githubConfiguration.webUrl()).thenReturn("webUrl");
    when(githubConfiguration.allowedOrganizations()).thenReturn(new LinkedHashSet<>(Set.of("org1", "org2", "org3")));
    when(githubConfiguration.provisioningType()).thenReturn(provisioningType);
    when(githubConfiguration.allowUsersToSignUp()).thenReturn(true);
    when(githubConfiguration.provisionProjectVisibility()).thenReturn(true);
    when(githubConfiguration.userConsentRequiredAfterUpgrade()).thenReturn(userConsentRequiredAfterUpgrade);
    return githubConfiguration;
  }

  private static void assertConfigurationIsCorrect(GithubConfiguration expectedConfiguration, GithubConfiguration actualConfiguration) {
    assertThat(actualConfiguration.id()).isEqualTo(expectedConfiguration.id());
    assertThat(actualConfiguration.enabled()).isEqualTo(expectedConfiguration.enabled());
    assertThat(actualConfiguration.clientId()).isEqualTo(expectedConfiguration.clientId());
    assertThat(actualConfiguration.clientSecret()).isEqualTo(expectedConfiguration.clientSecret());
    assertThat(actualConfiguration.applicationId()).isEqualTo(expectedConfiguration.applicationId());
    assertThat(actualConfiguration.privateKey()).isEqualTo(expectedConfiguration.privateKey());
    assertThat(actualConfiguration.synchronizeGroups()).isEqualTo(expectedConfiguration.synchronizeGroups());
    assertThat(actualConfiguration.apiUrl()).isEqualTo(expectedConfiguration.apiUrl());
    assertThat(actualConfiguration.webUrl()).isEqualTo(expectedConfiguration.webUrl());
    assertThat(actualConfiguration.allowedOrganizations()).containsExactlyInAnyOrderElementsOf(expectedConfiguration.allowedOrganizations());
    assertThat(actualConfiguration.provisioningType()).isEqualTo(expectedConfiguration.provisioningType());
    assertThat(actualConfiguration.allowUsersToSignUp()).isEqualTo(expectedConfiguration.allowUsersToSignUp());
    assertThat(actualConfiguration.provisionProjectVisibility()).isEqualTo(expectedConfiguration.provisionProjectVisibility());
    assertThat(actualConfiguration.userConsentRequiredAfterUpgrade()).isEqualTo(expectedConfiguration.userConsentRequiredAfterUpgrade());
  }
}
