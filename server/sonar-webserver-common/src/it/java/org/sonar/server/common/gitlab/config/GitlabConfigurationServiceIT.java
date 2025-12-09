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
package org.sonar.server.common.gitlab.config;

import com.google.common.base.Strings;
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
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.auth.gitlab.GitLabIdentityProvider;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.ExternalGroupDto;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator.ValidationMode.AUTH_ONLY;
import static org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator.ValidationMode.COMPLETE;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOWED_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_APPLICATION_ID;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_PROVISIONING_TOKEN;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SECRET;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_SYNC_USER_GROUPS;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;
import static org.sonar.server.common.NonNullUpdatedValue.withValueOrThrow;
import static org.sonar.server.common.UpdatedValue.withValue;
import static org.sonar.server.common.gitlab.config.GitlabConfigurationService.UNIQUE_GITLAB_CONFIGURATION_ID;
import static org.sonar.server.common.gitlab.config.ProvisioningType.AUTO_PROVISIONING;
import static org.sonar.server.common.gitlab.config.ProvisioningType.JIT;
import static org.sonar.server.common.gitlab.config.UpdateGitlabConfigurationRequest.builder;

@RunWith(MockitoJUnitRunner.class)
public class GitlabConfigurationServiceIT {

  @Rule
  public DbTester dbTester = DbTester.create();

  @Mock
  private ManagedInstanceService managedInstanceService;

  @Mock
  private GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;

  @Mock
  private ThreadLocalSettings threadLocalSettings;

  private GitlabConfigurationService gitlabConfigurationService;

  @Before
  public void setUp() {
    when(managedInstanceService.getProviderName()).thenReturn("gitlab");
    gitlabConfigurationService = new GitlabConfigurationService(
      dbTester.getDbClient(),
      managedInstanceService,
      gitlabGlobalSettingsValidator,
      threadLocalSettings);
  }

  @Test
  public void getConfiguration_whenIdIsNotGitlabConfiguration_throwsException() {
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> gitlabConfigurationService.getConfiguration("not-gitlab-configuration"))
      .withMessage("Gitlab configuration with id not-gitlab-configuration not found");
  }

  @Test
  public void getConfiguration_whenNoConfiguration_throwsNotFoundException() {
    assertThatThrownBy(() -> gitlabConfigurationService.getConfiguration("gitlab-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitLab configuration doesn't exist.");

  }

  @Test
  public void getConfiguration_whenConfigurationSet_returnsConfig() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));

    GitlabConfiguration configuration = gitlabConfigurationService.getConfiguration("gitlab-configuration");

    assertConfigurationFields(configuration);
  }

  @Test
  public void getConfiguration_whenConfigurationSetAndEmpty_returnsConfig() {
    dbTester.properties().insertProperty(GITLAB_AUTH_ENABLED, "true", null);
    dbTester.properties().insertProperty(GITLAB_AUTH_ALLOWED_GROUPS, "", null);

    GitlabConfiguration configuration = gitlabConfigurationService.getConfiguration("gitlab-configuration");

    assertThat(configuration.id()).isEqualTo("gitlab-configuration");
    assertThat(configuration.enabled()).isTrue();
    assertThat(configuration.applicationId()).isEmpty();
    assertThat(configuration.url()).isEmpty();
    assertThat(configuration.secret()).isEmpty();
    assertThat(configuration.synchronizeGroups()).isFalse();
    assertThat(configuration.allowedGroups()).isEmpty();
    assertThat(configuration.provisioningType()).isEqualTo(JIT);
    assertThat(configuration.allowUsersToSignUp()).isFalse();
    assertThat(configuration.provisioningToken()).isNull();
  }

  @Test
  public void updateConfiguration_whenIdIsNotGitlabConfiguration_throwsException() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = builder().gitlabConfigurationId("not-gitlab-configuration").build();
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest))
      .withMessage("Gitlab configuration with id not-gitlab-configuration not found");
  }

  @Test
  public void updateConfiguration_whenConfigurationDoesntExist_throwsException() {
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = builder().gitlabConfigurationId("gitlab-configuration").build();
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest))
      .withMessage("GitLab configuration doesn't exist.");
  }

  @Test
  public void updateConfiguration_whenAllowedGroupsIsEmptyAndAutoProvisioningIsEnabled_throwsException() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    UpdateGitlabConfigurationRequest updateGitlabConfigurationRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .allowedGroups(withValueOrThrow(new LinkedHashSet<>()))
      .build();

    assertThatThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateGitlabConfigurationRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("allowedGroups cannot be empty when Auto-provisioning is enabled.");
  }

  @Test
  public void updateConfiguration_whenAllUpdateFieldDefined_updatesEverything() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(JIT));

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(true))
      .applicationId(withValueOrThrow("applicationId"))
      .url(withValueOrThrow("url"))
      .secret(withValueOrThrow("secret"))
      .synchronizeGroups(withValueOrThrow(true))
      .allowedGroups(withValueOrThrow(new LinkedHashSet<>(List.of("group1", "group2", "group3"))))
      .provisioningType(withValueOrThrow(AUTO_PROVISIONING))
      .allowUserToSignUp(withValueOrThrow(true))
      .provisioningToken(withValueOrThrow("provisioningToken"))
      .build();

    GitlabConfiguration gitlabConfiguration = gitlabConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasSet(GITLAB_AUTH_ENABLED, "true");
    verifySettingWasSet(GITLAB_AUTH_APPLICATION_ID, "applicationId");
    verifySettingWasSet(GITLAB_AUTH_URL, "url");
    verifySettingWasSet(GITLAB_AUTH_SECRET, "secret");
    verifySettingWasSet(GITLAB_AUTH_SYNC_USER_GROUPS, "true");
    verifySettingWasSet(GITLAB_AUTH_ALLOWED_GROUPS, "group1,group2,group3");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_ENABLED, "true");
    verifySettingWasSet(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "true");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_TOKEN, "provisioningToken");
    verify(managedInstanceService).queueSynchronisationTask();

    assertConfigurationFields(gitlabConfiguration);
  }

  @Test
  public void updateConfiguration_whenAllUpdateFieldDefinedAndSetToFalse_updatesEverything() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    clearInvocations(managedInstanceService);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(false))
      .synchronizeGroups(withValueOrThrow(false))
      .provisioningType(withValueOrThrow(JIT))
      .allowUserToSignUp(withValueOrThrow(false))
      .build();

    gitlabConfigurationService.updateConfiguration(updateRequest);

    verifySettingWasSet(GITLAB_AUTH_ENABLED, "false");
    verifySettingWasSet(GITLAB_AUTH_SYNC_USER_GROUPS, "false");
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_ENABLED, "false");
    verifySettingWasSet(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, "false");
    verifyNoMoreInteractions(managedInstanceService);

  }

  @Test
  public void updateConfiguration_whenSwitchingFromAutoToJit_shouldNotScheduleSyncAndCallManagedInstanceChecker() {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("12", "12", GitLabIdentityProvider.KEY));
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("34", "34", GitLabIdentityProvider.KEY));
    dbSession.commit();

    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    reset(managedInstanceService);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningToken(withValue(null))
      .provisioningType(withValueOrThrow(JIT))
      .build();

    gitlabConfigurationService.updateConfiguration(updateRequest);

    verifyNoMoreInteractions(managedInstanceService);
    assertThat(dbTester.getDbClient().externalGroupDao().selectByIdentityProvider(dbTester.getSession(), GitLabIdentityProvider.KEY)).isEmpty();
  }

  @Test
  public void updateConfiguration_whenResettingAutoFromAuto_shouldTriggerSync() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    verify(managedInstanceService).queueSynchronisationTask();
    reset(managedInstanceService);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningType(withValueOrThrow(AUTO_PROVISIONING))
      .build();

    gitlabConfigurationService.updateConfiguration(updateRequest);

    verify(managedInstanceService).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenSwitchingToAutoProvisioningAndTheConfigIsNotEnabled_shouldThrow() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(JIT));

    UpdateGitlabConfigurationRequest disableRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .enabled(withValueOrThrow(false))
      .build();

    gitlabConfigurationService.updateConfiguration(disableRequest);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningType(withValueOrThrow(AUTO_PROVISIONING))
      .build();

    assertThatThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateRequest))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("GitLab authentication must be turned on to enable GitLab provisioning.");
    verify(managedInstanceService, times(0)).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenSwitchingToAutoProvisioningAndProvisioningTokenIsNotDefined_shouldThrow() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(JIT));

    UpdateGitlabConfigurationRequest removeTokenRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningToken(withValue(null))
      .build();

    gitlabConfigurationService.updateConfiguration(removeTokenRequest);

    UpdateGitlabConfigurationRequest updateRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .provisioningType(withValueOrThrow(AUTO_PROVISIONING))
      .build();

    assertThatThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateRequest))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Provisioning token must be set to enable GitLab provisioning.");
    verify(managedInstanceService, times(0)).queueSynchronisationTask();
  }

  @Test
  public void updateConfiguration_whenURLChangesWithoutSecret_shouldFail() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(JIT));

    UpdateGitlabConfigurationRequest updateUrlRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .url(withValueOrThrow("http://malicious.url"))
      .build();

    assertThatThrownBy(() -> gitlabConfigurationService.updateConfiguration(updateUrlRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("For security reasons, the URL can't be updated without providing the secret.");
  }

  @Test
  public void updateConfiguration_whenURLChangesWithAllSecrets_shouldUpdate() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(JIT));

    UpdateGitlabConfigurationRequest updateUrlRequest = builder()
      .gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID)
      .url(withValueOrThrow("http://new.url"))
      .secret(withValueOrThrow("new_secret"))
      .build();

    gitlabConfigurationService.updateConfiguration(updateUrlRequest);

    verifySettingWasSet(GITLAB_AUTH_URL, "http://new.url");
    verifySettingWasSet(GITLAB_AUTH_SECRET, "new_secret");
  }

  private static void assertConfigurationFields(GitlabConfiguration configuration) {
    assertThat(configuration).isNotNull();
    assertThat(configuration.id()).isEqualTo("gitlab-configuration");
    assertThat(configuration.enabled()).isTrue();
    assertThat(configuration.applicationId()).isEqualTo("applicationId");
    assertThat(configuration.url()).isEqualTo("url");
    assertThat(configuration.secret()).isEqualTo("secret");
    assertThat(configuration.synchronizeGroups()).isTrue();
    assertThat(configuration.allowedGroups()).containsExactlyInAnyOrder("group1", "group2", "group3");
    assertThat(configuration.provisioningType()).isEqualTo(AUTO_PROVISIONING);
    assertThat(configuration.allowUsersToSignUp()).isTrue();
    assertThat(configuration.provisioningToken()).isEqualTo("provisioningToken");
  }

  @Test
  public void createConfiguration_whenConfigurationAlreadyExists_shouldThrow() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(AUTO_PROVISIONING);
    gitlabConfigurationService.createConfiguration(gitlabConfiguration);

    assertThatThrownBy(() -> gitlabConfigurationService.createConfiguration(gitlabConfiguration))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("GitLab configuration already exists. Only one Gitlab configuration is supported.");
  }

  @Test
  public void createConfiguration_whenAllowedGroupsIsEmptyAndAutoProvisioningIsEnabled_shouldReturnBadRequest() {
    GitlabConfiguration configuration = new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      true,
      "applicationId",
      "url",
      "secret",
      true,
      Set.of(),
      true,
      AUTO_PROVISIONING,
      "provisioningToken");

    assertThatThrownBy(() -> gitlabConfigurationService.createConfiguration(configuration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("allowedGroups cannot be empty when Auto-provisioning is enabled.");
  }

  @Test
  public void createConfiguration_whenAutoProvisioning_shouldCreateCorrectConfigurationAndScheduleSync() {
    GitlabConfiguration configuration = buildGitlabConfiguration(AUTO_PROVISIONING);

    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(configuration);

    assertConfigurationIsCorrect(configuration, createdConfiguration);

    verifyCommonSettings(configuration);

    verify(managedInstanceService).queueSynchronisationTask();

  }

  @Test
  public void createConfiguration_whenAutoProvisioningConfigIsIncorrect_shouldThrow() {
    GitlabConfiguration configuration = new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      true,
      "applicationId",
      "url",
      "secret",
      true,
      Set.of("group1", "group2", "group3"),
      true,
      AUTO_PROVISIONING,
      null);

    assertThatThrownBy(() -> gitlabConfigurationService.createConfiguration(configuration))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Provisioning token must be set to enable GitLab provisioning.");

  }

  @Test
  public void createConfiguration_whenInstanceIsExternallyManaged_shouldThrow() {
    GitlabConfiguration configuration = buildGitlabConfiguration(AUTO_PROVISIONING);

    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(managedInstanceService.getProviderName()).thenReturn("not-gitlab");

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.createConfiguration(configuration))
      .withMessage("It is not possible to synchronize SonarQube using GitLab, as it is already managed by not-gitlab.");

  }

  @Test
  public void createConfiguration_whenJitProvisioning_shouldCreateCorrectConfiguration() {
    GitlabConfiguration configuration = buildGitlabConfiguration(JIT);

    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(configuration);

    assertConfigurationIsCorrect(configuration, createdConfiguration);

    verifyCommonSettings(configuration);
    verifyNoInteractions(managedInstanceService);

  }

  @Test
  public void createConfiguration_whenJitProvisioningAndProvisioningTokenNotSet_shouldCreateCorrectConfiguration() {
    GitlabConfiguration configuration = new GitlabConfiguration(
      UNIQUE_GITLAB_CONFIGURATION_ID,
      true,
      "applicationId",
      "url",
      "secret",
      true,
      Set.of("group1", "group2", "group3"),
      true,
      JIT,
      null);

    GitlabConfiguration createdConfiguration = gitlabConfigurationService.createConfiguration(configuration);

    assertConfigurationIsCorrect(configuration, createdConfiguration);

    verifyCommonSettings(configuration);
    verifyNoInteractions(managedInstanceService);

  }

  private void verifyCommonSettings(GitlabConfiguration configuration) {
    verifySettingWasSet(GITLAB_AUTH_ENABLED, String.valueOf(configuration.enabled()));
    verifySettingWasSet(GITLAB_AUTH_APPLICATION_ID, configuration.applicationId());
    verifySettingWasSet(GITLAB_AUTH_URL, configuration.url());
    verifySettingWasSet(GITLAB_AUTH_SECRET, configuration.secret());
    verifySettingWasSet(GITLAB_AUTH_SYNC_USER_GROUPS, String.valueOf(configuration.synchronizeGroups()));
    verifySettingWasSet(GITLAB_AUTH_ALLOWED_GROUPS, String.join(",", configuration.allowedGroups()));
    verifySettingWasSet(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP, String.valueOf(configuration.allowUsersToSignUp()));
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_TOKEN, Strings.nullToEmpty(configuration.provisioningToken()));
    verifySettingWasSet(GITLAB_AUTH_PROVISIONING_ENABLED, String.valueOf(configuration.provisioningType().equals(AUTO_PROVISIONING)));
  }

  private void verifySettingWasSet(String setting, @Nullable String value) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(setting).getValue()).isEqualTo(value);
  }

  @Test
  public void deleteConfiguration_whenIdIsNotGitlabConfiguration_throwsException() {
    assertThatThrownBy(() -> gitlabConfigurationService.deleteConfiguration("not-gitlab-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Gitlab configuration with id not-gitlab-configuration not found");
  }

  @Test
  public void deleteConfiguration_whenConfigurationDoesntExist_throwsException() {
    assertThatThrownBy(() -> gitlabConfigurationService.deleteConfiguration("gitlab-configuration"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitLab configuration doesn't exist.");
  }

  @Test
  public void deleteConfiguration_whenConfigurationExists_shouldDeleteConfiguration() {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("12", "12", GitLabIdentityProvider.KEY));
    dbTester.getDbClient().externalGroupDao().insert(dbSession, new ExternalGroupDto("34", "34", GitLabIdentityProvider.KEY));
    dbSession.commit();
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    gitlabConfigurationService.deleteConfiguration("gitlab-configuration");

    assertPropertyIsDeleted(GITLAB_AUTH_ENABLED);
    assertPropertyIsDeleted(GITLAB_AUTH_APPLICATION_ID);
    assertPropertyIsDeleted(GITLAB_AUTH_URL);
    assertPropertyIsDeleted(GITLAB_AUTH_SECRET);
    assertPropertyIsDeleted(GITLAB_AUTH_SYNC_USER_GROUPS);
    assertPropertyIsDeleted(GITLAB_AUTH_ALLOWED_GROUPS);
    assertPropertyIsDeleted(GITLAB_AUTH_PROVISIONING_ENABLED);
    assertPropertyIsDeleted(GITLAB_AUTH_ALLOW_USERS_TO_SIGNUP);
    assertPropertyIsDeleted(GITLAB_AUTH_PROVISIONING_TOKEN);

    assertThat(dbTester.getDbClient().externalGroupDao().selectByIdentityProvider(dbTester.getSession(), GitLabIdentityProvider.KEY)).isEmpty();
  }

  private void assertPropertyIsDeleted(String property) {
    assertThat(dbTester.getDbClient().propertiesDao().selectGlobalProperty(property)).isNull();
  }

  @Test
  public void triggerRun_whenConfigIsCorrect_shouldTriggerSync() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(AUTO_PROVISIONING));
    reset(managedInstanceService);

    gitlabConfigurationService.triggerRun();

    verify(managedInstanceService).queueSynchronisationTask();
  }

  @Test
  public void triggerRun_whenConfigIsForJit_shouldThrow() {
    gitlabConfigurationService.createConfiguration(buildGitlabConfiguration(JIT));

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.triggerRun())
      .withMessage("Auto provisioning must be activated");
  }

  @Test
  public void triggerRun_whenConfigIsDisabled_shouldThrow() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(AUTO_PROVISIONING);
    gitlabConfigurationService.createConfiguration(gitlabConfiguration);
    gitlabConfigurationService.updateConfiguration(builder().gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID).enabled(withValueOrThrow(false)).build());

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.triggerRun())
      .withMessage("GitLab authentication must be turned on to enable GitLab provisioning.");
  }

  @Test
  public void triggerRun_whenProvisioningTokenIsNotSet_shouldThrow() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(AUTO_PROVISIONING);
    gitlabConfigurationService.createConfiguration(gitlabConfiguration);
    gitlabConfigurationService.updateConfiguration(builder().gitlabConfigurationId(UNIQUE_GITLAB_CONFIGURATION_ID).provisioningToken(withValue(null)).build());

    assertThatIllegalStateException()
      .isThrownBy(() -> gitlabConfigurationService.triggerRun())
      .withMessage("Provisioning token must be set to enable GitLab provisioning.");
  }

  @Test
  public void validate_whenConfigurationIsDisabled_shouldNotValidate() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(AUTO_PROVISIONING);
    when(gitlabConfiguration.enabled()).thenReturn(false);

    gitlabConfigurationService.validate(gitlabConfiguration);

    verifyNoInteractions(gitlabGlobalSettingsValidator);
  }

  @Test
  public void validate_whenConfigurationIsValidAndJIT_returnEmptyOptional() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(JIT);
    when(gitlabConfiguration.enabled()).thenReturn(true);

    gitlabConfigurationService.validate(gitlabConfiguration);

    verify(gitlabGlobalSettingsValidator).validate(AUTH_ONLY, gitlabConfiguration.url() + "/api/v4", gitlabConfiguration.provisioningToken());
  }

  @Test
  public void validate_whenConfigurationIsValidAndAutoProvisioning_returnEmptyOptional() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(AUTO_PROVISIONING);
    when(gitlabConfiguration.enabled()).thenReturn(true);

    gitlabConfigurationService.validate(gitlabConfiguration);

    verify(gitlabGlobalSettingsValidator).validate(COMPLETE, gitlabConfiguration.url() + "/api/v4", gitlabConfiguration.provisioningToken());
  }

  @Test
  public void validate_whenConfigurationIsInValid_returnsExceptionMessage() {
    GitlabConfiguration gitlabConfiguration = buildGitlabConfiguration(AUTO_PROVISIONING);
    when(gitlabConfiguration.enabled()).thenReturn(true);

    Exception exception = new IllegalStateException("Invalid configuration");
    when(gitlabConfigurationService.validate(gitlabConfiguration)).thenThrow(exception);

    Optional<String> message = gitlabConfigurationService.validate(gitlabConfiguration);

    assertThat(message).contains("Invalid configuration");
  }

  private static GitlabConfiguration buildGitlabConfiguration(ProvisioningType provisioningType) {
    GitlabConfiguration gitlabConfiguration = mock();
    when(gitlabConfiguration.id()).thenReturn("gitlab-configuration");
    when(gitlabConfiguration.enabled()).thenReturn(true);
    when(gitlabConfiguration.applicationId()).thenReturn("applicationId");
    when(gitlabConfiguration.url()).thenReturn("url");
    when(gitlabConfiguration.secret()).thenReturn("secret");
    when(gitlabConfiguration.synchronizeGroups()).thenReturn(true);
    when(gitlabConfiguration.allowedGroups()).thenReturn(new LinkedHashSet<>(Set.of("group1", "group2", "group3")));
    when(gitlabConfiguration.provisioningType()).thenReturn(provisioningType);
    when(gitlabConfiguration.allowUsersToSignUp()).thenReturn(true);
    when(gitlabConfiguration.provisioningToken()).thenReturn("provisioningToken");
    return gitlabConfiguration;
  }

  private static void assertConfigurationIsCorrect(GitlabConfiguration expectedConfiguration, GitlabConfiguration actualConfiguration) {
    assertThat(actualConfiguration.id()).isEqualTo(expectedConfiguration.id());
    assertThat(actualConfiguration.enabled()).isEqualTo(expectedConfiguration.enabled());
    assertThat(actualConfiguration.applicationId()).isEqualTo(expectedConfiguration.applicationId());
    assertThat(actualConfiguration.url()).isEqualTo(expectedConfiguration.url());
    assertThat(actualConfiguration.secret()).isEqualTo(expectedConfiguration.secret());
    assertThat(actualConfiguration.synchronizeGroups()).isEqualTo(expectedConfiguration.synchronizeGroups());
    assertThat(actualConfiguration.allowedGroups()).containsExactlyInAnyOrderElementsOf(expectedConfiguration.allowedGroups());
    assertThat(actualConfiguration.provisioningType()).isEqualTo(expectedConfiguration.provisioningType());
    assertThat(actualConfiguration.allowUsersToSignUp()).isEqualTo(expectedConfiguration.allowUsersToSignUp());
    assertThat(actualConfiguration.provisioningToken()).isEqualTo(expectedConfiguration.provisioningToken());
  }
}
